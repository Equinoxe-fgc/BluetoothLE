package com.equinoxe.bluetoothle;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;


public class Datos extends AppCompatActivity {
    //final static long lTiempoRefrescoDatos = 120 * 1000;  // Tiempo de muestra de datos
    final static long lTiempoRefrescoDatos = 10 * 1000;  // Tiempo de muestra de datos

    final static int iMIN_READ_TIME = 15;
    final static int iMIN_RANDOM_TIME = 5;
    final static int iMAX_RANDOM_TIME = 30;

    BluetoothDataList listaDatos;

    private MiAdaptadorDatos adaptadorDatos;
    private TextView txtLongitud;
    private  TextView txtLatitud;
    private TextView txtMensajes;

    Handler handlerDatos;
    Handler handlerWeb;
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
    boolean bWebNavigation;

    boolean bTime;
    long lTime;

    int iNumDevices;
    int iPeriodo;

    String sAddresses[];

    boolean bServicioParado;
    Intent intentChkServicio = null;

    Timer timerTiempo;


    protected WebView webView;
    protected Vector links;
    protected Vector linksInicial;
    protected String sLinks = "";
    protected boolean bTerminado;
    Random r;
    int random;



    @Override
    protected void onResume() {
        super.onResume();

        //Toast.makeText(this,"Resume Datos", Toast.LENGTH_LONG);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datos);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        RecyclerView recyclerViewDatos;
        RecyclerView.LayoutManager layoutManager;

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
        bWebNavigation = extras.getBoolean("WebNavigation");

        bTime = extras.getBoolean("bTime");
        lTime = extras.getLong("Time");

        if (bSendServer || bWebNavigation) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            /*WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.screenBrightness = 1f / 255f;
            window.setAttributes(layoutParams);*/
        }


        recyclerViewDatos = findViewById(R.id.recycler_viewDatos);
        txtLatitud = findViewById(R.id.textViewLatitud);
        txtLongitud = findViewById(R.id.textViewLongitud);
        txtMensajes = findViewById(R.id.textViewMensajes);

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

        if (bWebNavigation) {
            bTemperatura = false;
            linksInicial = new Vector();
            linksInicial.addElement("http://www.wikipedia.com");
            linksInicial.addElement("https://tapeline.info/v2/index.php?");
            linksInicial.addElement("https://www.c64-wiki.com/");
            linksInicial.addElement("https://www.worldofspectrum.org/");
            linksInicial.addElement("http://www.cpcwiki.eu/index.php/Main_Page");
            linksInicial.addElement("https://www.nvidia.com/es-es/");

            links = new Vector();
            r = new Random();

            webView = findViewById(R.id.webView);

            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(new MyJavaScriptInterface(this), "HtmlViewer");

            random = r.nextInt(linksInicial.size());
            String sURL = (String) linksInicial.elementAt(random);
            webView.loadUrl(sURL);
            bTerminado = false;

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    webView.loadUrl("javascript:window.HtmlViewer.showHTML" +
                            "('<html>'+document.getElementsByTagName('html')[0].innerHTML+'</html>');");
                }
            });}


        bSensing = false;

        bServicioParado = true;

        crearServicio();

        registerReceiver(receiver, new IntentFilter(ServiceDatos.NOTIFICATION));

        if (bTime) {
            final TimerTask timerTaskTiempo = new TimerTask() {
                public void run() {
                    btnPararClick(null);
                }
            };

            timerTiempo = new Timer();
            timerTiempo.schedule(timerTaskTiempo, lTime);
        }


        handlerDatos = new Handler();
        handlerWeb = new Handler();
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                if (bSensing) {
                    adaptadorDatos.notifyDataSetChanged();
                }
                handlerDatos.postDelayed(this, lTiempoRefrescoDatos);

                if (bWebNavigation) {
                    //if (bTerminado) {
                        bTerminado = false;
                        String sURL;

                        if (links.isEmpty()) {
                            random = r.nextInt(linksInicial.size());
                            sURL = (String) linksInicial.elementAt(random);
                        } else {
                            random = r.nextInt(100);
                            if (random < 20) {
                                random = r.nextInt(linksInicial.size());
                                sURL = (String) linksInicial.elementAt(random);
                            } else {
                                random = r.nextInt(links.size());
                                sURL = (String) links.elementAt(random);
                            }
                        }
                        webView.loadUrl(sURL);

                        random = r.nextInt(iMAX_RANDOM_TIME - iMIN_RANDOM_TIME) + iMIN_RANDOM_TIME;
                        //handlerWeb.postDelayed(this, (iMIN_READ_TIME + random)*1000);
                    //}
                }
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
        //intentChkServicio.putExtra("bTime", bTime);
        //intentChkServicio.putExtra("Time", lTime);

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
                if (iDevice == ServiceDatos.MSG) {
                    txtMensajes.append(sCadena.substring(16));
                }
                else {
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
        }
    };



    public  void btnPararClick(View v) {
        stopService(intentChkServicio);
        unregisterReceiver(receiver);

        finish();
    }


    class MyJavaScriptInterface {
        private Context ctx;

        MyJavaScriptInterface(Context ctx) {
            this.ctx = ctx;
        }

        @android.webkit.JavascriptInterface
        public void showHTML(String html) {
            int index1, index2;
            String sURL;
            sLinks = "";
            links.clear();

            int indexA = html.indexOf("<a");
            while (indexA >= 0){
                index1 = html.indexOf("href=\"", indexA);
                index2 = html.indexOf('\"', index1 + 7);

                String sSub = html.substring(index1+6, index1+10);
                if (sSub.compareToIgnoreCase("http") == 0 || sSub.compareToIgnoreCase("https") == 0) {
                    sURL = html.substring(index1 + 6, index2);
                    sLinks += "\n" + sURL + "\n";
                    links.addElement(sURL);
                }

                indexA = html.indexOf("<a", index2 + 1);
            }

            bTerminado = true;
            //new AlertDialog.Builder(ctx).setTitle("HTML").setMessage(sLinks)
            //        .setPositiveButton(android.R.string.ok, null).setCancelable(false).create().show();
        }

    }
}
