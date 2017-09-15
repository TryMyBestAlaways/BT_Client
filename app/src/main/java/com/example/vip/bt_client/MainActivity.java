package com.example.vip.bt_client;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.LinkedList;

import static com.example.vip.bt_client.BluetoothLeService.ACTION_DATA_AVAILABLE;
import static com.example.vip.bt_client.BluetoothLeService.ACTION_GATT_CONNECTED;
import static com.example.vip.bt_client.BluetoothLeService.ACTION_GATT_DISCONNECTED;
import static com.example.vip.bt_client.BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED;

/**
 *
 */
public class MainActivity extends AppCompatActivity {
    private Button connect, send;
    private TextView pitchAngText, rollAngText, yawAngText, altText,
            distanceText, voltageText;
    private EditText roolI, roolD, yawP, yawI, yawD, pitchP, pitchI, pitchD,
            altitudeP, altitudeI, altitudeD, info;
    private RadioGroup roolP;
    private Handler handler;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final static int WRITE_DATA_PERIOD = 40;
    private static int IMU_CNT = 0;
    private final static int REQUEST_CONNECT_DEVICE = 1;
    private boolean connected;
    private String deviceName;
    private String deviceAddress;
    private final String MSP_HEADER = "$M<";
    private boolean isSend;
    private BluetoothLeService mBluetoothLeService;
    private ImageView bg;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            Log.w("tag", "mBluetoothLeService:" + mBluetoothLeService);
            if (!mBluetoothLeService.initialize()) {
                Log.w("tag", "Unable to initialize Bluetooth");
                finish();

            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w("tag", "连接失败");
            mBluetoothLeService = null;
        }
    };


    Runnable runnable = new Runnable() {
        @Override
        public void run() {

            try {
                handler.postDelayed(this, WRITE_DATA_PERIOD);
                if (IMU_CNT >= 10) {
                    IMU_CNT = 0;
                    btSendBytes(Protocol.getSendData(Protocol.FLY_STATE, Protocol
                            .getCommandData(Protocol.FLY_STATE)));
                }

                IMU_CNT++;
                if (isSend) {
                    btSendBytes(Protocol.getSendData(Protocol.SET_4CON, Protocol
                            .getCommandData(Protocol.SET_4CON)));
                    Toast.makeText(MainActivity.this, "发送成功", Toast.LENGTH_SHORT).show();
                    isSend = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };


    /**
     * 定义处理BLE收发服务的各类事件接收机mGattUpdateReceiver，主要包括下面几种：
     * ACTION_GATT_CONNECTED: 连接到GATT
     * ACTION_GATT_DISCONNECTED: 断开GATT
     * ACTION_GATT_SERVICES_DISCOVERED: 发现GATT下的服务
     * ACTION_DATA_AVAILABLE: BLE收到数据
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            int reCmd = -2;
//               Log.w("tag", "已经进入接收函数mGattUpdateReceiver");
            if (ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                connect.setText("断开");
                Log.w("tag", "已经在连接");
            } else if (ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                connect.setText("连接");
                Log.w("tag", "连接不成功");
            } else if (ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                mBluetoothLeService.getSupportedGattServices();
            } else if (ACTION_DATA_AVAILABLE.equals(action)) {
                final byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data) {
                        stringBuilder.append(String.format("%02X ", byteChar));
                    }
//                         Log.w("tag", "RX Data:" + stringBuilder);
                }
                Protocol.processDataIn(data, data.length);
                updateLogData(); // 跟新IMU数据，update the IMU data

            }
        }
    };

    private void updateLogData() {

        pitchAngText.setText("PitchAng: " + Protocol.pitchAng);
        rollAngText.setText("RollAng: " + Protocol.rollAng);
        yawAngText.setText("YawAng: " + Protocol.yawAng);
        altText.setText("Altitude:" + Protocol.alt + "m");

        voltageText.setText("Voltage:" + Protocol.voltage + " V");
        distanceText.setText("Speed:" + Protocol.speedZ + "m/s");

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide();
        handler = new Handler();
        connect = (Button) findViewById(R.id.connect);
        send = (Button) findViewById(R.id.send);
        roolP = (RadioGroup) findViewById(R.id.rp);
        roolI = (EditText) findViewById(R.id.ri);
        roolD = (EditText) findViewById(R.id.rd);
        yawP = (EditText) findViewById(R.id.yp);
        yawI = (EditText) findViewById(R.id.yi);
        yawD = (EditText) findViewById(R.id.yd);
        pitchP = (EditText) findViewById(R.id.pp);
        pitchAngText = (TextView) findViewById(R.id.pitchAngText);
        rollAngText = (TextView) findViewById(R.id.rollAngText);
        yawAngText = (TextView) findViewById(R.id.yawAngText);
        altText = (TextView) findViewById(R.id.altText);
        voltageText = (TextView) findViewById(R.id.voltageText);
        distanceText = (TextView) findViewById(R.id.distanceText);

/*
        Intent serviceIntent = new Intent(this, BluetoothLeService.class);
        boolean isBind = bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        handler.postDelayed(runnable, WRITE_DATA_PERIOD);
        roolP.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.p) {
                    rp = "P".getBytes();
                } else if (checkedId == R.id.i) {
                    rp = "I".getBytes();
                } else if (checkedId == R.id.d) {
                    rp = "D".getBytes();
                } else {
                    rp = "0".getBytes();
                }
                Log.w("tag", "rp的值++++++" + rp[0]);
            }
        });
        Log.w("tag", isBind + "已进入主程序onCreate");*/
        bg = (ImageView) findViewById(R.id.background);
        Glide.with(this).load(R.drawable.background).into(bg);
    }

    public void onConnect(View v) {
        if (!connected) {
            Intent serverIntent = new Intent(MainActivity.this, ConnectActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        } else {
            mBluetoothLeService.disconnect();
        }

    }

    private byte[] rp;

    public void getInputPID() {

        LinkedList<Byte> data = new LinkedList<Byte>();
        Log.w("tag", "rp的值" + rp[0]);
        final int ri = intFromFloatString(roolI.getText());
        final int rd = intFromFloatString(roolD.getText());
        final int yp = intFromFloatString(yawP.getText());
        final int yi = intFromFloatString(yawI.getText());
        final int yd = intFromFloatString(yawD.getText());
        final int pp = intFromFloatString(pitchP.getText());


        data.add((byte) ((rp[0]) & 0xff));
        data.add((byte) ((6) & 0xff));
        data.add((byte) ((ri) & 0xff));
        data.add((byte) ((ri >> 8) & 0xff));
        data.add((byte) ((rd) & 0xff));
        data.add((byte) ((rd >> 8) & 0xff));

        data.add((byte) ((yp) & 0xff));
        data.add((byte) ((yp >> 8) & 0xff));
        data.add((byte) ((yi) & 0xff));
        data.add((byte) ((yi >> 8) & 0xff));
        data.add((byte) ((yd) & 0xff));
        data.add((byte) ((yd >> 8) & 0xff));

        data.add((byte) ((pp) & 0xff));
        data.add((byte) ((pp >> 8) & 0xff));
        Log.w("tag", "@@@@@@@@@@@" + ((data.get(0) & 0xff) + (data.get(1) << 8)));

        int i = 0;
        for (byte b : data) {
            Protocol.arrayData[i++] = b;
        }
        Log.w("tag", "************" + ((Protocol.arrayData[0] & 0xff) + (Protocol.arrayData[1]
                << 8)));
    }

    public void onSend(View v) {
        if (connected) {
            getInputPID();
            isSend = true;
        } else {
            Toast.makeText(this, "还没连上", Toast.LENGTH_SHORT).show();

        }
    }

    private int intFromFloatString(Editable e) {
        String str = e.toString();
        if ("".equals(str) || str == null) {
            return 0;
        }
        return (int) (Float.valueOf(str) * 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            Log.w("tag", "mBluetoothLeService NOT null");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONNECT_DEVICE && resultCode == Activity.RESULT_OK) {
            deviceName = data.getExtras().getString(EXTRAS_DEVICE_NAME);
            deviceAddress = data.getExtras().getString(EXTRAS_DEVICE_ADDRESS);
            Log.w("tag", "mDeviceName:" + deviceName + ",mDeviceAddress:" + deviceAddress);
            Log.w("tag", "mBluetoothLeService:" + mBluetoothLeService);
            if (mBluetoothLeService != null) {
                final boolean result = mBluetoothLeService.connect(deviceAddress);
                Log.w("tag", "Connect request result=" + result);
            }

        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GATT_CONNECTED);
        intentFilter.addAction(ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        mBluetoothLeService = null;
    }

    public void btSendBytes(byte[] data) {
        if (connected) {
            mBluetoothLeService.writeCharacteristic(data);
        }
    }


}
