package com.equinoxe.bluetoothle;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;


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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent i = new Intent(this, Settings.class);
                startActivity(i);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //verifyStoragePermissions();

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

        btnScanOnClick(btnScan);
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
            btnConnect.setVisibility(View.INVISIBLE);

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
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {  // Only ask for these permissions on runtime when running Android 6.0 or higher
            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION} , 1);
            }

            permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }

            permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);
            }

            permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WAKE_LOCK}, 1);
            }
        }
    }

    /*private void verifyStoragePermissions() {
        // Check if we have write permission
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (Build.VERSION.SDK_INT >= 23) {
            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);
            }
        }
    }*/


    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (!btDeviceInfoList.isPresent(device.getAddress())) {
                String sName = device.getName();
                if (sName != null && sName.compareTo(SENSORTAG_STRING) == 0) {
                    BluetoothDeviceInfo btDeviceInfo = new BluetoothDeviceInfo(false, device.getName(), device.getAddress());
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
