package com.equinoxe.bluetoothle;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;


public class Datos extends AppCompatActivity {
    final static long lTiempoMuestraDatos = 120 * 1000;  // Tiempo de muestra de datos

    BluetoothDataList listaDatos;

    private MiAdaptadorDatos adaptadorDatos;
    private TextView txtLongitud;
    private  TextView txtLatitud;

    private boolean bHumedad, bBarometro, bLuz, bTemperatura, bAcelerometro, bGiroscopo, bMagnetometro;
    private boolean bSensores[][];
    private boolean bActivacion[][];
    private boolean bConfigPeriodo[][];
    private int iPeriodo;
    private int iNumDevices;
    private String sAddresses[] = new String[8];

    Handler handler;

    boolean bSensing;

    boolean bLocation;
    boolean bSendServer;

    DecimalFormat df;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datos);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        RecyclerView recyclerViewDatos;
        RecyclerView.LayoutManager layoutManager;

        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        df = new DecimalFormat("###.##");

        Bundle extras = getIntent().getExtras();
        iNumDevices = extras.getInt("NumDevices");

        bSensores = new boolean[iNumDevices][4];
        bActivacion = new boolean[iNumDevices][4];
        bConfigPeriodo = new boolean[iNumDevices][4];

        for (int i = 0; i < iNumDevices; i++)
            sAddresses[i] = extras.getString("Address" + i);
        iPeriodo = extras.getInt("Periodo");

        bHumedad = extras.getBoolean("Humedad");
        bBarometro = extras.getBoolean("Barometro");
        bLuz = extras.getBoolean("Luz");
        bTemperatura = extras.getBoolean("Temperatura");
        bAcelerometro = extras.getBoolean("Acelerometro");
        bGiroscopo = extras.getBoolean("Giroscopo");
        bMagnetometro = extras.getBoolean("Magnetometro");

        bLocation = extras.getBoolean("Location");
        bSendServer = extras.getBoolean("SendServer");

        recyclerViewDatos = findViewById(R.id.recycler_viewDatos);
        txtLatitud = findViewById(R.id.textViewLatitud);
        txtLongitud = findViewById(R.id.textViewLongitud);

        listaDatos = new BluetoothDataList(iNumDevices, sAddresses);

        adaptadorDatos = new MiAdaptadorDatos(this, listaDatos);
        layoutManager = new LinearLayoutManager(this);

        recyclerViewDatos.setAdapter(adaptadorDatos);
        recyclerViewDatos.setLayoutManager(layoutManager);

        if (bLocation) {
            txtLongitud.setVisibility(View.VISIBLE);
            txtLatitud.setVisibility(View.VISIBLE);
        } else {
            txtLongitud.setVisibility(View.GONE);
            txtLatitud.setVisibility(View.GONE);
        }

        bSensing = false;

        registerReceiver(receiver, new IntentFilter(ServiceDatos.NOTIFICATION));

        Intent intent = new Intent(this, ServiceDatos.class);
        // add infos for the service which file to download and where to store
        intent.putExtra("Periodo", iPeriodo);
        intent.putExtra("NumDevices", iNumDevices);
        for (int i = 0; i < iNumDevices; i++)
            intent.putExtra("Address" + i, sAddresses[i]);
        intent.putExtra("Humedad", bHumedad);
        intent.putExtra("Barometro", bBarometro);
        intent.putExtra("Luz", bLuz);
        intent.putExtra("Temperatura", bTemperatura);
        intent.putExtra("Acelerometro", bAcelerometro);
        intent.putExtra("Giroscopo", bGiroscopo);
        intent.putExtra("Magnetometro", bMagnetometro);
        intent.putExtra("Location", bLocation);
        intent.putExtra("SendServer", bSendServer);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent);
        else
            startService(intent);



        handler = new Handler();
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if (bSensing) {
                    adaptadorDatos.notifyDataSetChanged();
                }
                handler.postDelayed(this, lTiempoMuestraDatos);
            }
        });
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            bSensing = true;
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                int iSensor = bundle.getInt("Sensor");
                int iDevice = bundle.getInt("Device");
                String sCadena = bundle.getString("Cadena");
                switch (iSensor) {
                    case ServiceDatos.GIROSCOPO:
                        listaDatos.setMovimiento1(iDevice, sCadena);
                        break;
                    case ServiceDatos.ACELEROMETRO:
                        listaDatos.setMovimiento2(iDevice, sCadena);
                        break;
                    case ServiceDatos.MAGNETOMETRO:
                        listaDatos.setMovimiento3(iDevice, sCadena);
                        break;
                    case ServiceDatos.HUMEDAD:
                        listaDatos.setHumedad(iDevice, sCadena);
                        break;
                    case ServiceDatos.LUZ:
                        listaDatos.setLuz(iDevice, sCadena);
                        break;
                    case ServiceDatos.BAROMETRO:
                        listaDatos.setBarometro(iDevice, sCadena);
                        break;
                    case ServiceDatos.TEMPERATURA:
                        listaDatos.setTemperatura(iDevice, sCadena);
                        break;
                    case ServiceDatos.LOCALIZACION_LAT:
                        txtLatitud.setText("Lat: " + sCadena);
                        break;
                    case ServiceDatos.LOCALIZACION_LONG:
                        txtLongitud.setText("Long: " + sCadena);
                        break;
                }
            }
        }
    };



    public  void btnPararClick(View v) {
        stopService(new Intent(this, ServiceDatos.class));
        finish();
    }
}
