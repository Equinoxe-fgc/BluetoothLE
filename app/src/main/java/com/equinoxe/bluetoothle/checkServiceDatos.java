package com.equinoxe.bluetoothle;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;

import java.util.Timer;
import java.util.TimerTask;

public class checkServiceDatos extends Service {
    Intent intentServicio = null;

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
    long lTiempoRefrescoDatos;

    String sAddresses[] = new String[8];

    //private Looper mServiceLooper;
    //private ServiceHandler mServiceHandler;

    public checkServiceDatos() {
        super();
    }

    /*// Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // Completar
        }
    }*/


    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("checkServiceDatos", HandlerThread.MIN_PRIORITY);
        thread.start();

        /*mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);*/
    }

    private void crearServicio() {
        intentServicio = new Intent(this, ServiceDatos.class);

        intentServicio.putExtra("Periodo", iPeriodo);
        intentServicio.putExtra("NumDevices", iNumDevices);
        intentServicio.putExtra("Refresco", lTiempoRefrescoDatos);
        for (int i = 0; i < iNumDevices; i++)
            intentServicio.putExtra("Address" + i, sAddresses[i]);
        intentServicio.putExtra("Humedad", bHumedad);
        intentServicio.putExtra("Barometro", bBarometro);
        intentServicio.putExtra("Luz", bLuz);
        intentServicio.putExtra("Temperatura", bTemperatura);
        intentServicio.putExtra("Acelerometro", bAcelerometro);
        intentServicio.putExtra("Giroscopo", bGiroscopo);
        intentServicio.putExtra("Magnetometro", bMagnetometro);
        intentServicio.putExtra("Location", bLocation);
        intentServicio.putExtra("SendServer", bSendServer);

        startService(intentServicio);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        iNumDevices = intent.getIntExtra("NumDevices",1);
        iPeriodo = intent.getIntExtra("Periodo",20);
        lTiempoRefrescoDatos = intent.getLongExtra("Refresco", 120000);
        for (int i = 0; i < iNumDevices; i++)
            sAddresses[i] = intent.getStringExtra("Address" + i);
        bHumedad = intent.getBooleanExtra("Humedad", false);
        bAcelerometro = intent.getBooleanExtra("Acelerometro", true);
        bGiroscopo = intent.getBooleanExtra("Giroscopo", true);
        bMagnetometro = intent.getBooleanExtra("Magnetometro", true);
        bBarometro = intent.getBooleanExtra("Barometro", false);
        bTemperatura = intent.getBooleanExtra("Temperatura", false);
        bLuz = intent.getBooleanExtra("Luz", false);

        bLocation = intent.getBooleanExtra("Location", false);
        bSendServer = intent.getBooleanExtra("SendServer", false);

        crearServicio();

        registerReceiver(receiver, new IntentFilter(ServiceDatos.NOTIFICATION));

        /*TimerTask timerTask = new TimerTask() {
            public void run() {
                if (bServicioParado) {
                    //registerReceiver(receiver, new IntentFilter(IntentServiceDatos.NOTIFICATION));
                    registerReceiver(receiver, new IntentFilter(ServiceDatos.NOTIFICATION));
                    crearServicio();
                }
            }
        };

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, lTiempoComprobacionDesconexion);*/

        return START_NOT_STICKY;
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();

            if (bundle != null) {
                int iDevice = bundle.getInt("Device");

                if (iDevice == ServiceDatos.ERROR) {
                    stopService(intentServicio);
                    startService(intentServicio);
                }

            }
        }
    };

    @Override
    public void onDestroy() {
        stopService(intentServicio);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}