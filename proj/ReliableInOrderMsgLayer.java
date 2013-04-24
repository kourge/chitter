import java.lang.reflect.Method;
import java.util.*;

import edu.washington.cs.cse490h.lib.Callback;
import edu.washington.cs.cse490h.lib.Utility;

/**
 * Layer above the basic messaging layer that provides reliable, in-order
 * delivery in the absence of faults. This layer does not provide much more than
 * the above.
 * 
 * At a minimum, the student should extend/modify this layer to provide
 * reliable, in-order message delivery, even in the presence of node failures.
 */
public class ReliableInOrderMsgLayer {
	public static int TIMEOUT = 5;
	
	private HashMap<Integer, InChannel> inConnections;
	private HashMap<Integer, OutChannel> outConnections;
	private HashMap<Integer, Integer> sessionsWaiting;
	private RIONode n;

    // how many resends before we give up
    public static final int NUM_RESENDS = 5;

    public static final int MAX_SESSION_RESENDS = 4;

	/**
	 * Constructor.
	 * 
	 * @param destAddr
	 *            The address of the destination host
	 * @param msg
	 *            The message that was sent
	 * @param timeSent
	 *            The time that the ping was sent
	 */
	public ReliableInOrderMsgLayer(RIONode n) {
		inConnections = new HashMap<Integer, InChannel>();
		outConnections = new HashMap<Integer, OutChannel>();
		sessionsWaiting = new HashMap<Integer, Integer>();
		this.n = n;
	}
	
	/**
	 * Receive a data packet.
	 * 
	 * @param from
	 *            The address from which the data packet came
	 * @param pkt
	 *            The Packet of data
	 */
	public void RIODataReceive(int from, byte[] msg) {

		RIOPacket riopkt = RIOPacket.unpack(msg);

        // if the UUID is incorrect, then initiate a new session
        if (riopkt.getSessionId() != n.getUUID()) {
            System.out.println("Bad id, sending session setup");

            // clean up
			inConnections.put(from, new InChannel());
            if (outConnections.get(from) != null)
                outConnections.put(from, new OutChannel(this, from));

            String tmp = "" + n.getUUID();
            n.send(from, Protocol.INITIATE_SESSION, tmp.getBytes());

            // mark that we're waiting on an ACK
            sessionsWaiting.put(from, 0);

            // add callback for resends
            try {
                Method onTimeoutMethod = Callback.getMethod("onTimeoutSession", this,
                    new String[]{ "java.lang.Integer", "java.lang.Long" });
                n.addTimeout(new Callback(onTimeoutMethod, this,
                    new Object[]{ from, n.getUUID() }), ReliableInOrderMsgLayer.TIMEOUT);
            } catch (Exception e) {}
        }

		InChannel in = inConnections.get(from);
		if(in == null) {
			in = new InChannel();
			inConnections.put(from, in);
        }
    
        if (riopkt.getSessionId() == n.getUUID()) {
            LinkedList<RIOPacket> toBeDelivered = in.gotPacket(riopkt);
            for(RIOPacket p: toBeDelivered) {
                // deliver in-order the next sequence of packets
                n.onRIOReceive(from, p.getProtocol(), p.getPayload());
            }
        }
	}
	
	/**
	 * Receive an acknowledgment packet.
	 * 
	 * @param from
	 *            The address from which the data packet came
	 * @param pkt
	 *            The Packet of data
	 */
	public void RIOAckReceive(int from, byte[] msg) {
        if (outConnections.get(from) != null) {
            int seqNum = Integer.parseInt( Utility.byteArrayToString(msg) );
            outConnections.get(from).gotACK(seqNum);
        }
	}

	public void RIOSessionReceive(int from, byte[] msg) {
        
        // we need to start a new session...
        
        // set id
        long id = Long.parseLong( Utility.byteArrayToString(msg) );
        n.peerSessions.put(from, id);

        // renumber in-flight packets
        if (outConnections.get(from) != null) {
            outConnections.get(from).resetSequence(id);
        }

        // and restart in channel
        inConnections.put(from, new InChannel());

        // send ack with out UUID
        byte[] sessionIdByteArray = Utility.stringToByteArray("" + n.getUUID());
        n.send(from, Protocol.ACK_SESSION, sessionIdByteArray);
	}

	public void RIOSessionAck(int from, byte[] msg) {
        long id = Long.parseLong( Utility.byteArrayToString(msg) );
        if (sessionsWaiting.get(from) != null) {
            sessionsWaiting.remove(from);
        }
        n.peerSessions.put(from, id);
	}

	/**
	 * Send a packet using this reliable, in-order messaging layer. Note that
	 * this method does not include a reliable, in-order broadcast mechanism.
	 * 
	 * @param destAddr
	 *            The address of the destination for this packet
	 * @param protocol
	 *            The protocol identifier for the packet
	 * @param payload
	 *            The payload to be sent
	 */
	public int RIOSend(int destAddr, int protocol, byte[] payload) {
		OutChannel out = outConnections.get(destAddr);
		if(out == null) {
			out = new OutChannel(this, destAddr);
			outConnections.put(destAddr, out);
		}
		
		return out.sendRIOPacket(n, protocol, payload);
	}

	/**
	 * Callback for timeouts while waiting for an ACK.
	 * 
	 * This method is here and not in OutChannel because OutChannel is not a
	 * public class.
	 * 
	 * @param destAddr
	 *            The receiving node of the unACKed packet
	 * @param seqNum
	 *            The sequence number of the unACKed packet
	 */
	public void onTimeout(Integer destAddr, Integer seqNum) {
		outConnections.get(destAddr).onTimeout(n, seqNum);
	}

	public void onTimeoutSession(Integer addr, Long sessionNum) {
        if (sessionsWaiting.get(addr) != null) {
            if (sessionsWaiting.get(addr) < MAX_SESSION_RESENDS) {
                // if we're still awaiting a session by now, then resend
                byte[] sessionIdByteArray = Utility.stringToByteArray("" + sessionNum);
                n.send(addr, Protocol.INITIATE_SESSION, sessionIdByteArray);

                System.out.println("Resending session init: " + sessionNum);

                try {
                    // setup another timeout
                    Method onTimeoutMethod = Callback.getMethod("onTimeoutSession", this,
                        new String[]{ "java.lang.Integer", "java.lang.Long" });
                    n.addTimeout(new Callback(onTimeoutMethod, this, new Object[]
                        { addr, sessionNum }), ReliableInOrderMsgLayer.TIMEOUT);
                } catch (Exception e) {}
            } else {
                System.out.println("Drop session init");
            }
        }
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(Integer i: inConnections.keySet()) {
			sb.append(inConnections.get(i).toString() + "\n");
		}
		
		return sb.toString();
	}
}

/**
 * Representation of an incoming channel to this node
 */
class InChannel {
	private int lastSeqNumDelivered;
	private HashMap<Integer, RIOPacket> outOfOrderMsgs;

	InChannel(){
		lastSeqNumDelivered = -1;
		outOfOrderMsgs = new HashMap<Integer, RIOPacket>();
	}

    public void setSeqNum(int num) {
        lastSeqNumDelivered = num;
    }

	/**
	 * Method called whenever we receive a data packet.
	 * 
	 * @param pkt
	 *            The packet
	 * @return A list of the packets that we can now deliver due to the receipt
	 *         of this packet
	 */
	public LinkedList<RIOPacket> gotPacket(RIOPacket pkt) {
		LinkedList<RIOPacket> pktsToBeDelivered = new LinkedList<RIOPacket>();
		int seqNum = pkt.getSeqNum();
		
		if(seqNum == lastSeqNumDelivered + 1) {
			// We were waiting for this packet
			pktsToBeDelivered.add(pkt);
			++lastSeqNumDelivered;
			deliverSequence(pktsToBeDelivered);
		}else if(seqNum > lastSeqNumDelivered + 1){
			// We received a subsequent packet and should store it
			outOfOrderMsgs.put(seqNum, pkt);
		}
		// Duplicate packets are ignored
		
		return pktsToBeDelivered;
	}

	/**
	 * Helper method to grab all the packets we can now deliver.
	 * 
	 * @param pktsToBeDelivered
	 *            List to append to
	 */
	private void deliverSequence(LinkedList<RIOPacket> pktsToBeDelivered) {
		while(outOfOrderMsgs.containsKey(lastSeqNumDelivered + 1)) {
			++lastSeqNumDelivered;
			pktsToBeDelivered.add(outOfOrderMsgs.remove(lastSeqNumDelivered));
		}
	}

    public void dropMessages() {
        outOfOrderMsgs.clear();
    }
	
	@Override
	public String toString() {
		return "last delivered: " + lastSeqNumDelivered + ", outstanding: " + outOfOrderMsgs.size();
	}
}

/**
 * Representation of an outgoing channel to this node
 */
class OutChannel {
	private TreeMap<Integer, RIOPacket> unACKedPackets;
	private int lastSeqNumSent;
	private ReliableInOrderMsgLayer parent;
	private int destAddr;

	
	OutChannel(ReliableInOrderMsgLayer parent, int destAddr){
		lastSeqNumSent = -1;
		unACKedPackets = new TreeMap<Integer, RIOPacket>();
		this.parent = parent;
		this.destAddr = destAddr;
	}

    public void resetSequence(long session) {
        // renumber unacked packets and reset sequence number
        TreeMap<Integer, RIOPacket> newPackets = new TreeMap<Integer, RIOPacket>();
        Iterator it = unACKedPackets.keySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            RIOPacket p = unACKedPackets.get(it.next());
            p.setSessionId(session);
            p.setSeqNum(newPackets.size());
            lastSeqNumSent = newPackets.size();
            newPackets.put(newPackets.size(), p);
        }
    }
	
	/**
	 * Send a new RIOPacket out on this channel.
	 * 
	 * @param n
	 *            The sender and parent of this channel
	 * @param protocol
	 *            The protocol identifier of this packet
	 * @param payload
	 *            The payload to be sent
	 */
	protected int sendRIOPacket(RIONode n, int protocol, byte[] payload) {
		try{
			Method onTimeoutMethod = Callback.getMethod("onTimeout", parent, new String[]{ "java.lang.Integer", "java.lang.Integer" });
            long out_uuid = n.getUUID();
            if (n.peerSessions.get(destAddr) != null) {
                out_uuid = n.peerSessions.get(destAddr);
                //System.out.println("Sending to uuid " + out_uuid);
            } else {
                //System.out.println("Sending from uuid " + out_uuid);
            }
			RIOPacket newPkt = new RIOPacket(protocol, ++lastSeqNumSent, out_uuid, payload);
			unACKedPackets.put(lastSeqNumSent, newPkt);
			
			n.send(destAddr, Protocol.DATA, newPkt.pack());

            //System.out.println("timeout");
			n.addTimeout(new Callback(onTimeoutMethod, parent, new Object[]{ destAddr, lastSeqNumSent }), ReliableInOrderMsgLayer.TIMEOUT);
		}catch(Exception e) {
			e.printStackTrace();
		}
        return lastSeqNumSent;
	}
	
	/**
	 * Called when a timeout for this channel triggers
	 * 
	 * @param n
	 *            The sender and parent of this channel
	 * @param seqNum
	 *            The sequence number of the unACKed packet
	 */
	public void onTimeout(RIONode n, Integer seqNum) {
		if(unACKedPackets.containsKey(seqNum)) {
			resendRIOPacket(n, seqNum);
		}
	}

	/**
	 * Called when we get an ACK back. Removes the outstanding packet if it is
	 * still in unACKedPackets.
	 * 
	 * @param seqNum
	 *            The sequence number that was just ACKed
	 */
	protected void gotACK(int seqNum) {
		unACKedPackets.remove(seqNum);
	}

    public void setSeq(int s) {
        lastSeqNumSent = s;
    }
	
	/**
	 * Resend an unACKed packet.
	 * 
	 * @param n
	 *            The sender and parent of this channel
	 * @param seqNum
	 *            The sequence number of the unACKed packet
	 */
	private void resendRIOPacket(RIONode n, int seqNum) {
        System.out.println("RESEND");
		try{
			Method onTimeoutMethod = Callback.getMethod("onTimeout", parent, new String[]{ "java.lang.Integer", "java.lang.Integer" });
			RIOPacket riopkt = unACKedPackets.get(seqNum);
            if (n.peerSessions.get(destAddr) != null) {
                riopkt.setSessionId(n.peerSessions.get(destAddr));
            } else {
                riopkt.setSessionId(n.getUUID());
            }

            System.out.println("proto: " + riopkt.getProtocol());

            // If we fail to get acked for too long, give up
            if (riopkt.numResends > ReliableInOrderMsgLayer.NUM_RESENDS) {
                unACKedPackets.remove(seqNum);
                System.out.println("PACKET DROPPED");
                return;
            }
            riopkt.numResends++;
			
            // backoff linearly (2 * num_resends)
			n.send(destAddr, Protocol.DATA, riopkt.pack());
			n.addTimeout(new Callback(onTimeoutMethod, parent, new Object[]{ destAddr, seqNum }), ReliableInOrderMsgLayer.TIMEOUT * riopkt.numResends);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
}
