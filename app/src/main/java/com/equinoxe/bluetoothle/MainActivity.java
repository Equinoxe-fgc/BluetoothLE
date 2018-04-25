package com.equinoxe.bluetoothle;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.CountDownTimer;
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
    private boolean bScanning = false;
    private BluetoothLeScannerCompat scanner;

    private Button btnScan, btnConnect;
    private RecyclerView recyclerView;
    private MiAdaptador adaptador;
    private RecyclerView.LayoutManager layoutManager;

    private BluetoothDeviceInfoList btDeviceInfoList;

    private UUID SERVICE_UUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btDeviceInfoList = new BluetoothDeviceInfoList();

        btnScan = (Button)findViewById(R.id.btnScan);
        btnConnect = (Button)findViewById(R.id.btnConnect);
        recyclerView = (RecyclerView)findViewById(R.id.recycler_view);

        adaptador = new MiAdaptador(this, btDeviceInfoList, this);
        layoutManager = new LinearLayoutManager(this);
        adaptador.setOnItemClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "elemento " + recyclerView.getChildAdapterPosition(v), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void btnScanOnClick(View v) {
        if (bScanning) {
            btnScan.setText(R.string.scan);
            scanner.stopScan(mScanCallback);

            if (btDeviceInfoList.getSize() != 0) {
                recyclerView.setAdapter(adaptador);
                recyclerView.setLayoutManager(layoutManager);
            }
        } else {
            btDeviceInfoList.clearAllBluetoothDeviceInfo();

            scanner = BluetoothLeScannerCompat.getScanner();

            // We want to receive a list of found devices every second
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(1000)
                    .build();

            SERVICE_UUID = UUID.fromString("b993bf90-81e1-11e4-b4a9-0800200c9a66");
            // We only want to scan for devices advertising our custom service
            ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_UUID)).build();
            checkForPermissions();
            //scanner.startScan(Arrays.asList(scanFilter), settings, mScanCallback);
            scanner.startScan(mScanCallback);

            btnScan.setText(R.string.stop);
        }

        bScanning = !bScanning;
    }

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
                BluetoothDeviceInfo btDeviceInfo = new BluetoothDeviceInfo(false, device.getName(), device.getAddress());
                btDeviceInfoList.addBluetoothDeviceInfo(btDeviceInfo);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (int i = 0; i < results.size(); i++) {
                ScanResult result = results.get(i);
                BluetoothDevice device = result.getDevice();

                BluetoothDeviceInfo btDeviceInfo = new BluetoothDeviceInfo(false, device.getName(), device.getAddress());
                btDeviceInfoList.addBluetoothDeviceInfo(btDeviceInfo);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            // Scan error
        }
    };


    public void btnConnectClick(View v) {

    }

    public void notifySomeSelected(boolean bSomeSelected) {
        if (bSomeSelected)
            btnConnect.setVisibility(View.VISIBLE);
        else
            btnConnect.setVisibility(View.INVISIBLE);
    }
}
