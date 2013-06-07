from paxos_node import PaxosNode

import Protocol
import LocalFS
import Snapshot as BaseSnapshot
import Delta
import Serialization
import SnapshotCommitJournal
import ClientJournal
import Journal
from pyoperation import RemoteOp
from pyfs import RPC, Transaction
from java.util import HashSet as Set

from collections import deque
import time


# These functions act as decorators that look pretty and do nothing but serve
# to annotate the role of each method, reminiscent of Java annotations.
K = lambda x: x
override = K
server = K
client = K


class Queue(deque):
    """This Queue class partially mimics the interface of Python's standard
    library Queue class, but does not support blocking and timeouts."""

    def put(self, item):
        return self.append(item)

    def get(self):
        return self.popleft()

    def empty(self):
        return len(self) == 0


class TTLDict(dict):
    """A dict class that associates each key-value entry with a time-to-live
    value in the unit of seconds. Upon, expiration, the next subsequent attempt
    to get the key-value entry deletes it, raising KeyError. Calling len() on a
    TTLDict is not reliable, as it may count expired entries that have not been
    evicted yet."""

    def __init__(self, ttl=10):
        dict.__init__(self)
        self._ttl = ttl

    def __getitem__(self, key):
        value, expiration = dict.__getitem__(self, key)
        if expiration <= time.time():
            self.__delitem__(key)
            raise KeyError(key)
        return value

    def __setitem__(self, key, value):
        expiration = time.time() + self._ttl
        dict.__setitem__(self, key, (value, expiration))

    def __contains__(self, key):
        if not dict.__contains__(self, key):
            return False

        try:
            self.__getitem__(key)
            return True
        except KeyError as e:
            return False


class Snapshot(BaseSnapshot):
    """A subclass of the Java Snapshot class that adds additional methods."""

    @property
    def proposal(self):
        """Produce a less compact format for deltas of a snapshot. An append is
        coerced into an overwrite through a merge. A delete is represented as
        an overwrite with None. This allows Paxos nodes to learn complete file
        contents even without prior knowledge of the file."""

        deltas = self.deltas
        proposal = {}

        for filename in deltas:
            if deltas[filename].type == Delta.Type.DELETE:
                proposal[filename] = None
            else:
                proposal[filename] = self.read(filename)

        return proposal


class ServerNode(object):
    def handle_request(self, src_addr, msg):
        try:
            req = Serialization.decode(msg)
            print 'handle_request: %r' % (req,)
            req = self.on_request(req, src_addr)
            out = Serialization.encode(req)
            if len(out) != 0 and req is not None:
                print 'replying to %d: %r' % (src_addr, req)
                self.RIOSend(src_addr, Protocol.CHITTER_RPC_REPLY, out)
        except Serialization.DecodingException as e:
            print 'failed to decode RPC request'
        except Serialization.EncodingException as e:
            print 'failed to encode RPC request'

    def on_request(self, req, src_addr):
        raise NotImplementedError()



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
        raise NotImplementedError()


# Journals are not meant to be used this way, and I feel bad about myself
class TransactionJournal(Journal):
    def __init__(self, node):
        Journal.__init__(self, "$txn_journal", node)
        self.node = node

    def execute(self, obj):
        pass

    @override
    def pop(self):
        pass

    @override
    def completePendingOps(self):
        pass


class ChitterNode(ServerNode, ClientNode, PaxosNode):
    def __init__(self):
        PaxosNode.__init__(self)
        ClientNode.__init__(self)

        self.pending_cmds = {}

        self.sid = 0
        self.last_sid = 0
        self.tid = 0
        self.pid = 0
        self.completed_transactions = set()

        self.snapshots = {}
        self.cache = TTLDict()
        self.op = RemoteOp()

        self.pending_proposals = {}

    @server
    def next_sid(self):
        """Issues a new session ID"""

        sid = self.sid
        self.sid += 1
        return sid

    @client
    def next_tid(self):
        """Generates a new transaction ID"""

        tid = self.tid
        self.tid += 1
        return tid

    @server
    def next_pid(self):
        """Generates a new Paxos ID"""

        pid = self.pid
        self.pid += 1
        return pid

    @override
    def start(self):
        super(self.__class__, self).start()

        print 'Node %d started' % (self.addr,)

        self.client_journal = ClientJournal(self)
        self.txn_journal = TransactionJournal(self)
        for cookie in self.txn_journal.pendingOperations:
            self.completed_transactions.add(cookie)

        for command in self.client_journal.commands:
            node_addr, cmd_name, cmd_str = command.split(None, 2)
            proc = self.op(cmd_name, cmd_str)
            self.pending_cmds[command] = proc
            value = proc.next()
            self.act(int(node_addr), command, value)

        self.tid = self.client_journal.count
        # wrap up a commit that we failed during, if necessary:
        commit_snap = SnapshotCommitJournal(self, self.fs)
        commit_snap.completePendingOps()

        # Manually set up Paxos
        nodes = {0, 1, 2}
        nodes.remove(self.addr)
        other_nodes = ' '.join(str(addr) for addr in nodes)
        PaxosNode.onCommand(self, 'paxos_setup ' + other_nodes)

    @override
    def fail(self):
        print 'failed'
        super(self.__class__, self).fail()

    @override
    def onRIOReceive(self, from_addr, protocol, msg):
        ClientNode.onRIOReceive(self, from_addr, protocol, msg)
        PaxosNode.onRIOReceive(self, from_addr, protocol, msg)

    @override
    def onCommand(self, command):
        """Called when a command is issued through reply or console"""

        super(self.__class__, self).onCommand(command)

        node_addr, cmd_name, cmd_str = command.split(None, 2)
        proc = self.op(cmd_name, cmd_str)

        self.client_journal.push(command)
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

        # Place read results in cache
        if req['action'] == 'do' and req['name'] == 'read':
            filename, content = req['args'][0], req['result']
            self.cache[filename] = content

        # Resume the corresponding generator
        proc = self.pending_cmds[command]
        value = proc.send(req.get('result', None))

        node_addr = req['dest']
        self.act(node_addr, command, value, req['sid'], req['tid'])
        self.pump_recv_queue()

    @client
    def act(self, node_addr, command, value, sid=None, tid=None):
        """Given an original command and a value yielded from a generator, act."""

        message = {'command': command, 'dest': node_addr}

        if isinstance(value, Transaction):
            # Start a transaction with a new tid
            message.update(action='begin', tid=self.next_tid())
            self.send_rpc(message)
        elif isinstance(value, RPC):
            # Perform an action
            name, args = value()
            message.update(action='do', name=name, args=args, sid=sid, tid=tid)

            # Try hitting the cache
            should_intercept_send = self.before_send_rpc(message)
            if not should_intercept_send:
                self.send_rpc(message)
        elif value is Transaction.Commit:
            # Commit the transaction
            message.update(action='commit', sid=sid, tid=tid)
            self.send_rpc(message)
        else:
            # The operation completed with a return value
            self.pending_cmds.pop(command)
            self.on_command_complete(command, value)

    @client
    def on_command_complete(self, command, result):
        print '[done] %s = %r' % (command, result)
        self.client_journal.complete(command)

    @client
    def before_send_rpc(self, message):
        """Called before self.send_rpc is called. Returning True will prevent
        send_rpc from being called."""

        if not message['action'] == 'do':
            return False

        # A write is about to happen. Evict from cache if possible.
        if message['name'] in ('overwrite', 'append'):
            filename = message['args'][0]

            try:
                del self.cache[filename]
            except KeyError:
                pass

            return False

        # A read is about to happen. Try to load from cache and avoid network
        if message['name'] == 'read':
            filename = message['args'][0]

            if filename not in self.cache:
                return False

            message['result'] = self.cache[filename]
            self.recv_queue.put(message)

            return True

    @server
    def on_request(self, req, src_addr=None):
        """Called when a server receives a request"""

        action = req['action']

        if action == 'begin':
            return self.respond_begin(req, src_addr)
        elif action == 'do':
            return self.respond_do(req, src_addr)
        elif action == 'commit':
            return self.respond_commit(req, src_addr)

    @server
    def respond_begin(self, req, src_addr):
        cookie = (src_addr, req['tid'])
        if cookie in self.completed_transactions:
            req['error'] = 'transaction already completed'
            return req

        sid = self.next_sid()
        self.snapshots[sid] = Snapshot(self.fs)
        req['sid'] = sid
        return req

    @server
    def respond_do(self, req, src_addr):
        sid = req['sid']
        if sid not in self.snapshots:
            req['error'] = 'invalid session ID'
            return req

        snapshot = self.snapshots[sid]
        result = getattr(snapshot, req['name'])(*req['args'])
        req['result'] = result
        return req

    @server
    def respond_commit(self, req, src_addr):
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
            snapshot = self.snapshots.pop(sid)

            pid = self.next_pid()
            self.pending_proposals[pid] = {
                'snapshot': snapshot, 'req': req, 'src_addr': src_addr,
                'sid': sid
            }

            proposal = {'value': snapshot.proposal, 'pid': pid}

            if not snapshot.empty:
                self.propose(proposal)
            else:
                self.on_paxos_consensus(True, proposal)
            return None

    @override
    def on_paxos_consensus(self, success, proposal):
        pid = proposal['pid']
        if pid not in self.pending_proposals:
            # Ignore for now
            return

        memo = self.pending_proposals.pop(pid)
        req, src_addr = memo['req'], memo['src_addr']
        snapshot, sid = memo['snapshot'], memo['sid']

        if success:
            snapshot.commit(self)

            cookie = (src_addr, req['tid'])
            self.completed_transactions.add(cookie)
            self.txn_journal.push(cookie)

            self.last_sid = sid
            req['result'] = True

        else:
            req['error'] = 'Paxos proposal rejected'

        out = Serialization.encode(req)
        print 'replying to %d: %r' % (src_addr, req)
        self.RIOSend(src_addr, Protocol.CHITTER_RPC_REPLY, out)

    @override
    def on_paxos_learned(self, value):
        proposal = value['value']
        for filename, content in proposal.iteritems():
            if content is None:
                self.fs.delete(filename)
                return

            if not self.fs.exists(filename):
                self.fs.create(filename)

            self.fs.overwriteIfNotChanged(filename, content, -1)
