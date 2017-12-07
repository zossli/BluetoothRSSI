package li.zoss.bfh.bsc.bluetoothrssi;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ACTION_REQUEST_ENABLE_ = 1;
    private static final int REQUEST_SCAN_MODE_CONNECTABLE = 2;

    private String TAG = "MainActivity";

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> listItems = new ArrayList<>();
    BluetoothAdapter mBluetoothAdapter;
    private String oldName = "";
    private String name;
    private TextView mDeviceView;
    private TextView mRssiView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final int CODE = 5; // app defined constant used for onRequestPermissionsResult
        name = getString(R.string.name) + (int) (Math.random() * 1000);
        String[] permissionsToRequest =
                {
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                };

        boolean allPermissionsGranted = true;

        for (String permission : permissionsToRequest) {
            allPermissionsGranted = allPermissionsGranted && (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED);
        }

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, CODE);
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ACTION_REQUEST_ENABLE_);

        }


        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));// Register for broadcasts when a device is discovered.
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));

        listView = findViewById(R.id.listview);
        mDeviceView = findViewById(R.id.txtDeviceName);
        mRssiView = findViewById(R.id.txtRSSI);
        mDeviceView.setText("");
        mRssiView.setText("");

        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        listView.setAdapter(adapter);

        findViewById(R.id.btnSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.startDiscovery();
                    if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000);
                        startActivityForResult(discoverableIntent, REQUEST_SCAN_MODE_CONNECTABLE);
                    }
                } else {
                    mBluetoothAdapter.cancelDiscovery();
                    mBluetoothAdapter.startDiscovery();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

    }


    @Override
    protected void onPause() {
        super.onPause();
        mBluetoothAdapter.setName(oldName);

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                if (!(deviceName == null)) {
                    if (deviceName.startsWith(getString(R.string.name))) {
                        addItems(deviceName, rssi);
                        mBluetoothAdapter.cancelDiscovery();
                        Log.i(TAG, "onReceive: Found Device: deviceName: " + deviceName + " device RSSI: " + rssi);
                    }
                }

            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action) || BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if (!mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.startDiscovery();
                }
            }
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                int RSSI = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String mDeviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                Log.i(TAG, "onReceive: mDeviceName: " + mDeviceName + " RSSI: " + RSSI);
            }
        }
    };

    public void addItems(String deviceName, int RSSI) {
        listItems.add(deviceName + "  ->  " + RSSI);
        mDeviceView.setText(deviceName);
        mRssiView.setText("RSSI: "+RSSI);
        adapter.notifyDataSetChanged();
        listView.smoothScrollToPosition( adapter.getCount()-1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ACTION_REQUEST_ENABLE_) {
            Log.i(TAG, "onActivityResult: " + resultCode);
        }
        Log.i(TAG, "onActivityResult: " + resultCode);

    }


    @Override
    protected void onDestroy() {
        mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.disable();
        unregisterReceiver(mReceiver);
        super.onDestroy();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (oldName.isEmpty()) {
            oldName = mBluetoothAdapter.getName();
            Log.i(TAG, "onCreate: old Name set to " + oldName);
        }
        mBluetoothAdapter.setName(name);

    }


}

