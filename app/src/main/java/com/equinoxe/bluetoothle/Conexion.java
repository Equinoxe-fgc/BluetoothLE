package com.equinoxe.bluetoothle;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;


public class Conexion extends AppCompatActivity {
    BluetoothGatt btGatt;
    List<BluetoothGattService> listServices;
    BluetoothServiceInfoList listaServicesInfo;
    private final Handler handler = new Handler();

    int iNumDevices;
    String[] sAddresses = new String[8];

    private TextView txtPeriodo;
    private Button btnStart;
    private CheckBox chkGPS;
    private CheckBox chkSendServer;
    private CheckBox chkTiempo;
    private CheckBox chkLogCurrent;
    private TextView txtTiempo, txtMaxInterval, txtMinInterval, txtLatency, txtTimeout, txtPeriodoMaxRes;
    private CheckBox chkWebNavigation;
    private RecyclerView recyclerViewSensores;
    private MiAdaptadorSensores adaptadorSensores;
    private RecyclerView.LayoutManager layoutManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conexion);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        recyclerViewSensores = findViewById(R.id.recyclerViewSensores);
        txtPeriodo = findViewById(R.id.txtPeriodo);
        btnStart = findViewById(R.id.btnStart);
        chkGPS = findViewById(R.id.chkGPS);
        chkSendServer = findViewById(R.id.chkEnvioServidor);
        chkTiempo = findViewById(R.id.chkTiempo);
        txtTiempo = findViewById(R.id.txtTiempo);
        chkWebNavigation = findViewById(R.id.chkWebNavigation);

        chkLogCurrent = findViewById(R.id.chkLogConsumoCorriente);

        txtMaxInterval = findViewById(R.id.txtMAX_INTERVAL);
        txtMinInterval = findViewById(R.id.txtMIN_INTERVAL);
        txtLatency = findViewById(R.id.txtLatency);
        txtTimeout = findViewById(R.id.txtTimeout);
        txtPeriodoMaxRes = findViewById(R.id.txtPeriodoMaxRes);

        listaServicesInfo = new BluetoothServiceInfoList();

        adaptadorSensores = new MiAdaptadorSensores(this, listaServicesInfo);
        layoutManager = new LinearLayoutManager(this);

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        Bundle extras = getIntent().getExtras();
        iNumDevices = extras.getInt("NumDevices");
        for (int i = 0; i < iNumDevices; i++)
            sAddresses[i] = extras.getString("Address" + i);

        BluetoothDevice device = adapter.getRemoteDevice(sAddresses[0]);

        btGatt = device.connectGatt(this, false, mBluetoothGattCallback);

        handler.removeCallbacks(sendUpdatesToUI);


        chkTiempo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (((CompoundButton) view).isChecked()){
                    txtTiempo.setEnabled(true);
                } else {
                    txtTiempo.setEnabled(false);
                }
            }
        });
    }


    @Override
    public void onBackPressed() {
        //btGatt.disconnect();
        //btGatt.close();

        super.onBackPressed();
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {
            BluetoothGattService service;
            for (int i = 0; i < listServices.size(); i++) {
                service = listServices.get(i);
                if (service != null) {
                    String sServiceName = getServiceName(service.getUuid().toString());
                    if (sServiceName.length() != 0) {
                        BluetoothServiceInfo serviceInfo;

                        /*if (sServiceName.compareTo(getString(R.string.Motion)) != 0) {
                            serviceInfo = new BluetoothServiceInfo(true, sServiceName, service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                        }*/

                        /*if (sServiceName.compareTo(getString(R.string.Barometer)) == 0) {   // El servicio de barómetro también tiene el de temperatura
                            serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Temperature), service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                        } else*/ if (sServiceName.compareTo(getString(R.string.Motion)) == 0) {   // El servicio de movimiento tiene giróscopo, acelerómetro y magnetómetro
                            serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Gyroscope), service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                            serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Accelerometer), service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                            serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Magnetometer), service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);

                            btnStart.setEnabled(true);
                        }
                    }
                }
            }

            if (listServices.size() == 0)
                handler.postDelayed(this, 1000); // 1 seconds
            else {
                // Se desconecta una vez encontrados los servicios
                recyclerViewSensores.setAdapter(adaptadorSensores);
                recyclerViewSensores.setLayoutManager(layoutManager);
                btGatt.disconnect();
                btGatt.close();
            }
        }
    };

    String getServiceName(String UUID) {
        String sServiceName = "";

        if (UUID.compareToIgnoreCase(UUIDs.UUID_BAR_SERV.toString()) == 0)
            sServiceName = getString(R.string.Barometer);
        else if (UUID.compareToIgnoreCase(UUIDs.UUID_HUM_SERV.toString()) == 0)
            sServiceName = getString(R.string.Humidity);
        else if (UUID.compareToIgnoreCase(UUIDs.UUID_OPT_SERV.toString()) == 0)
            sServiceName = getString(R.string.Light);
        else if (UUID.compareToIgnoreCase(UUIDs.UUID_MOV_SERV.toString()) == 0)
            sServiceName = getString(R.string.Motion);

        return sServiceName;
    }

    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS)
                if (newState == BluetoothGatt.STATE_CONNECTED)
                    btGatt.discoverServices();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                listServices = gatt.getServices();
                handler.postDelayed(sendUpdatesToUI, 1000); // 1 second
            }
        }
    };

    public void onStartSingle(View v) {
        //btGatt.disconnect();
        //btGatt.close();
        if (txtMaxInterval.getText().toString().length() == 0)
            txtMaxInterval.setText("0");
        int iMaxInterval = Integer.valueOf(txtMaxInterval.getText().toString());
        if ((iMaxInterval != 0 && iMaxInterval < 0x06) || iMaxInterval > 0xC80) {
            Toast.makeText(this, getString(R.string.ERROR_MAX_INTERVAL), Toast.LENGTH_LONG).show();
            return;
        }

        if (txtMinInterval.getText().toString().length() == 0)
            txtMinInterval.setText("0");
        int iMinInterval = Integer.valueOf(txtMinInterval.getText().toString());
        if ((iMinInterval != 0 && iMinInterval < 0x06) || iMinInterval > 0xC80) {
            Toast.makeText(this, getString(R.string.ERROR_MIN_INTERVAL), Toast.LENGTH_LONG).show();
            return;
        }

        if (iMaxInterval < iMinInterval) {
            Toast.makeText(this, getString(R.string.ERROR_MAX_MIN_INTERVAL), Toast.LENGTH_SHORT).show();
            return;
        }

        if (txtLatency.getText().toString().length() == 0)
            txtLatency.setText("0");
        int iLatency = Integer.valueOf(txtLatency.getText().toString());
        if (iLatency > 0x3E8) {
            Toast.makeText(this, getString(R.string.ERROR_LATENCY), Toast.LENGTH_LONG).show();
            return;
        }


        if (txtTimeout.getText().toString().length() == 0)
            txtTimeout.setText("0");
        int iTimeout = Integer.valueOf(txtTimeout.getText().toString());
        if ((iTimeout != 0 && iTimeout < 10) || iTimeout > 0xC80) {
            Toast.makeText(this, getString(R.string.ERROR_TIMEOUT), Toast.LENGTH_LONG).show();
            return;
        }


        if (txtPeriodoMaxRes.getText().toString().length() == 0)
            txtPeriodoMaxRes.setText("0");
        int iPeriodoMaxRes = Integer.valueOf(txtPeriodoMaxRes.getText().toString());


        Intent intent = new Intent(this, Datos.class);
        intent.putExtra("NumDevices", iNumDevices);
        for (int i = 0; i < iNumDevices; i++)
            intent.putExtra("Address" + i, sAddresses[i]);
        intent.putExtra("Periodo", Integer.valueOf(txtPeriodo.getText().toString()));

        for (int i = 0; i < listaServicesInfo.getSize(); i++) {
            BluetoothServiceInfo serviceInfo = listaServicesInfo.getBluetoothServiceInfo(i);
            String sName = serviceInfo.getName();

            if (sName.compareTo(getString(R.string.Humidity)) == 0)
                intent.putExtra("Humedad", serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Barometer)) == 0)
                intent.putExtra("Barometro", serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Light)) == 0)
                intent.putExtra("Luz", serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Temperature)) == 0)
                intent.putExtra("Temperatura", serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Gyroscope)) == 0)
                intent.putExtra("Giroscopo", serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Accelerometer)) == 0)
                intent.putExtra("Acelerometro", serviceInfo.isSelected());
            else if (sName.compareTo(getString(R.string.Magnetometer)) == 0)
                intent.putExtra("Magnetometro", serviceInfo.isSelected());
        }

        intent.putExtra("LOGCurrent", chkLogCurrent.isChecked());

        intent.putExtra("Location", chkGPS.isChecked());
        intent.putExtra("SendServer", chkSendServer.isChecked());
        intent.putExtra("bTime",chkTiempo.isChecked());
        intent.putExtra("WebNavigation", chkWebNavigation.isChecked());
        if (!chkTiempo.isChecked())
            txtTiempo.setText("0");
        long lTime = 1000*Integer.valueOf(txtTiempo.getText().toString());
        intent.putExtra("Time", lTime);


        intent.putExtra("MaxInterval", iMaxInterval);
        intent.putExtra("MinInterval", iMinInterval);
        intent.putExtra("Latency", iLatency);
        intent.putExtra("Timeout", iTimeout);
        intent.putExtra("PeriodoMaxRes", iPeriodoMaxRes);

        startActivity(intent);
    }

    public void onStartBatch(View v) {

    }
}
