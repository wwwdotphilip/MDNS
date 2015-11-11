
package mdns.android.com.mdns.multicast;

import android.content.Context;
import android.net.wifi.WifiManager.MulticastLock;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import mdns.android.com.mdns.dns.DNSMessage;

/**
 * This thread runs in the background while the user has our
 * program in the foreground, and handles sending mDNS queries
 * and processing incoming mDNS packets.
 */
public class NetThread extends Thread {

    public static final String TAG = "NetThread";

    // the standard mDNS multicast address and port number
    private static final byte[] MDNS_ADDR =
            new byte[]{(byte) 224, (byte) 0, (byte) 0, (byte) 251};
    private static final int MDNS_PORT = 5353;

    private static final int BUFFER_SIZE = 4096;

    private NetworkInterface networkInterface;
    private InetAddress groupAddress;
    private MulticastSocket multicastSocket;
    private NetUtil netUtil;
    private Context context;
    private IPCHandler ipcHandler;
//    private MainActivity activity;

    /**
     * Construct the network thread.
     *
     * @param context
     */
    public NetThread(IPCHandler ipcHandler, Context context) {
        super("net");
        this.context = context;
        this.ipcHandler = ipcHandler;
        netUtil = new NetUtil(context);
    }

    /**
     * Open a multicast socket on the mDNS address and port.
     *
     * @throws IOException
     */
    private void openSocket() throws IOException {
        multicastSocket = new MulticastSocket(MDNS_PORT);
        multicastSocket.setTimeToLive(2);
        multicastSocket.setReuseAddress(true);
        multicastSocket.setNetworkInterface(networkInterface);
        multicastSocket.joinGroup(groupAddress);
    }

    /**
     * The main network loop.  Multicast DNS packets are received,
     * processed, and sent to the UI.
     * <p/>
     * This loop may be interrupted by closing the multicastSocket,
     * at which time any commands in the commandQueue will be
     * processed.
     */
    @Override
    public void run() {
        Log.v(TAG, "starting network thread");

        Set<InetAddress> localAddresses = NetUtil.getLocalAddresses();
        MulticastLock multicastLock = null;

        // initialize the network
        try {
            networkInterface = netUtil.getFirstWifiOrEthernetInterface();
            if (networkInterface == null) {
                throw new IOException("Your WiFi is not enabled.");
            }
            groupAddress = InetAddress.getByAddress(MDNS_ADDR);

            multicastLock = netUtil.getWifiManager().createMulticastLock("unmote");
            multicastLock.acquire();
            //Log.v(TAG, "acquired multicast lock: "+multicastLock);

            openSocket();
        } catch (IOException e1) {
            e1.printStackTrace();
            ipcHandler.setStatus("cannot initialize network");
            ipcHandler.error(e1);
//            activity.ipc.setStatus("cannot initialize network.");
//            activity.ipc.error(e1);
            return;
        }

        // set up the buffer for incoming packets
        byte[] responseBuffer = new byte[BUFFER_SIZE];
        DatagramPacket response = new DatagramPacket(responseBuffer, BUFFER_SIZE);

        // loop!
        while (true) {
            // zero the incoming buffer for good measure.
            java.util.Arrays.fill(responseBuffer, (byte) 0); // clear buffer

            // receive a packet (or process an incoming command)
            try {
                multicastSocket.receive(response);
            } catch (IOException e) {
                // check for commands to be run
                Command cmd = commandQueue.poll();
                if (cmd == null) {
                    Log.e(TAG, e.toString());
                    ipcHandler.error(e);
//                    activity.ipc.error(e);
                    return;
                }

                // reopen the socket
                try {
                    openSocket();
                } catch (IOException e1) {
                    Log.e(TAG, e1.toString());
                    ipcHandler.error(new RuntimeException("socket reopen: " + e1.getMessage()));
                    return;
                }

                // process commands
                if (cmd instanceof QueryCommand) {
                    try {
                        query(((QueryCommand) cmd).host);
                    } catch (IOException e1) {
                        Log.e(TAG, e1.toString());
                        ipcHandler.error(e1);
                    }
                } else if (cmd instanceof QuitCommand) {
                    break;
                }

                continue;
            }

            // ignore our own packet transmissions.
            if (localAddresses.contains(response.getAddress())) {
                continue;
            }

            // parse the DNS packet
            DNSMessage message;
            try {
                message = new DNSMessage(response.getData(), response.getOffset(), response.getLength());
            } catch (Exception e) {
                e.printStackTrace();
                ipcHandler.error(e);
                continue;
            }

            // send the packet to the UI
            Packet packet = new Packet(response, multicastSocket);
            packet.description = message.toString().trim();
            ipcHandler.addPacket(packet);
            Log.e(TAG, packet.toString());
        }

        // release the multicast lock
        multicastLock.release();

        Log.v(TAG, "stopping network thread");
    }

    /**
     * Transmit an mDNS query on the local network.
     *
     * @param host
     * @throws IOException
     */
    private void query(String host) throws IOException {
        byte[] requestData = (new DNSMessage(host)).serialize();
        DatagramPacket request =
                new DatagramPacket(requestData, requestData.length, InetAddress.getByAddress(MDNS_ADDR), MDNS_PORT);
        multicastSocket.send(request);
    }

    // inter-process communication
    // poor man's message queue

    private Queue<Command> commandQueue = new ConcurrentLinkedQueue<Command>();

    private static abstract class Command {
    }

    private static class QuitCommand extends Command {
    }

    private static class QueryCommand extends Command {
        public QueryCommand(String host) {
            this.host = host;
        }

        public String host;
    }

    public void submitQuery(String host) {
        commandQueue.offer(new QueryCommand(host));
        multicastSocket.close();
    }

    public void submitQuit() {
        commandQueue.offer(new QuitCommand());
        if (multicastSocket != null) {
            multicastSocket.close();
        }
    }

}
