import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;
import java.util.Iterator;

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
	public static int TIMEOUT = 3;
	
	private HashMap<Integer, InChannel> inConnections;
	private HashMap<Integer, OutChannel> outConnections;
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

		InChannel in = inConnections.get(from);
		if(in == null) {
			in = new InChannel();
			inConnections.put(from, in);


            // new connection, we want to establish a session
            long proposedSessionId = UUID.randomUUID().getMostSignificantBits();
            in.sessionId = proposedSessionId;
            in.awaitingSessionAck = true;
            in.sessionResends = 0;

            byte[] sessionIdByteArray = Utility.stringToByteArray("" + proposedSessionId);
            n.send(from, Protocol.INITIATE_SESSION, sessionIdByteArray);

            // setup a timeout
            try {
                Method onTimeoutMethod = Callback.getMethod("onTimeoutSession", this,
                    new String[]{ "java.lang.Integer", "java.lang.Long" });
                n.addTimeout(new Callback(onTimeoutMethod, this, new Object[]
                    { from, proposedSessionId }), ReliableInOrderMsgLayer.TIMEOUT);
            } catch (Exception e) {}
            return;
		}

        // only ack and such if we have a session and it matches the packet
        if (!in.awaitingSessionAck && in.sessionId == riopkt.getSessionId()) {
            // at-most-once semantics
            byte[] seqNumByteArray = Utility.stringToByteArray("" + riopkt.getSeqNum());
            n.send(from, Protocol.ACK, seqNumByteArray);

            LinkedList<RIOPacket> toBeDelivered = in.gotPacket(riopkt);
            for(RIOPacket p: toBeDelivered) {
                // deliver in-order the next sequence of packets
                n.onRIOReceive(from, p.getProtocol(), p.getPayload());
            }
        } else {
            //System.out.println("Packet ignored, session: " + riopkt.getSessionId());
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
		int seqNum = Integer.parseInt( Utility.byteArrayToString(msg) );
		outConnections.get(from).gotACK(seqNum);
	}

	public void RIOSessionReceive(int from, byte[] msg) {
        long session = Long.parseLong(Utility.byteArrayToString(msg));
        OutChannel out = outConnections.get(from);
		out.sessionId = session;

        // we'll send back the lowest seqnum
        byte[] seqByteArray = Utility.stringToByteArray("" + out.getSeq());
        n.send(from, Protocol.ACK_SESSION, seqByteArray);
	}
        
	public void RIOSessionAck(int from, byte[] msg) {
        // we've gotten an ack, yay
		inConnections.get(from).awaitingSessionAck = false;
		int seqNum = Integer.parseInt( Utility.byteArrayToString(msg) );
        inConnections.get(from).setSeqNum(seqNum);
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
        if (inConnections.get(addr).awaitingSessionAck) {
            InChannel in = inConnections.get(addr);
            in.sessionResends++;
            if (in.sessionResends < MAX_SESSION_RESENDS) {
                // if we're still awaiting a session by now, then resend
                byte[] sessionIdByteArray = Utility.stringToByteArray("" + sessionNum);
                n.send(addr, Protocol.INITIATE_SESSION, sessionIdByteArray);

                try {
                    // setup a timeout
                    Method onTimeoutMethod = Callback.getMethod("onTimeoutSession", this,
                        new String[]{ "java.lang.Integer", "java.lang.Long" });
                    n.addTimeout(new Callback(onTimeoutMethod, this, new Object[]
                        { addr, sessionNum }), ReliableInOrderMsgLayer.TIMEOUT * in.sessionResends);
                } catch (Exception e) {}
            } // else { give up for now }
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

    // session info
    public long sessionId;
    public boolean awaitingSessionAck;
    public int sessionResends;
	
	InChannel(){
		lastSeqNumDelivered = -1;
		outOfOrderMsgs = new HashMap<Integer, RIOPacket>();
        this.sessionId = -1;
        this.sessionResends = 0;
        this.awaitingSessionAck = false;
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
	
	@Override
	public String toString() {
		return "last delivered: " + lastSeqNumDelivered + ", outstanding: " + outOfOrderMsgs.size();
	}
}

/**
 * Representation of an outgoing channel to this node
 */
class OutChannel {
	private HashMap<Integer, RIOPacket> unACKedPackets;
	private int lastSeqNumSent;
	private ReliableInOrderMsgLayer parent;
	private int destAddr;

    // session info
    public long sessionId;
	
	OutChannel(ReliableInOrderMsgLayer parent, int destAddr){
		lastSeqNumSent = -1;
		unACKedPackets = new HashMap<Integer, RIOPacket>();
		this.parent = parent;
		this.destAddr = destAddr;
        this.sessionId = -1;
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
			RIOPacket newPkt = new RIOPacket(protocol, ++lastSeqNumSent, sessionId, payload);
			unACKedPackets.put(lastSeqNumSent, newPkt);
			
			n.send(destAddr, Protocol.DATA, newPkt.pack());
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

    public int getSeq() {
        if (unACKedPackets.isEmpty()) {
            return lastSeqNumSent;
        }
        int min = 0;
        Iterator it = unACKedPackets.keySet().iterator();
        while(it.hasNext()) {
            int k = (Integer)it.next();
            if (k < min) {
                min = k;
            }
        }
        return min - 1;
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
		try{
			Method onTimeoutMethod = Callback.getMethod("onTimeout", parent, new String[]{ "java.lang.Integer", "java.lang.Integer" });
			RIOPacket riopkt = unACKedPackets.get(seqNum);
            riopkt.setSessionId(sessionId);

            // If we fail to get acked for too long, give up
            if (riopkt.numResends > ReliableInOrderMsgLayer.NUM_RESENDS) {
                unACKedPackets.remove(seqNum);
                //System.out.println("PACKET DROPPED");
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
