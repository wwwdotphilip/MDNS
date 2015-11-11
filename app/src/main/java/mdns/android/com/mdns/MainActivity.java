package mdns.android.com.mdns;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import mdns.android.com.mdns.multicast.DNSData;
import mdns.android.com.mdns.multicast.IPCHandler;

public class MainActivity extends Activity {

    public static final String TAG = "MainActivity";
    private TextView statusLine;
    private IPCHandler ipc;

    /**
     * Set up the user interface and perform certain setup steps.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ipc = new IPCHandler(MainActivity.this);
        ipc.setMDNSListener(new MDNSListener());
        statusLine = (TextView) findViewById(R.id.status_line);
    }

    /**
     * This is called when the user resumes using the activity
     * after using other programs (and at activity creation time).
     * <p/>
     * We don't keep the network thread running when the user is
     * not running this program in the foreground, so we use this
     * method to initialize the packet list and start the
     * network thread.
     */
    @Override
    protected void onResume() {
        super.onResume();
        ipc.startMDNS();
    }

    /**
     * This is called when the user leaves the activity to run
     * another program.  We stop the network thread when this
     * happens.
     */
    @Override
    protected void onPause() {
        super.onPause();
        ipc.stopMDNS();
    }

    /**
     * Handle submitting an mDNS query.
     */
    public void handleQueryButton(View view) {

        statusLine.setText("sending query...");
        try {
            ipc.sendRequest();
        } catch (Exception e) {
            Log.w(TAG, e.getMessage(), e);
            statusLine.setText("query error: " + e.getMessage());
            return;
        }
        statusLine.setText("query sent.");
    }

    public class MDNSListener implements IPCHandler.MDNSListener{

        @Override
        public void onResponse(DNSData data) {
            toast("Response received from: " + data.src);
        }
    }

    private void toast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
