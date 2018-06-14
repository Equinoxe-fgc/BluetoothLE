package com.equinoxe.bluetoothle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
    String sAddresses[] = new String[8];

    private TextView txtPeriodo;
    private Button btnStartBatch;
    private RecyclerView recyclerViewSensores;
    private MiAdaptadorSensores adaptadorSensores;
    private RecyclerView.LayoutManager layoutManager;
    private boolean bFicheroLotes;
    private int iNumSimulacionesLotes;
    private int iSimulationTime[];
    private boolean bSimulationParameters[][];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conexion);

        recyclerViewSensores = findViewById(R.id.recyclerViewSensores);
        txtPeriodo = findViewById(R.id.txtPeriodo);
        btnStartBatch = findViewById(R.id.btnStartBat);

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

        bFicheroLotes = procesaFicheroLotes();

        if (bFicheroLotes)
            btnStartBatch.setVisibility(Button.VISIBLE);
    }

    private boolean procesaFicheroLotes() {
        boolean bFicheroLotes = true;

        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard,"simulBT.bat");

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            line = br.readLine();
            iNumSimulacionesLotes = Integer.parseInt(line);
            iSimulationTime = new int[iNumSimulacionesLotes];
            bSimulationParameters = new boolean[iNumSimulacionesLotes][7];

            for (int i = 0; i < iNumSimulacionesLotes; i++) {
                line = br.readLine();
                int iPosSeparador = line.indexOf(" ");
                iSimulationTime[i] = Integer.parseInt(line.substring(0, iPosSeparador - 1));

                for (int iSensor = 0; iSensor < 7; iSensor++) {
                    bSimulationParameters[i][iSensor] = line.charAt(iPosSeparador + iSensor + 1) != '0';
                }
            }
            br.close();
        }
        catch (IOException e) {
            bFicheroLotes = false;
        }

        return bFicheroLotes;
    }

    @Override
    public void onBackPressed() {
        btGatt.disconnect();
        btGatt.close();

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

                        if (sServiceName.compareTo(getString(R.string.Motion)) != 0) {
                            serviceInfo = new BluetoothServiceInfo(true, sServiceName, service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                        }

                        if (sServiceName.compareTo(getString(R.string.Barometer)) == 0) {   // El servicio de barómetro también tiene el de temperatura
                            serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Temperature), service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                        } else if (sServiceName.compareTo(getString(R.string.Motion)) == 0) {   // El servicio de movimiento tiene giróscopo, acelerómetro y magnetómetro
                            serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Gyroscope), service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                            serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Accelerometer), service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                            serviceInfo = new BluetoothServiceInfo(true, getString(R.string.Magnetometer), service.getUuid().toString());
                            listaServicesInfo.addBluetoothServiceInfo(serviceInfo);
                        }
                    }
                }
            }
            if (listServices.size() == 0)
                handler.postDelayed(this, 1000); // 1 seconds
            else {
                recyclerViewSensores.setAdapter(adaptadorSensores);
                recyclerViewSensores.setLayoutManager(layoutManager);
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
        btGatt.disconnect();
        btGatt.close();

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

        startActivity(intent);
    }

    public void onStartBatch(View v) {

    }
}
