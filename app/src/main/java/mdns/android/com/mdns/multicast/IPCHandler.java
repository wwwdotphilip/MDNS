package mdns.android.com.mdns.multicast;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Field;

public class IPCHandler extends Handler {

    private static final int MSG_SET_STATUS = 1;
    private static final int MSG_ADD_PACKET = 2;
    private static final int MSG_ERROR = 3;
    private NetThread netThread = null;
    private static final String TAG = "";
    private Context context;
    public DNSData data;
    private MDNSListener listener;

    public static final String IpRequest = "IpRequest";
    public static final String IpResponse = "IpRequest";

    public IPCHandler(Context context){
        this.context = context;
        data = new DNSData();
    }

    public interface MDNSListener{
        void onResponse(DNSData data);
    }

    public void setMDNSListener(MDNSListener listener){
        this.listener = listener;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        for (Field field : msg.obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            Object value = null;
            try {
                value = field.get(msg.obj);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (value != null) {
                switch (field.getName()) {
                    case "description":
                        data.description = value.toString().replace("ANY? ", "");
                        break;
                    case "src":
                        data.src = value.toString().replace("/", "");
                        break;
                    case "dstPort":
                        data.dstPort = value.toString();
                        break;
                    case "srcPort":
                        data.srcPort = value.toString();
                        break;
                    default:
                        break;
                }
                Log.e("ObjectField", field.getName() + "=" + value);
            }
        }

        if (data.description != null && data.description.equals("IpRequest")) {
            try {
                Log.e(TAG, "Sending response to " + data.src);
                netThread.submitQuery("IpResponse");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else if (data.description != null && data.description.equals("IpResponse")) {
            Log.e("DNSData", "Receive response from: " + data.src);
            if (listener != null){
                listener.onResponse(data);
            }
        }

        switch (msg.what) {
            case MSG_SET_STATUS:
                Log.e("StatusPacket", msg.obj.toString());
                break;
            case MSG_ADD_PACKET:
                Log.e("AddPacket", msg.obj.toString());
                break;
            case MSG_ERROR:
                Log.e("ErrorPacket", msg.obj.toString());
                break;
            default:
                Log.w(TAG, "unknown activity message code: " + msg);
                break;
        }
    }

    public void setStatus(String status) {
        sendMessage(Message.obtain(this, MSG_SET_STATUS, status));
    }

    public void addPacket(Packet packet) {
        sendMessage(Message.obtain(this, MSG_ADD_PACKET, packet));
    }

    public void error(Throwable throwable) {
        sendMessage(Message.obtain(this, MSG_ERROR, throwable));
    }

    public void startMDNS(){

        if (netThread != null) {
            Log.e(TAG, "netThread should be null!");
            netThread.submitQuit();
        }
        netThread = new NetThread(this, context);
        netThread.start();
    }

    public void stopMDNS(){
        if (netThread == null) {
            Log.e(TAG, "netThread should not be null!");
            return;
        }
        netThread.submitQuit();
        netThread = null;
    }

    public void sendRequest(){
        netThread.submitQuery(IpRequest);
    }

    public void replyRequest(){
        netThread.submitQuery(IpResponse);
    }
}
