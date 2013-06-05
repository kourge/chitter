import AbstractNode

import Protocol
import LocalFS
import Snapshot
import Serialization
from pyoperation import RemoteOp
from pyfs import RPC, Transaction


K = lambda x: x
override = K
server = K
client = K


class Queue(list):
    def put(self, item):
        return self.append(item)

    def get(self):
        return self.pop(0)

    def empty(self):
        return len(self) == 0


class ServerNode(object):
    def handle_request(self, src_addr, msg):
        try:
            req = Serialization.decode(msg)
            print 'handle_request: %r' % (req,)
            req = self.on_request(req)
            out = Serialization.encode(req)
            if len(out) != 0:
                print 'replying to %d: %r' % (src_addr, req)
                self.RIOSend(src_addr, Protocol.CHITTER_RPC_REPLY, out)
        except Serialization.DecodingException as e:
            print 'failed to decode RPC request'
        except Serialization.EncodingException as e:
            print 'failed to encode RPC request'

    def on_request(self, src_addr, req):
        raise Exception()



class ClientNode(object):
    def __init__(self):
        self.send_queue = Queue()
        self.recv_queue = Queue()

    @override
    def onRIOReceive(self, src_addr, protocol, msg):
        if False:
            pass
        elif protocol == Protocol.CHITTER_RPC_REQUEST:
            self.handle_request(src_addr, msg)
        elif protocol == Protocol.CHITTER_RPC_REPLY:
            self.handle_reply(src_addr, msg)
            self.pump_recv_queue()
            self.pump_send_queue()
        else:
            pass
            # print 'unknown protocol packet: %r' % (protocol,)

    def handle_reply(self, src_addr, msg):
        print 'handle_reply'
        try:
            req = Serialization.decode(msg)
            self.recv_queue.put(req)
            self.pump_recv_queue()
        except Serialization.DecodingException as e:
            print 'failed to decode RPC reply'

    def pump_send_queue(self):
        if self.send_queue.empty():
            return
        print 'send_queue: %r' % (self.send_queue,)

        req = self.send_queue.get()
        try:
            payload = Serialization.encode(req)
            if len(payload) != 0:
                self.RIOSend(req['dest'], Protocol.CHITTER_RPC_REQUEST, payload)
        except Serialization.EncodingException as e:
            print 'failed to encode request'

    def pump_recv_queue(self):
        if self.recv_queue.empty():
            return
        print 'recv_queue: %r' % (self.recv_queue,)

        req = self.recv_queue.get()

        if req is None:
            print 'received unknown response: %r' % (req,)
            return

        self.on_complete(req)

    def send_rpc(self, msg):
        if msg is None:
            return False

        self.send_queue.put(msg)
        self.pump_send_queue()

    def onRIODrop(self, payload):
        try:
            req = Serialization.decode(payload)
            self.send_queue.put(req)
            self.pump_send_queue()
        except Serialization.DecodingException as e:
            print "Failed to decode dropped RPC request"

    def on_complete(self, req):
        raise Exception()


class ChitterNode(ServerNode, ClientNode, AbstractNode):
    def __init__(self):
        ClientNode.__init__(self)
        self.pending_cmds = {}
        self.sid = 0
        self.last_sid = 0
        self.snapshots = {}
        self.op = RemoteOp()

    def create_sid(self):
        """Issues a new session ID"""

        sid = self.sid
        self.sid += 1
        return sid

    @override
    def start(self):
        print 'Node %d started' % (self.addr,)
        pass

    @override
    def fail(self):
        print 'failed'
        super(self.__class__, self).fail()

    @override
    def onRIOReceive(self, from_addr, protocol, msg):
        super(self.__class__, self).onRIOReceive(from_addr, protocol, msg)

        if protocol != Protocol.PAXOS:
            return

        # TODO deserialize msg to a PaxosMessage type
        pass

    @override
    def onCommand(self, command):
        """Called when a command is issued through reply or console"""

        node_addr, cmd_name, cmd_str = command.split(None, 2)
        proc = self.op(cmd_name, cmd_str)

        self.pending_cmds[command] = proc

        value = proc.next()
        self.act(int(node_addr), command, value)

    @client
    def on_complete(self, req):
        """Called when a client request is complete"""

        command = req['command']
        if command not in self.pending_cmds:
            print 'unknown response: %r' % (req,)
            return

        if 'error' in req:
            print 'error: %s' % (req['error'],)
            return

        proc = self.pending_cmds[command]
        value = proc.send(req.get('result', None))
        node_addr = req['dest']
        self.act(node_addr, command, value, req['sid'])

    def act(self, node_addr, command, value, sid=None):
        """Given an original command and a value yielded from a generator, act."""

        message = {'command': command, 'dest': node_addr}

        if isinstance(value, Transaction):
            # Start the transaction
            message.update(action='begin')
            self.send_rpc(message)
        elif isinstance(value, RPC):
            # Perform an action
            name, args = value()
            message.update(action='do', name=name, args=args, sid=sid)
            self.send_rpc(message)
        elif value is Transaction.Commit:
            # Commit the transaction
            message.update(action='commit', sid=sid)
            self.send_rpc(message)
        else:
            # The operation completed with a return value
            print '[done] %s = %r' % (command, value)

    @server
    def on_request(self, req):
        """Called when a server receives a request"""

        action = req['action']

        if action == 'begin':
            sid = self.create_sid()
            self.snapshots[sid] = Snapshot(self.fs)
            req['sid'] = sid
            return req
        elif action == 'do':
            sid = req['sid']
            if sid not in self.snapshots:
                req['error'] = 'invalid session ID'
                return req

            snapshot = self.snapshots[sid]
            result = getattr(snapshot, req['name'])(*req['args'])
            req['result'] = result
            return req
        elif action == 'commit':
            sid = req['sid']
            if sid not in self.snapshots:
                req['error'] = 'invalid session ID'
                return req

            if self.last_sid > sid:
                if False:
                    # TODO: optimize for transactions that don't touch other files
                    pass
                else:
                    req['error'] = 'transaction failed: sid: %d, last sid: %d' % (
                        sid, self.last_sid
                    )
                    return req
            else:
                snapshot = self.snapshots[sid]
                snapshot.commit()

                self.last_sid = sid
                req['result'] = True
                return req
