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

    boolean bHumedad;
    boolean bBarometro;
    boolean bLuz;
    boolean bTemperatura;
    boolean bAcelerometro;
    boolean bGiroscopo;
    boolean bMagnetometro;

    boolean bLocation;
    boolean bSendServer;

    int iNumDevices;
    int iPeriodo;

    String sAddresses[];

    boolean bServicioParado;
    Intent intentChkServicio = null;

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

        sAddresses = new String[8];
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

        bServicioParado = true;

        crearServicio();

        registerReceiver(receiver, new IntentFilter(ServiceDatos.NOTIFICATION));


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


    private void crearServicio() {
        intentChkServicio = new Intent(this, checkServiceDatos.class);

        intentChkServicio.putExtra("Periodo", iPeriodo);
        intentChkServicio.putExtra("Refresco", lTiempoRefrescoDatos);
        intentChkServicio.putExtra("NumDevices", iNumDevices);
        for (int i = 0; i < iNumDevices; i++)
            intentChkServicio.putExtra("Address" + i, sAddresses[i]);
        intentChkServicio.putExtra("Humedad", bHumedad);
        intentChkServicio.putExtra("Barometro", bBarometro);
        intentChkServicio.putExtra("Luz", bLuz);
        intentChkServicio.putExtra("Temperatura", bTemperatura);
        intentChkServicio.putExtra("Acelerometro", bAcelerometro);
        intentChkServicio.putExtra("Giroscopo", bGiroscopo);
        intentChkServicio.putExtra("Magnetometro", bMagnetometro);
        intentChkServicio.putExtra("Location", bLocation);
        intentChkServicio.putExtra("SendServer", bSendServer);

        startService(intentChkServicio);
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

                /*if (!bServicioParado && iSensor == 0 && iDevice == 0 && sCadena.length() == 0) {
                    bServicioParado = true;
                    unregisterReceiver(this);
                }*/
                if (iDevice != ServiceDatos.ERROR)
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
        stopService(intentChkServicio);
        unregisterReceiver(receiver);
        finish();
    }
}
