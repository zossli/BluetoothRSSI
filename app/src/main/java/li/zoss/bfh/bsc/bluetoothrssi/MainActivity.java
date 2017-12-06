package li.zoss.bfh.bsc.bluetoothrssi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.usage.NetworkStatsManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Network;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ACTION_REQUEST_ENABLE_ = 1;
    private static final int REQUEST_SCAN_MODE_CONNECTABLE = 2;
    private static final boolean CON_SECURE = true;
    private String TAG = "MainActivity";
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> listItems = new ArrayList<String>();
    private ArrayList<BluetoothDevice> macAddresses = new ArrayList<BluetoothDevice>();
    BluetoothAdapter mBluetoothAdapter;
    private MyBTService myBTService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final int CODE = 5; // app defined constant used for onRequestPermissionsResult

        String[] permissionsToRequest =
                {
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
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
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.i(TAG, "Paired Devices: device Name: " + deviceName);
                Log.i(TAG, "Paired Devices: device MAC: " + deviceHardwareAddress);
            }
        }
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE));
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));// Register for broadcasts when a device is discovered.
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED));

        listView = findViewById(R.id.listview);

        adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1,
                listItems);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, final long id) {
                Log.i(TAG, "Item was clicked id=" + position);
                myBTService.connect(macAddresses.get(position),CON_SECURE);
            }
        });
        findViewById(R.id.btnsend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               myBTService.write("hallo".getBytes());
                Log.i(TAG, "onClick: send data");
            }
        });
        findViewById(R.id.btnSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mBluetoothAdapter.isDiscovering()) {
                    adapter.clear();
                    macAddresses.clear();
                    mBluetoothAdapter.startDiscovery();
                    if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3000);
                        startActivityForResult(discoverableIntent, REQUEST_SCAN_MODE_CONNECTABLE);
                    }
                } else {
                    Log.i(TAG, "btnSearch clicked but already discovering");
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        myBTService = new MyBTService(this, mhandler);

    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                Log.i(TAG, "onReceive: Found Device: deviceName: " + deviceName + " device RSSI: " + rssi);
                Log.i(TAG, "ACTION_FOUND");

                addItems(deviceName + "", device);

            }
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Log.i(TAG, "ACTION_DISCOVERY_STARTED");
            }
            if (BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE.equals(action)) {
                Log.i(TAG, "ACTION_REQUEST_DISCOVERABLE");
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "ACTION_DISCOVERY_FINISHED");
            }
            if(BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                int RSSI = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                String mDeviceName = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                Log.i(TAG, "onReceive: mDeviceName: "+mDeviceName + " RSSI: "+RSSI);
            }
        }
    };

    public void addItems(String deviceName, BluetoothDevice device) {
        adapter.add(deviceName);
        macAddresses.add(device);
        adapter.notifyDataSetChanged();

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
        super.onDestroy();
        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(mReceiver);
    }
    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (myBTService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (myBTService.getState() == myBTService.STATE_NONE) {
                // Start the Bluetooth chat services
                myBTService.start();
            }
        }
    }

    public static final int STARTRUNNER = 100;
    private BluetoothManager blManager;
    @SuppressLint("HandlerLeak")
    private final Handler mhandler = new Handler() {
        @SuppressLint("WrongConstant")
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == STARTRUNNER) {
                Log.i(TAG, "handleMessage: We got connected");
                ((BluetoothSocket)msg.arg1).getRemoteDevice().ge
            }
        }

    };


}

