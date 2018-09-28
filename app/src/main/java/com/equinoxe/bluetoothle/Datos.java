package com.equinoxe.bluetoothle;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;


public class Datos extends AppCompatActivity {
    final static long lTiempoRefrescoDatos = 120 * 1000;  // Tiempo de muestra de datos

    BluetoothDataList listaDatos;

    private MiAdaptadorDatos adaptadorDatos;
    private TextView txtLongitud;
    private  TextView txtLatitud;

    Handler handler;
    boolean bSensing;
    DecimalFormat df;

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
        int iNumDevices = extras.getInt("NumDevices");

        String sAddresses[] = new String[8];
        for (int i = 0; i < iNumDevices; i++)
            sAddresses[i] = extras.getString("Address" + i);
        int iPeriodo = extras.getInt("Periodo");

        boolean bHumedad = extras.getBoolean("Humedad");
        boolean bBarometro = extras.getBoolean("Barometro");
        boolean bLuz = extras.getBoolean("Luz");
        boolean bTemperatura = extras.getBoolean("Temperatura");
        boolean bAcelerometro = extras.getBoolean("Acelerometro");
        boolean bGiroscopo = extras.getBoolean("Giroscopo");
        boolean bMagnetometro = extras.getBoolean("Magnetometro");

        boolean bLocation = extras.getBoolean("Location");
        boolean bSendServer = extras.getBoolean("SendServer");

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

        //registerReceiver(receiver, new IntentFilter(IntentServiceDatos.NOTIFICATION));
        registerReceiver(receiver, new IntentFilter(ServiceDatos.NOTIFICATION));

        //Intent intent = new Intent(this, IntentServiceDatos.class);
        Intent intent = new Intent(this, ServiceDatos.class);
        // add infos for the service which file to download and where to store
        intent.putExtra("Periodo", iPeriodo);
        intent.putExtra("Refresco", lTiempoRefrescoDatos);
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

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent);
        else*/
            startService(intent);


        handler = new Handler();
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if (bSensing) {
                    adaptadorDatos.notifyDataSetChanged();
                }
                handler.postDelayed(this, lTiempoRefrescoDatos);
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
                    //case IntentServiceDatos.GIROSCOPO:
                    case ServiceDatos.GIROSCOPO:
                        listaDatos.setMovimiento1(iDevice, sCadena);
                        break;
                    //case IntentServiceDatos.ACELEROMETRO:
                    case ServiceDatos.ACELEROMETRO:
                        listaDatos.setMovimiento2(iDevice, sCadena);
                        break;
                    //case IntentServiceDatos.MAGNETOMETRO:
                    case ServiceDatos.MAGNETOMETRO:
                        listaDatos.setMovimiento3(iDevice, sCadena);
                        break;
                    //case IntentServiceDatos.HUMEDAD:
                    case ServiceDatos.HUMEDAD:
                        listaDatos.setHumedad(iDevice, sCadena);
                        break;
                    //case IntentServiceDatos.LUZ:
                    case ServiceDatos.LUZ:
                        listaDatos.setLuz(iDevice, sCadena);
                        break;
                    //case IntentServiceDatos.BAROMETRO:
                    case ServiceDatos.BAROMETRO:
                        listaDatos.setBarometro(iDevice, sCadena);
                        break;
                    //case IntentServiceDatos.TEMPERATURA:
                    case ServiceDatos.TEMPERATURA:
                        listaDatos.setTemperatura(iDevice, sCadena);
                        break;
                    //case IntentServiceDatos.LOCALIZACION_LAT:
                    case ServiceDatos.LOCALIZACION_LAT:
                        txtLatitud.setText("Lat: " + sCadena);
                        break;
                    //case IntentServiceDatos.LOCALIZACION_LONG:
                    case ServiceDatos.LOCALIZACION_LONG:
                        txtLongitud.setText("Long: " + sCadena);
                        break;
                }
            }
        }
    };



    public  void btnPararClick(View v) {
        stopService(new Intent(this, IntentServiceDatos.class));
        finish();
    }
}
