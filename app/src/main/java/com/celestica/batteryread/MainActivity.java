package com.celestica.batteryread;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = "MainActivity";

    private static final int REQUEST_ENABLE_BT = 0;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler = new Handler();

    private static final long SCAN_PERIOD = 20000;

    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    UUID service_notify = UUID.fromString("33C8F429-7A5E-0000-74C6-DBDB025488B4");
    public final static UUID characteristic_status = UUID.fromString("33C8F429-7A5E-0000-74C6-DBDB025453B4");
    private TextView batteryLevelTextView;
    UUID descriptor_notify = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mActivity = getParent();
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        batteryLevelTextView = (TextView) findViewById(R.id.battery_textView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Scanning BLE device", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                scanLeDevice(true);
            }
        });

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "juf juf from P'bomb", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


    }

    @Override
    protected void onResume() {
        scanLeDevice(true);
        super.onResume();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();
    }

    private void scanLeDevice(boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi,
                                     byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.i("Nong jiw", " love you : " + device.getName() + " : " + device.getAddress());
                            if (device.getName() != null) {
                                if (device.getName().equals("Revolar")) {
                                    Log.i("Nong jiw 2", " love  : " + device.getName() + " : " + device.getAddress());
                                    mBluetoothGatt = device.connectGatt(getParent(), false, mGattCallback);
                                }
                            }
                        }
                    });


                }


            };

    // Various callback methods defined by the BLE API.
    private final BluetoothGattCallback mGattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    Log.i(TAG, "onConnectionStateChange");

                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        mConnectionState = STATE_CONNECTED;
                        scanLeDevice(false);
                        Log.i(TAG, "Connected to GATT server.");
                        Log.i(TAG, "Attempting to start service discovery:" +
                                gatt.discoverServices());
                        gatt.discoverServices();


                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        mConnectionState = STATE_DISCONNECTED;
                        scanLeDevice(true);
                        Log.i(TAG, "Disconnected from GATT server.");
                    }
                }

                @Override
                public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                    super.onCharacteristicChanged(gatt, characteristic);
                    Log.i(TAG, "onCharacteristicChanged");

                    if (characteristic.getUuid().equals(characteristic_status)) {
                        checkBatteryLevel(characteristic);
                    }
                }


                @Override
                public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    Log.i(TAG, "onCharacteristicWrite");
                    super.onCharacteristicWrite(gatt, characteristic, status);
                }



                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.i(TAG, "onServicesDiscovered");
                    gatt.setCharacteristicNotification(gatt.getService(service_notify).getCharacteristic(characteristic_status), true);

                    BluetoothGattDescriptor descriptor = gatt.getService(service_notify).getCharacteristic(characteristic_status).getDescriptor(descriptor_notify);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

                    gatt.writeDescriptor(descriptor);
                }

                @Override
                // Result of a characteristic read operation
                public void onCharacteristicRead(BluetoothGatt gatt,
                                                 BluetoothGattCharacteristic characteristic,
                                                 int status) {
                    Log.i(TAG, "onCharacteristicRead");

                    checkBatteryLevel(characteristic);


                }
            };

    private void checkBatteryLevel(BluetoothGattCharacteristic characteristic) {
        Log.i(TAG, "checkBatteryLevel");
        try {
            byte[] battery_read = characteristic.getValue();
            String hex = String.format("%02x", battery_read[1]);
            int value = Integer.parseInt(hex, 16);
            double percent = value * 0.392156863;
            Log.i(TAG, "try" + percent);
            sentBatteryToUi(percent);

        } catch (Exception e) {
            Log.i(TAG, "catch");
        }
    }

    private void sentBatteryToUi(final double percent) {
        final String percentBattery = Double.toString(percent);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Toast.makeText(this, "battery level :"+percentBattery, Toast.LENGTH_SHORT).show();
                batteryLevelTextView.setText("Battery percent is "+percentBattery);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
