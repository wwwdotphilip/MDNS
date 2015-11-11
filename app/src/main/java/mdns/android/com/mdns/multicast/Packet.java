package mdns.android.com.mdns.multicast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Encapsulate packet details that we are interested in.
 */
public class Packet {
    public InetAddress src;
    public int srcPort;
    public InetAddress dst;
    public int dstPort;
    public String description;

    public Packet() {
    }

    public Packet(DatagramPacket dp, DatagramSocket socket) {
        src = dp.getAddress();
        srcPort = dp.getPort();
        dst = socket.getLocalAddress();
        dstPort = socket.getLocalPort();
    }
}
