/*
 * Copyright (c) 20214. Anthony Fryberg.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dashboard.obd.driving;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dashboard.obd.meta.StartActivityForResults;
import com.dashboard.obd.service.SettingsStore;
import com.dashboard.obd.service.VehicleService;
import com.dashboard.obd.R;
import com.dashboard.obd.floatinghead.FloatingHeadService;
import com.dashboard.obd.receiver.BringToFrontReceiver;
import com.dashboard.obd.service.VehicleDataBroadcaster;
import com.pokevian.lib.obd2.data.ObdData;
import com.pokevian.lib.obd2.defs.FuelType;
import com.pokevian.lib.obd2.defs.KEY;
import com.pokevian.lib.obd2.defs.VehicleEngineStatus;
import com.pokevian.lib.obd2.listener.OnObdStateListener;
import com.pokevian.lib.obd2.listener.OnObdStateListener.ObdState;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import android.os.HandlerThread;

public final class DrivingActivity extends AppCompatActivity
        implements OnClickListener, SensorEventListener {

    static final String TAG = "DrivingActivity";

    final static int ENABLE_BT_REQUEST = 1;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private boolean mIsPaused;
    private BluetoothAdapter btAdapter;
    private String chosenDeviceName, chosenDeviceAddress;
    public static final String EXTRA_REQUEST_EXIT = "request_exit";
    public static final String EXTRA_VES = "vehicle_engine_status";


    private DrivingService mDrivingService;
   private VehicleService mVehicleService;
   private LocalBroadcastManager mBroadcastManager;
   private SettingsStore mStoreItem;
    private IndicatorFragment mIndicatorFragment;
    private ObdClusterFragment mObdFragment;
    private TripClusterFragment mTripFragment;
    private BottomFragment mBottomFragment;
    private DrivingDetailInfoFragment mDetailInfoFragment;

    private boolean mIsDrivingPaused;
    private boolean mIsDetailInfoShown;
    private boolean mIsDetailInfoAnimating;

    private RelativeLayout mPreviewSpeedControl;
    private SpeedMeter mPreviewSpeedMeter;


    private int mPreviewSpeedControlShown = -1;

    private boolean mShowDetailInfo;

    private boolean mLaunchNavi;
    private View mContentView;

    private static final int REQUEST_BLUETOOTH_CONNECT = 1;

    private Handler handler = new Handler(); //gps 위치 업데이트

    private static JSONObject sendData = new JSONObject(); //서버 전송 JSON
    private String selectedUser = "유저 ID 설정";
    private int sendSpeed; //BroadcastReceiver의 ACTION_OBD_EXTRA_DATA_RECEIVED에서 업데이트
    private float sendRpm; //BroadcastReceiver의 ACTION_OBD_EXTRA_DATA_RECEIVED에서 업데이트
    private int sendEngine_coolant_temperature; //2개는 ACTION_OBD_DATA_RECEIVED에서 업데이트
    private int sendIntake_manifold_absolute_pressure;
    private float sendThrottle_position;
    private float sendFuel_level;
    private int sendFuel_pressure;
    private float sendAccel_d;
    private float sendEngine_load;
    private float sendShort_term_fuel_trim;
    private float sendLong_term_fuel_trim;
    private int sendAmbient_air_temperature;
    private int sendEngine_oil_temperature;
    private float sendMass_air_flow;

    private double sendLatitude; //2개는 currentMyLocation에서 업데이트
    private double sendLongitude;
    private float accel_x;
    private float accel_y;
    private float accel_z;

    private static final String SERVER_IP = "14.45.110.117"; //14.45.110.117
    private static final int SERVER_PORT = 60000;
    private HandlerThread udpHandlerThread;
    private Handler udpHandler;
    private DatagramSocket socket;

    private ArrayAdapter<String> adapterList;
    private ArrayList<String> deviceList;

    ImageView gpsIndicator;
    ListView ScanDevices;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving);

        if (Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT) {
            getWindow().setStatusBarColor(Color.BLACK);
        }

        View mContentView = findViewById(android.R.id.content);
        ImageView mPreviewBtn = findViewById(R.id.iv_btn_preview);
        mPreviewBtn.setOnClickListener(this);
        findViewById(R.id.detail_info_btn).setOnClickListener(this);

        FragmentManager fm = getSupportFragmentManager();
        mIndicatorFragment = (IndicatorFragment) fm.findFragmentById(R.id.center_fragment);
        mObdFragment = (ObdClusterFragment) fm.findFragmentById(R.id.obd_fragment);
        mTripFragment = (TripClusterFragment) fm.findFragmentById(R.id.trip_fragment);
        mBottomFragment = (BottomFragment) fm.findFragmentById(R.id.bottom_fragment);
        mDetailInfoFragment = (DrivingDetailInfoFragment) fm.findFragmentById(R.id.detail_info_fragment);
        mStoreItem=SettingsStore.getInstance();

        if (savedInstanceState == null) {
            mLaunchNavi = true;
        } else {
            mShowDiagnosticDialog =  savedInstanceState.getBoolean("show-diagnostic", true);
            mShowDetailInfo = savedInstanceState.getBoolean("is-detail-info-shown", false);
            mLastMil = savedInstanceState.getBoolean("last-mil", false);
        }

        ScanDevices = findViewById(R.id.scanDevices);
        deviceList = new ArrayList<>();
        adapterList = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceList);
        ScanDevices.setAdapter(adapterList);
        ScanDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = deviceList.get(position);
                String deviceAddress = selectedItem.split("\n")[1];
                BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    device.createBond();
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                Toast.makeText(getApplicationContext(), "페어링 중: " + device.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "이미 페어링된 기기입니다.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null && btAdapter.isEnabled()) {
            // Bluetooth가 켜져 있는 경우
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12 이상에서는 Bluetooth 권한 체크 필요
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                                != PackageManager.PERMISSION_GRANTED) {
                    // 권한 요청
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                            1);
                } else {
                    startBluetoothConnection();
                }
            } else {
                startBluetoothConnection();
            }
        } else {
            if (btAdapter == null) {
                Toast.makeText(this, "Bluetooth를 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth를 키고 재시작 해주세요.", Toast.LENGTH_SHORT).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                            != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                                    != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},
                                1);
                    }
                }
            }
        }
        sendSpeed=-1; //sendData 에 넣는 변수 초기화
        sendRpm=-1;
        sendEngine_coolant_temperature=-1;
        sendIntake_manifold_absolute_pressure=-1;
        sendThrottle_position = -1;
        sendFuel_level = -1;
        sendFuel_pressure = -1;
        sendAccel_d = -1;
        sendEngine_load = -1;
        sendShort_term_fuel_trim = -1;
        sendLong_term_fuel_trim = -1;
        sendAmbient_air_temperature = -1;
        sendEngine_oil_temperature = -1;
        sendMass_air_flow = -1;

        sendLatitude=-1;
        sendLongitude=-1;
        accel_x = -1; accel_y = -1; accel_z = -1;
        try { //sendData 초기화
            sendData.put("UserID", selectedUser);
            sendData.put("speed", sendSpeed);
            sendData.put("rpm", sendRpm);
            sendData.put("engine_coolant_temperature", sendEngine_coolant_temperature);
            sendData.put("intake_manifold_absolute_pressure", sendIntake_manifold_absolute_pressure);
            sendData.put("Throttle_position", sendThrottle_position);
            sendData.put("Fuel_level",sendFuel_level);
            sendData.put("Fuel_pressure",sendFuel_pressure);
            sendData.put("Accel_d", sendAccel_d);
            sendData.put("Engine_load", sendEngine_load);
            sendData.put("Short_term_fuel_trim", sendShort_term_fuel_trim);
            sendData.put("Long_term_fuel_trim", sendLong_term_fuel_trim);
            sendData.put("Ambient_air_temperature", sendAmbient_air_temperature);
            sendData.put("Engine_oil_temperature", sendEngine_oil_temperature);
            sendData.put("Mass_air_flow", sendMass_air_flow);

            sendData.put("Latitude", sendLatitude);
            sendData.put("Longitude", sendLongitude);
            sendData.put("Accel_x", accel_x);
            sendData.put("Accel_y", accel_y);
            sendData.put("Accel_z", accel_z);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        // UDP 전송을 위한 HandlerThread 시작
        udpHandlerThread = new HandlerThread("UdpHandlerThread");
        udpHandlerThread.start();
        udpHandler = new Handler(udpHandlerThread.getLooper());

        try {
            socket = new DatagramSocket();
            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
            socket.bind(new InetSocketAddress(serverAddress, SERVER_PORT));
        } catch (SocketException e) {
            Log.e(TAG, "소켓 생성 실패");
        } catch (UnknownHostException e) {
            Log.e(TAG, "소켓 생성 실패2");
        }
        startUdpTransmission();

        gpsIndicator = findViewById(R.id.GPSOn); // 레이아웃 XML에서 id를 지정해야 함
        gpsIndicator.setVisibility(View.GONE);// 초기에는 숨김 상태로 설정

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        currentMyLocation();
        startDiscovery();
    }

    private void startDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Bluetooth 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        deviceList.clear();
        adapterList.notifyDataSetChanged();
        Toast.makeText(this, "Bluetooth Scan", Toast.LENGTH_SHORT).show();
        btAdapter.startDiscovery();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mVehicleReceiver, filter);
    }

    private void startUdpTransmission() {
        udpHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendUdpData();
                udpHandler.postDelayed(this, 1000);  // 1초 후 반복 실행
            }
        }, 1000);  // 1초 지연 후 처음 실행
    }

    private void sendUdpData() {
        udpHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket != null && !socket.isClosed()) {
                        sendData.put("UserID", selectedUser);
                        sendData.put("speed", sendSpeed);
                        sendData.put("rpm", sendRpm);
                        sendData.put("engine_coolant_temperature", sendEngine_coolant_temperature);
                        sendData.put("intake_manifold_absolute_pressure", sendIntake_manifold_absolute_pressure);
                        sendData.put("Throttle_position", sendThrottle_position);
                        sendData.put("Fuel_level",sendFuel_level);
                        sendData.put("Fuel_pressure",sendFuel_pressure);
                        sendData.put("Accel_d", sendAccel_d);
                        sendData.put("Engine_load", sendEngine_load);
                        sendData.put("Short_term_fuel_trim", sendShort_term_fuel_trim);
                        sendData.put("Long_term_fuel_trim", sendLong_term_fuel_trim);
                        sendData.put("Ambient_air_temperature", sendAmbient_air_temperature);
                        sendData.put("Engine_oil_temperature", sendEngine_oil_temperature);
                        sendData.put("Mass_air_flow", sendMass_air_flow);

                        sendData.put("Latitude", sendLatitude);
                        sendData.put("Longitude", sendLongitude);

                        sendData.put("Accel_x", accel_x);
                        sendData.put("Accel_y", accel_y);
                        sendData.put("Accel_z", accel_z);
                        byte[] sendDataBytes = sendData.toString().getBytes();

                        // UDP 패킷 생성
                        InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
                        DatagramPacket packet = new DatagramPacket(sendDataBytes, sendDataBytes.length, serverAddress, SERVER_PORT);

                        // UDP 소켓을 열고 데이터 전송
                        socket.send(packet);
                        Log.d(TAG, "JSON 데이터 전송: " + sendData.toString());
                    } else {
                        Log.e(TAG, "소켓이 닫혀 있거나 유효하지 않음.");
                    }
                } catch (UnknownHostException e) {
                    Log.e(TAG, "서버 주소를 찾을 수 없습니다: " + e.getMessage());
                } catch (SocketException e) {
                    Log.e(TAG, "소켓 오류: " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "데이터 전송 오류: " + e.getMessage());
                } catch (JSONException e) {
                    Log.e(TAG, "JSON 데이터 생성 오류: " + e.getMessage());
                }
            }
        });
    }

    private void currentMyLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        GPSListener gpsListener = new GPSListener();
        long minTime = 1000;
        float minDistance = 0;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    minTime,
                    minDistance,
                    gpsListener);
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    minTime,
                    minDistance,
                    gpsListener);

            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            // GPS 위치가 없다면 NETWORK_PROVIDER를 사용
            if (lastLocation == null) {
                lastLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            // 위치가 있으면 TextView에 설정
            if (lastLocation != null) {
                double latitude = lastLocation.getLatitude();
                double longitude = lastLocation.getLongitude();
                sendLatitude = latitude;
                sendLongitude = longitude;
            } else {
            }

        } catch (SecurityException e) {
            Log.e(TAG, "위치 권한 오류: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "위치 정보 가져오기 오류: " + e.getMessage());
        }
    }

    // LocationListener 정의
    private class GPSListener implements LocationListener {
        // LocationManager에서 위치정보가 변경되면 호출
        @Override
        public void onLocationChanged(Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            sendLatitude = latitude;
            sendLongitude = longitude;
            // 위치 정보를 텍스트뷰에 설정
            showGpsIndicator();
        }

        @Override
        public void onProviderDisabled(String provider) {
            // 위치 제공자가 비활성화되면 호출
        }

        @Override
        public void onProviderEnabled(String provider) {
            // 위치 제공자가 활성화되면 호출
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // 상태가 변경되면 호출
        }
    }

    public void onScanButtonClick(View view) {
        startDiscovery();
    }

    private void showGpsIndicator() {
        gpsIndicator.setVisibility(View.VISIBLE); // 파란 불 0.5초후에 사라짐
        new Handler().postDelayed(() -> gpsIndicator.setVisibility(View.GONE), 500);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                currentMyLocation();
            } else {
                Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_BLUETOOTH_CONNECT) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                continueBluetooth();
            } else {
                Toast.makeText(this, "Bluetooth 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startBluetoothConnection() {
        if (mStoreItem.getObdAddress() != null) {
            chosenDeviceAddress = mStoreItem.getObdAddress();
            Toast.makeText(this, mStoreItem.getObdAddress(), Toast.LENGTH_SHORT).show();
            startAndBindVehicleService();
            registerVehicleReceiver();
        } else {
            Toast.makeText(this, "OBD 기기가 선택되지 않음", Toast.LENGTH_SHORT).show();
            chooseBluetoothDevice();
        }
    }

    @Override
    protected void onDestroy() {
        stopService(new Intent(getApplicationContext(), FloatingHeadService.class));
        handler.removeCallbacksAndMessages(null);
        unregisterVehicleReceiver();
        unbindVehicleService();
        super.onDestroy();

        if (udpHandlerThread != null) {
            udpHandlerThread.quitSafely();  // HandlerThread 종료
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("show-diagnostic", mShowDiagnosticDialog);
        outState.putBoolean("is-detail-info-shown", mIsDetailInfoShown);
        outState.putBoolean("last-mil", mLastMil);
    }

    @Override
    public void finish() {
        super.finish();

        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_down);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        sensorManager.unregisterListener(this);
        if (!isFinishing()) {
            if (mVehicleService != null) {
                mVehicleService.startDataBackup();
            }

            if (!mIsShowDiagnosticActivity) {
                Intent service = new Intent(getApplicationContext(), FloatingHeadService.class);
                service.putExtra(FloatingHeadService.EXTRA_INTENT, "com.dashboard.obd.intent.ACTION_LAUNCH_DRIVING");
                startService(service);
            }
        }
        super.onPause();
    }
    @Override
    protected void onResume() {
        super.onResume();

        mIsShowDiagnosticActivity = false;
        stopService(new Intent(getApplicationContext(), FloatingHeadService.class));
        if (mVehicleService != null) {
            mVehicleService.stopDataBackup();
        }
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (mShowDetailInfo) {
            new Handler().post(this::toggleDetailInfo);
        }
    }
    @Override
    public void onBackPressed() {
        stopService(new Intent(getApplicationContext(), VehicleService.class));
        unregisterVehicleReceiver();
        super.finish();

    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        accel_x = event.values[0];
        accel_y = event.values[1];
        accel_z = event.values[2];
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Intent data = result.getData();
                if (data != null) {
                    int requestCode = Integer.parseInt(data.getStringExtra("requestCode"));
                    if (requestCode == ENABLE_BT_REQUEST) {
                        if (result.getResultCode() == RESULT_OK) {
                            continueBluetooth();
                        }
                        if (result.getResultCode() == RESULT_CANCELED) {
                            Toast.makeText(DrivingActivity.this, "Application requires Bluetooth enabled", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Log.e("DrivingActivity", "Received null data in onActivityResult");
                    // 적절한 처리 로직 추가 (예: 오류 메시지 또는 대체 행동)
                }
            }
    );

    private void startAndBindVehicleService() {
        startService(new Intent(DrivingActivity.this, VehicleService.class));

        bindService(new Intent(DrivingActivity.this, VehicleService.class), mVehicleServiceConnection, 0);
    }

    private void unbindVehicleService() {
        try {
            unbindService(mVehicleServiceConnection);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();

        }
        mVehicleService = null;
    }

    private final ServiceConnection mVehicleServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            try {
                mVehicleService = ((VehicleService.VehicleServiceBinder) binder).getService();
                Toast.makeText(DrivingActivity.this, "Service Connection", Toast.LENGTH_LONG).show();
                mVehicleService.resetVehiclePersistData("Capa_001");
                mVehicleService.disconnectVehicle();
                onVehicleEngineStatusChanged(mVehicleService.getVehicleEngineStatus());
                if (chosenDeviceAddress == null) {
                    chooseBluetoothDevice();
                } else {
                    Toast.makeText(DrivingActivity.this, chosenDeviceAddress, Toast.LENGTH_LONG).show();
                    mVehicleService.connectVehicle(chosenDeviceAddress, true);
                    registerVehicleReceiver();
                }
            } catch (Exception e){
                Log.e("ServiceConnection", "서비스 연결 오류");
            }
        }
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private void registerVehicleReceiver() {
        mBroadcastManager=LocalBroadcastManager.getInstance(this);
        unregisterVehicleReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED);
        filter.addAction(VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_EXTRA_DATA_RECEIVED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_BUSINIT_ERROR);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_NO_DATA);

        filter.addAction(DrivingService.ACTION_GOTO_EXIT);
        filter.addAction(DrivingService.ACTION_GOTO_HOME);
        filter.addAction(DrivingService.ACTION_GOTO_PAUSE);
        filter.addAction(DrivingService.ACTION_READY_TO_EXIT);
        filter.addAction(DrivingService.ACTION_ERS_TARGET_CHANGED);
        filter.addAction(DrivingService.ACTION_DIALOG_DISMISSED);
        filter.addAction(VehicleDataBroadcaster.ACTION_OBD_CLEAR_STORED_DTC);

        if (mVehicleReceiver!=null) {
            mBroadcastManager.registerReceiver(mVehicleReceiver, filter);
        }
    }

    private void unregisterVehicleReceiver() {
        try {
            mBroadcastManager.unregisterReceiver(mVehicleReceiver);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private final BroadcastReceiver mVehicleReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            if (isFinishing()) return;
            final String action = intent.getAction();

            if (VehicleDataBroadcaster.ACTION_OBD_STATE_CHANGED.equals(action)) {
                OnObdStateListener.ObdState obdState = (OnObdStateListener.ObdState) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_STATE);
                onObdStateChanged(obdState);
            } else if (VehicleDataBroadcaster.ACTION_VEHICLE_ENGINE_STATUS_CHANGED.equals(action)) {
                int ves = intent.getIntExtra(VehicleDataBroadcaster.EXTRA_VEHICLE_ENGINE_STATUS,
                        VehicleEngineStatus.UNKNOWN);
                onVehicleEngineStatusChanged(ves);
            } else if (VehicleDataBroadcaster.ACTION_OBD_DATA_RECEIVED.equals(action)) {
                ObdData obdData = (ObdData) intent.getSerializableExtra(VehicleDataBroadcaster.EXTRA_OBD_DATA);
                onObdDataReceived(obdData);
                try {
                    sendEngine_coolant_temperature = obdData.getInteger(KEY.SAE_ECT);
                } catch (Exception e) {
                    Log.e("sendData","sendEngine_coolant_temperature");
                    sendEngine_coolant_temperature = -1;
                }

                try {
                    sendIntake_manifold_absolute_pressure = obdData.getInteger(KEY.SAE_MAP);
                } catch (Exception e) {
                    Log.e("sendData","sendIntake_manifold_absolute_pressure");
                    sendIntake_manifold_absolute_pressure = -1;
                }

                try {
                    sendThrottle_position = obdData.getFloat(KEY.SAE_TP);
                } catch (Exception e) {
                    Log.e("sendData","sendThrottle_position");
                    sendThrottle_position = -1.0f;
                }

                try {
                    sendFuel_level = obdData.getFloat(KEY.SAE_FLI);
                } catch (Exception e) {
                    Log.e("sendData","sendFuel_level");
                    sendFuel_level = -1.0f;
                }

                try {
                    sendFuel_pressure = obdData.getInteger(KEY.SAE_FP);
                } catch (Exception e) {
                    Log.e("sendData","sendFuel_pressure");
                    sendFuel_pressure = -1;
                }

                try{
                    sendAccel_d = obdData.getFloat(KEY.SAE_ACCEL_D);
                } catch (Exception e) {
                    Log.e("sendData","sendAccel_D");
                    sendAccel_d = -1;
                }

                try{
                    sendEngine_load = obdData.getFloat(KEY.SAE_LOAD_PCT);
                } catch (Exception e) {
                    Log.e("sendData","sendEngine_load");
                    sendEngine_load = -1;
                }

                try{
                    sendShort_term_fuel_trim = obdData.getFloat(KEY.SAE_SFT_B1);
                } catch (Exception e) {
                    Log.e("sendData","sendShort_term_fuel_trim");
                    sendShort_term_fuel_trim = -1;
                }

                try{
                    sendLong_term_fuel_trim = obdData.getFloat(KEY.SAE_LFT_B1);
                } catch (Exception e) {
                    Log.e("sendData","sendLong_term_fuel_trim");
                    sendLong_term_fuel_trim = -1;
                }

                try{
                    sendAmbient_air_temperature = obdData.getInteger(KEY.SAE_AAT);
                } catch (Exception e) {
                    Log.e("sendData","sendAmbient_air_temperature");
                    sendAmbient_air_temperature = -1;
                }

                try{
                    sendEngine_oil_temperature = obdData.getInteger(KEY.SAE_EOT);
                } catch (Exception e) {
                    Log.e("sendData","sendEngine_oil_temperature");
                    sendEngine_oil_temperature = -1;
                }

                try{
                    sendMass_air_flow = obdData.getFloat(KEY.SAE_MAF);
                } catch (Exception e) {
                    Log.e("sendData","sendMass_air_flow");
                    sendMass_air_flow = -1;
                }
            } else if (VehicleDataBroadcaster.ACTION_OBD_EXTRA_DATA_RECEIVED.equals(action)) {
                float rpm;
                int vss;

                try {
                    rpm = intent.getFloatExtra(VehicleDataBroadcaster.EXTRA_RPM, -1);
                    sendRpm = rpm;
                } catch (Exception e) {
                    Log.e("sendData","sendRpm");
                    sendRpm = -1;
                }

                try {
                    vss = intent.getIntExtra(VehicleDataBroadcaster.EXTRA_VSS, -1);
                    sendSpeed = vss;
                } catch (Exception e) {
                    Log.e("sendData","sendSpeed");
                    sendSpeed = -1;
                }

                onObdExtraDataReceived(sendRpm, sendSpeed);
            }
            else if (VehicleDataBroadcaster.ACTION_OBD_CANNOT_CONNECT.equals(action)) {
                BluetoothDevice obdDevice = intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
                boolean isBlocked = intent.getBooleanExtra(VehicleDataBroadcaster.EXTRA_OBD_BLOCKED, false);
                onObdCannotConnect(obdDevice, isBlocked);
            } else if (VehicleDataBroadcaster.ACTION_OBD_DEVICE_NOT_SUPPORTED.equals(action)) {
                BluetoothDevice obdDevice = intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
                onObdDeviceNotSupported(obdDevice);
            } else if (VehicleDataBroadcaster.ACTION_OBD_PROTOCOL_NOT_SUPPORTED.equals(action)) {
                BluetoothDevice obdDevice = intent.getParcelableExtra(VehicleDataBroadcaster.EXTRA_OBD_DEVICE);
                onObdProtocolNotSupported(obdDevice);
            } else if (VehicleDataBroadcaster.ACTION_OBD_BUSINIT_ERROR.equals(action)) {
                Logger.getLogger(TAG).warn("BUSINIT_ERROR#" + intent.getIntExtra(VehicleDataBroadcaster.EXTRA_OBD_PROTOCOL, -1));
            } else if (VehicleDataBroadcaster.ACTION_OBD_NO_DATA.equals(action)) {
                Logger.getLogger(TAG).warn("No data!!");
            } else if (DrivingService.ACTION_GOTO_EXIT.equals(action)) {
                onGogoExit();
            } else if (DrivingService.ACTION_GOTO_HOME.equals(action)) {
                onGotoHome();
            } else if (DrivingService.ACTION_GOTO_PAUSE.equals(action)) {
                onGotoPause();
            } else if (DrivingService.ACTION_READY_TO_EXIT.equals(action)) {
                onReadyToExit();
            }
            else if (DrivingService.ACTION_DIALOG_DISMISSED.equals(action)) {
                Log.i(TAG, "onReceive#" + action);
                int type = intent.getIntExtra(DrivingService.EXTRA_DIALOG_TYPE, -1);
                onDialogDismiss(type);
            }
            else if (VehicleDataBroadcaster.ACTION_OBD_CLEAR_STORED_DTC.equals(action)) {
                if (mVehicleService != null) {
                    mVehicleService.clearStoredDTC();
                }
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                assert device != null;
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();
                if (deviceName != null && !deviceList.contains(deviceName)){
                    deviceList.add(deviceName + "\n" + deviceAddress);
                    adapterList.notifyDataSetChanged();
                }
            }
        }
    };

    private boolean mIsShowDiagnosticActivity;

    private void onObdStateChanged(ObdState obdState) {


        mIndicatorFragment.onObdStateChanged(obdState);
        mObdFragment.onObdStateChanged(obdState);
        mTripFragment.onObdStateChanged(obdState);

        if (obdState == ObdState.SCANNING) {
            if (mDrivingService != null) {
                mDrivingService.dismissObdRecoveryDialog();
            }
        }
        if (obdState==ObdState.CONNECTED&& mStoreItem.getObdAddress()==null)
        {

            mStoreItem.storeObdAddress(chosenDeviceAddress);
        }
    }

    private int mVes = VehicleEngineStatus.UNKNOWN;
    private void onVehicleEngineStatusChanged(int ves) {

        if (mDrivingService != null) {
            mDrivingService.dismissObdRecoveryDialog();
        }

        if (VehicleEngineStatus.isOnDriving(ves) && !VehicleEngineStatus.isOnDriving(mVes)) {
            moveTaskToFront();

            if (mVehicleService != null && mDrivingService != null) {
                mDrivingService.dismissDrivingOffDialog();

                if (mIsDrivingPaused) {
                    mIsDrivingPaused = false;

                    mVehicleService.resumeDriving();
                    mDrivingService.resumeDriving();

                    // Resume blackbox if needed
                    // FIXME: Need delay for screen rotation

                }
            }
        } else if (VehicleEngineStatus.isOffDriving(ves)) {
            moveTaskToFront();

            if (mVehicleService != null && mDrivingService != null) {
                if (!mDrivingService.isExitDialogShowing() && !mDrivingService.isDrivingOffDialogShowing()
                        && !mDrivingService.isWaitForExitDialogShowing()) {
                    boolean engineOnDetectionEnabled = isEngineOn();
                    if (!engineOnDetectionEnabled) mDrivingService.showDrivingOffDialog();
                }
            }
        }

        mVes = ves;
    }

    private boolean mShowDiagnosticDialog = true;
    private boolean mLastMil = false;
    protected String mDtc;

    private void onObdDataReceived(ObdData data) {
        mIndicatorFragment.onObdDataReceived(data);
        mObdFragment.onObdDataReceived(data);
        mTripFragment.onObdDataReceived(data);
        //mBottomFragment.onObdDataReceived(data);

        mDtc = getDTC(data);
        if (mDtc!=null) Toast.makeText(this, mDtc, Toast.LENGTH_SHORT).show();
        if (mShowDiagnosticDialog) {
            if (mDrivingService != null) {
                //showDiagnosticProcess(mDtc);
                mShowDiagnosticDialog = false;
                mLastMil = isMilOn(data);
            }
        } else if (isMilOn(data) && !mLastMil) {
            mLastMil = true;
            //showDiagnosticProcess(mDtc);
        }

        if (mIsDetailInfoShown) {
            mDetailInfoFragment.onObdDataReceived(data);
        }
    }

    String getDTC(ObdData data) {
        return data.getBoolean(KEY.SAE_MIL, false) ? data.getString(KEY.SAE_DTC) : null;
    }

    boolean isMilOn(ObdData data) {
        return !TextUtils.isEmpty(getDTC(data));
    }

    int mLastVss = -1;
    float mLastRpm = -1;
    private void onObdExtraDataReceived(float rpm, int vss) {
        mObdFragment.onObdExtraDataReceived(rpm, vss);
        mTripFragment.onObdExtraDataReceived(rpm, vss);

        if (mIsDetailInfoShown) {
            mDetailInfoFragment.onObdExtraDataReceived(rpm, vss);
        }

        mLastVss = vss ;
        mLastRpm = rpm;

        runOnUiThread(mUpdateVssRunnable);
    }

    private final Runnable mUpdateVssRunnable = new Runnable() {
        public void run() {

            if(mPreviewSpeedMeter != null) {
                mPreviewSpeedMeter.setValueText(String.valueOf(mLastVss));
            }

            if (mIsDetailInfoShown) {
                mDetailInfoFragment.onObdExtraDataReceived(mLastRpm, mLastVss);
            }

            if(mLastRpm > 0 && mLastVss > 0) {
                animatePreviewSpeedControl(View.VISIBLE);
            }else {
                animatePreviewSpeedControl(View.GONE);
            }

        }
    };

    private void onObdCannotConnect(BluetoothDevice obdDevice, boolean isBlocked) {

        mIndicatorFragment.onObdCannotConnect();
        mObdFragment.onObdCannotConnect();
        mTripFragment.onObdCannotConnect();
        if (mVehicleService != null) {
            mVehicleService.disconnectVehicle();

            if (obdDevice.getAddress()!=null) {
                mVehicleService.connectVehicle(chosenDeviceAddress);//chosenDeviceAddress

            }
        }
        if (isBlocked) {
            Logger.getLogger(TAG).info("device is blocked@onObdCannotConnect");
           if (mDrivingService != null) {
                mDrivingService.showObdRecoveryDialog();
            }
        }
    }

    private void onObdDeviceNotSupported(BluetoothDevice obdDevice) {

        onObdCannotConnect(obdDevice, false);
    }

    private void onObdProtocolNotSupported(BluetoothDevice obdDevice) {

        onObdCannotConnect(obdDevice, false);
    }

    private void onGogoExit() {
      //  logger.debug("onGogoExit()");

        if (mVehicleService != null) {
            mVehicleService.disconnectVehicle();
        }

        Intent result = new Intent();
        result.putExtra(EXTRA_REQUEST_EXIT, true);
        setResult(RESULT_OK, result);

        waitForExit();
    }

    private void onGotoHome() {
        //log.debug("onGotoHome()");

        // Home button
        Intent result = new Intent();
        if (mVehicleService != null) {
            result.putExtra(EXTRA_VES, mVehicleService.getVehicleEngineStatus());
        }
        setResult(RESULT_OK, result);

        waitForExit();
    }

    private void onGotoPause() {
       // logger.debug("onGotoPause()");

        // Pause button
        moveTaskToBack(true);

        if (!mIsDrivingPaused) {
            mIsDrivingPaused = true;

            if (mVehicleService != null) {
                mVehicleService.pauseDriving();
            }
            if (mDrivingService != null) {
                mDrivingService.pauseDriving();
            }

        }
    }

    private void onReadyToExit() {


        unregisterVehicleReceiver();

        // Finish at next thread time
        mContentView.post(this::finish);
    }



    private void onDialogDismiss(int type) {
       // logger.debug("onDialogDismiss(): type=" + type);


    }

    private void moveTaskToFront() {
       if (isPaused()) {
            Intent bringToFront = new Intent(BringToFrontReceiver.ACTION_BRING_TO_FRONT);
            sendBroadcast(bringToFront);
        }
    }



    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.detail_info_btn) {
            toggleDetailInfo();
        }else if (id==R.id.iv_btn_preview)
        {
            chooseBluetoothDevice();

        }
    }
    private void chooseBluetoothDevice(){
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null){
            Toast.makeText(this, "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
            return;
        }
        if(!btAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            StartActivityForResults.startActivityForResults(activityResultLauncher,enableBtIntent, ENABLE_BT_REQUEST);
        } else{
            continueBluetooth();
        }
    }
    private void continueBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_BLUETOOTH_CONNECT);
                return;
            }
        }

        // 권한이 허용된 경우에만 블루투스 작업을 계속 진행
        final ArrayList<String> pairedDevicesNames = new ArrayList<>();
        final ArrayList<String> pairedDevicesAddresses = new ArrayList<>();

        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesNames.add(device.getName());
                pairedDevicesAddresses.add(device.getAddress());
            }

            final String[] devicesString = pairedDevicesNames.toArray(new String[0]);
            final String[] devicesAddress = pairedDevicesAddresses.toArray(new String[0]);

            AlertDialog.Builder mBuilder = new AlertDialog.Builder(this, R.style.MyDialog);
            mBuilder.setTitle("Choose OBD device:");

            mBuilder.setSingleChoiceItems(devicesString, -1, (dialog, i) -> {
                dialog.dismiss();
                int position = ((AlertDialog) dialog).getListView().getCheckedItemPosition();

                chosenDeviceAddress = pairedDevicesAddresses.get(position);
                chosenDeviceName = pairedDevicesNames.get(position);
                Toast.makeText(this, "Chosen: " + chosenDeviceName, Toast.LENGTH_SHORT).show();

                if (chosenDeviceAddress != null) {
                    startAndBindVehicleService();
                    registerVehicleReceiver();
                }
            });

            AlertDialog mDialog = mBuilder.create();
            mDialog.show();
        } else {
            Toast.makeText(this, "No paired devices found", Toast.LENGTH_SHORT).show();
        }

        if (chosenDeviceAddress != null) {
            startAndBindVehicleService();
            registerVehicleReceiver();
        }
    }


    private void toggleDetailInfo() {
        if (mIsDetailInfoAnimating) {
            return;
        }

        if (mIsDetailInfoShown && !mIsDetailInfoAnimating) {
            try {
                mIsDetailInfoShown = false;

                AnimatorSet animSet = new AnimatorSet();
                animSet.setInterpolator(new DecelerateInterpolator());
                animSet.setDuration(500);

                View leftView = mObdFragment.getView();
                ObjectAnimator leftAnim = ObjectAnimator.ofFloat(leftView, "translationX", 0);

                View rightView = mTripFragment.getView();
                ObjectAnimator rightAnim = ObjectAnimator.ofFloat(rightView, "translationX", 0);

                View centerView1 = mIndicatorFragment.getView().findViewById(R.id.indicator_pane);
                ObjectAnimator centerAnim1 = ObjectAnimator.ofFloat(centerView1, "alpha", 1);

                View detailInfoView = mDetailInfoFragment.getView();
                ObjectAnimator detailInfoAnim = ObjectAnimator.ofFloat(detailInfoView, "alpha", 0);

                animSet.playTogether(leftAnim, rightAnim, centerAnim1, /*centerAnim2,*/ detailInfoAnim);
                animSet.addListener(new Animator.AnimatorListener() {
                    public void onAnimationRepeat(Animator animation) {
                    }

                    public void onAnimationCancel(Animator animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mIsDetailInfoAnimating = false;

                        FragmentManager fm = getSupportFragmentManager();
                        fm.beginTransaction().hide(mDetailInfoFragment).commitAllowingStateLoss();

                        ((CheckBox) findViewById(R.id.detail_info_btn)).setChecked(false);

                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                        mIsDetailInfoAnimating = true;

                    }
                });
                animSet.start();
            } catch(Exception e){
                Log.e("toggleDetailInfo", "toggleDetailInfo 에러");
            }

        } else if (!mIsDetailInfoShown && !mIsDetailInfoAnimating) {
            mIsDetailInfoShown = true;

            AnimatorSet animSet = new AnimatorSet();
            animSet.setInterpolator(new DecelerateInterpolator());
            animSet.setDuration(500);

            View leftView = mObdFragment.getView();
            ObjectAnimator leftAnim = ObjectAnimator.ofFloat(leftView, "translationX", -leftView.getWidth());

            View rightView = mTripFragment.getView();
            ObjectAnimator rightAnim = ObjectAnimator.ofFloat(rightView, "translationX", rightView.getWidth());

            View centerView1 = mIndicatorFragment.getView().findViewById(R.id.indicator_pane);
            ObjectAnimator centerAnim1 = ObjectAnimator.ofFloat(centerView1, "alpha", 0);

            if (isPortrait()) {
                centerAnim1 = ObjectAnimator.ofFloat(centerView1, "alpha", 1);
            }

            View detailInfoView = mDetailInfoFragment.getView();
            ObjectAnimator detailInfoAnim = ObjectAnimator.ofFloat(detailInfoView, "alpha", 1);

            animSet.playTogether(leftAnim, rightAnim, centerAnim1, /*centerAnim2,*/ detailInfoAnim);
            animSet.addListener(new Animator.AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                    mIsDetailInfoAnimating = true;

                    FragmentManager fm = getSupportFragmentManager();
                    fm.beginTransaction().show(mDetailInfoFragment).commitAllowingStateLoss();
                }

                public void onAnimationEnd(Animator animation) {
                    mIsDetailInfoAnimating = false;

                    ((CheckBox)findViewById(R.id.detail_info_btn)).setChecked(true);
                }

                public void onAnimationRepeat(Animator animation) {}
                public void onAnimationCancel(Animator animation) {}
            });
            animSet.start();
        }
    }



    private void waitForExit() {

    }

    private void animatePreviewSpeedControl(int visible) {
        if(visible == View.VISIBLE) {
            if(mPreviewSpeedControl != null && mPreviewSpeedControlShown != View.VISIBLE) {
                View v = mPreviewSpeedControl;
                //mPreviewSpeedControl.setVisibility(View.VISIBLE);
                ObjectAnimator leftAnim = ObjectAnimator.ofFloat(v, "translationX", 0);
                leftAnim.setDuration(500);
                leftAnim.start();
                mPreviewSpeedControlShown = View.VISIBLE;
            }
        } else if(visible == View.GONE) {
            if(mPreviewSpeedControl != null && mPreviewSpeedControlShown != View.GONE) {
                View v = (View) mPreviewSpeedControl;
                int mPreviewSpeedControlW = 0;
                ObjectAnimator leftAnim = ObjectAnimator.ofFloat(v, "translationX", -mPreviewSpeedControlW);
                leftAnim.setDuration(500);
                leftAnim.addListener(new Animator.AnimatorListener() {
                    public void onAnimationRepeat(Animator animation) {}
                    public void onAnimationCancel(Animator animation) {}

                    @Override
                    public void onAnimationEnd(Animator animation) {
                    }

                    @Override
                    public void onAnimationStart(Animator animation) {
                    }
                });
                leftAnim.start();
                mPreviewSpeedControlShown = View.GONE;
            }
        }
    }
    private FuelType mFuelType;

    protected int calcEcoLevel(ObdData data) {
        int ecoLevel = 0;

        if (mFuelType == FuelType.GASOLINE || mFuelType == FuelType.LPG) {
            if (data.isValid(KEY.SAE_VSS) && data.isValid(KEY.SAE_LOAD_PCT)) {
                int vss = data.getInteger(KEY.SAE_VSS);
                if (vss > 0) {
                    float loadPct = data.getFloat(KEY.SAE_LOAD_PCT);
                    if (loadPct < 50) {
                        ecoLevel = 1;
                    } else if (50 <= loadPct && loadPct < 80) {
                        ecoLevel = 2;
                    } else if (80 <= loadPct && loadPct < 90) {
                        ecoLevel = 3;
                    } else if (90 <= loadPct) {
                        ecoLevel = 4;
                    }
                }
            }
        } else if (mFuelType == FuelType.DIESEL) {
            if (data.isValid(KEY.SAE_VSS) && data.isValid(KEY.SAE_MAP)) {
                int vss = data.getInteger(KEY.SAE_VSS);
                if (vss > 0) {
                    int map = data.getInteger(KEY.SAE_MAP);
                    int baro = data.getInteger(KEY.SAE_BARO, 100);
                    int mapBaroDiff = (map - baro);

                    if (mapBaroDiff < 50) {
                        ecoLevel = 1;
                    } else if (mapBaroDiff < 80) {
                        ecoLevel = 2;
                    } else if (mapBaroDiff < 100) {
                        //by jake 09.16 (90->100)
                        ecoLevel = 3;
                    } else {
                        ecoLevel = 4;
                    }
                }
            }
        }

        return ecoLevel;
    }
    private boolean isPortrait() {
        return getWindowManager().getDefaultDisplay().getWidth() < getWindowManager().getDefaultDisplay().getHeight();
    }
    protected boolean isEngineOn() {
        return true;
    }
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
    }
    public boolean isPaused() {
        return mIsPaused;
    }
}
