package com.example.vip.bt_client;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vip on 2017/3/13.
 * 
 * 
 */
public class ConnectActivity extends ListActivity {
    private BluetoothAdapter bluetoothAdapter;
    private Handler handler;
    private static final long SCAN_PERIOD = 10000;
    private static final int REQUEST_ENABLE_BT = 1;
    private MyBluetoothAdapter myBluetoothAdapter;

    private Button searchButton;
    private Button cancelButton;
    private boolean scanning;


    private android.bluetooth.BluetoothAdapter.LeScanCallback callback = new android.bluetooth
            .BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    myBluetoothAdapter.addDevice(device);
                    myBluetoothAdapter.notifyDataSetChanged();
                }
            });
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        handler = new Handler();

        searchButton = (Button) findViewById(R.id.search);
        cancelButton = (Button) findViewById(R.id.cancel);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!scanning) {
                    scanDevice(true);
                } else {
                    scanDevice(false);
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanDevice(false);
                finish();
            }
        });
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "蓝牙透传不支持", Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context
                .BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "蓝牙透传不支持", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!bluetoothAdapter.isEnabled()) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        myBluetoothAdapter = new MyBluetoothAdapter();
        setListAdapter(myBluetoothAdapter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        scanDevice(false);
        myBluetoothAdapter.clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        final BluetoothDevice device = myBluetoothAdapter.getDevice(position);
        if (null == device) {
            return;
        }
        final Intent intent = new Intent();
        intent.putExtra(MainActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(MainActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (scanning) {
            bluetoothAdapter.stopLeScan(callback);
            scanning = false;
        }
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    public void scanDevice(boolean enable) {
        if (enable) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    bluetoothAdapter.stopLeScan(callback);

                }
            }, SCAN_PERIOD);
            scanning = true;
            bluetoothAdapter.startLeScan(callback);
            searchButton.setText("搜索中");
        } else {
            scanning = false;
            bluetoothAdapter.stopLeScan(callback);
            searchButton.setText("搜索");
        }
    }

    private class MyBluetoothAdapter extends BaseAdapter {
        private List<BluetoothDevice> devices;
        private LayoutInflater inflator;

        public MyBluetoothAdapter() {
            devices = new ArrayList<BluetoothDevice>();
            inflator = ConnectActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!devices.contains(device)) {
                devices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return devices.get(position);
        }

        public void clear() {
            devices.clear();
        }


        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflator.inflate(R.layout.adapter_list_item, null);
                holder = new ViewHolder();
                holder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
                holder.deviceAddress = (TextView) convertView.findViewById(R.id.device_address);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            BluetoothDevice device = devices.get(position);
            String dName = device.getName();

            if (null != dName && dName.length() > 0) {
                holder.deviceName.setText(dName);
            } else {
                holder.deviceName.setText("未知设备");
            }
            holder.deviceAddress.setText(device.getAddress());

            return convertView;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }


}

