package com.equinoxe.bluetoothle;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.browse.MediaBrowser;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;


public class MainActivity extends AppCompatActivity {
    final private String SENSORTAG_STRING = "CC2650 SensorTag";

    private boolean bScanning = false;
    private BluetoothLeScannerCompat scanner;

    private Button btnScan, btnConnect;
    private RecyclerView recyclerView;
    private MiAdaptador adaptador;
    private RecyclerView.LayoutManager layoutManager;
    private final Handler handler = new Handler();

    private BluetoothDeviceInfoList btDeviceInfoList;

    private int iContador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btDeviceInfoList = new BluetoothDeviceInfoList();

        btnScan = findViewById(R.id.btnScan);
        btnConnect = findViewById(R.id.btnConnect);
        recyclerView = findViewById(R.id.recycler_view);

        adaptador = new MiAdaptador(this, btDeviceInfoList, this);
        layoutManager = new LinearLayoutManager(this);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }

        iContador = 0;
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if (bScanning) {
                    iContador++;
                    if (btDeviceInfoList.getSize() != 0)
                        adaptador.notifyDataSetChanged();
                    if (iContador == 4)
                        btnScanOnClick(btnScan);
                } else
                    iContador = 0;

                handler.postDelayed(this, 1000);
            }
        });
    }

    public void btnScanOnClick(View v) {
        if (bScanning) {
            btnScan.setText(getString(R.string.scan));
            scanner.stopScan(mScanCallback);

            if (btDeviceInfoList.getSize() != 0) {
                recyclerView.setAdapter(adaptador);
                recyclerView.setLayoutManager(layoutManager);
            }
        } else {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mBluetoothAdapter.isEnabled()) {
                Toast.makeText(this, getString(R.string.EnableBluetooth), Toast.LENGTH_LONG).show();
                return;
            }

            btDeviceInfoList.clearAllBluetoothDeviceInfo();
            adaptador.notifyDataSetChanged();

            scanner = BluetoothLeScannerCompat.getScanner();

            // We want to receive a list of found devices every second
            /*ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(1000)
                    .build();*/

            checkForPermissions();
            scanner.startScan(mScanCallback);

            //handler.removeCallbacks(sendUpdatesToUI);

            btnScan.setText(getString(R.string.stop));
        }

        bScanning = !bScanning;
    }

    /*private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            if (btDeviceInfoList.getSize() != 0) {
                recyclerView.setAdapter(adaptador);
                recyclerView.setLayoutManager(layoutManager);
            } else
                handler.postDelayed(this, 1000); // 1 seconds
        }
    };*/

    private void checkForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            switch (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                case PackageManager.PERMISSION_DENIED:
                    if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        int REQUEST_ACCESS_COARSE_LOCATION = 1;
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                REQUEST_ACCESS_COARSE_LOCATION);
                    }
                    break;
                case PackageManager.PERMISSION_GRANTED:
                    break;
            }
        }
    }

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (!btDeviceInfoList.isPresent(device.getAddress())) {
                String sName = device.getName();
                if (sName != null && sName.compareTo(SENSORTAG_STRING) == 0) {
                    BluetoothDeviceInfo btDeviceInfo = new BluetoothDeviceInfo(false, device.getName(), device.getAddress(), device);
                    btDeviceInfoList.addBluetoothDeviceInfo(btDeviceInfo);
                    //btnScanOnClick(null);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            // Scan error
        }
    };

    public BluetoothDeviceInfoList getBtDeviceInfoList() {
        return btDeviceInfoList;
    }

    public void btnConnectClick(View v) {
        int iNumSelected = btDeviceInfoList.getNumSelected();

        Intent intent = new Intent(this, Conexion.class);
        intent.putExtra("NumDevices", iNumSelected);

        int iPos = 0;
        for (int i = 0; i < btDeviceInfoList.getSize(); i++)
            if (btDeviceInfoList.getBluetoothDeviceInfo(i).isSelected()) {
                intent.putExtra("Address" + iPos, btDeviceInfoList.getBluetoothDeviceInfo(i).getAddress());
                iPos++;
            }

        startActivity(intent);
    }

    public void notifySomeSelected(boolean bSomeSelected) {
        if (bSomeSelected)
            btnConnect.setVisibility(View.VISIBLE);
        else
            btnConnect.setVisibility(View.INVISIBLE);
    }

    public void connectOne(int iPos) {
        Intent intent = new Intent(this, Conexion.class);
        intent.putExtra("NumDevices", 1);
        intent.putExtra("Address0", btDeviceInfoList.getBluetoothDeviceInfo(iPos).getAddress());
        startActivity(intent);
   }
}
