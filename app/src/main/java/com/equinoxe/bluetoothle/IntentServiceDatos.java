package com.equinoxe.bluetoothle;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;

public class IntentServiceDatos extends IntentService {
    final static long lTiempoGPS = 10 * 1000;                   // Tiempo de toma de muestras de GPS (en ms)
    final static long lTiempoGrabacionDatos = 120 * 1000;       // Tiempo de grabación de las estadísticas (en ms)
    final static int SENSOR_MOV_DATA_LEN = 19;
    final static int SENSOR_MOV_SEC_POS = SENSOR_MOV_DATA_LEN - 1;

    final static int GIROSCOPO    = 0;
    final static int ACELEROMETRO = 1;
    final static int MAGNETOMETRO = 2;
    final static int HUMEDAD      = 3;
    final static int LUZ          = 4;
    final static int BAROMETRO    = 5;
    final static int TEMPERATURA  = 6;
    final static int LOCALIZACION_LAT  = 7;
    final static int LOCALIZACION_LONG = 8;

    public static final String NOTIFICATION = "com.equinoxe.bluetoothle.android.service.receiver";

    SimpleDateFormat sdf;

    FileOutputStream fOut;
    BatteryInfoBT batInfo;

    private int iNumDevices;
    private int iPeriodo;
    private long lTiempoRefrescoDatos;
    private long lMensajesParaEnvio;
    private long lMensajesPorSegundo;

    byte barometro[] = new byte[4];
    long valorBarometro, valorTemperatura;
    float fValorBarometro, fValorTemperatura;

    byte luz[] = new byte[2];
    float fValorLuz;

    long valorGiroX, valorGiroY, valorGiroZ;
    float fValorGiroX, fValorGiroY, fValorGiroZ;
    long valorAcelX, valorAcelY, valorAcelZ;
    float fValorAcelX, fValorAcelY, fValorAcelZ;
    long valorMagX, valorMagY, valorMagZ;
    float fValorMagX, fValorMagY, fValorMagZ;

    byte humedad[] = new byte[4];
    long valorHumedad;
    float fValorHumedad;

    boolean bLocation;
    LocationManager locManager;
    Location mejorLocaliz;
    boolean bGPSEnabled;
    boolean bNetworkEnabled;

    boolean bNetConnected;
    EnvioDatosSocket envioAsync;

    BluetoothGatt btGatt[];
    private boolean bSensores[][];
    private boolean bActivacion[][];
    private boolean bConfigPeriodo[][];

    long lDatosRecibidos[];
    long lDatosPerdidos[];
    byte iSecuencia[];
    boolean bPrimerDato[];

    byte movimiento[][];
    boolean bSensing;
    DecimalFormat df;

    private boolean bHumedad, bBarometro, bLuz, bTemperatura, bAcelerometro, bGiroscopo, bMagnetometro;
    private String sAddresses[] = new String[8];
    boolean bSendServer;


    public IntentServiceDatos() {
        super("ServiceDatos");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        iNumDevices = intent.getIntExtra("NumDevices",1);
        iPeriodo = intent.getIntExtra("Periodo",20);
        lTiempoRefrescoDatos = intent.getLongExtra("Refresco", 120000);
        lMensajesParaEnvio = lTiempoRefrescoDatos / iPeriodo;
        lMensajesPorSegundo = 1000 / iPeriodo;
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

        bSensores = new boolean[iNumDevices][4];
        bActivacion = new boolean[iNumDevices][4];
        bConfigPeriodo = new boolean[iNumDevices][4];

        lDatosRecibidos = new long[iNumDevices];
        lDatosPerdidos = new long[iNumDevices];
        iSecuencia = new byte[iNumDevices];
        bPrimerDato = new boolean[iNumDevices];

        for (int i = 0; i < iNumDevices; i++) {
            bSensores[i][0] = bActivacion[i][0] = bConfigPeriodo[i][0] = bAcelerometro || bGiroscopo || bMagnetometro;
            bSensores[i][1] = bActivacion[i][1] = bConfigPeriodo[i][1] = bHumedad;
            bSensores[i][2] = bActivacion[i][2] = bConfigPeriodo[i][2] = bBarometro || bTemperatura;
            bSensores[i][3] = bActivacion[i][3] = bConfigPeriodo[i][3] = bLuz;

            lDatosRecibidos[i] = 0;
            lDatosPerdidos[i] = 0;
            iSecuencia[i] = 0;
            bPrimerDato[i] = true;
        }

        movimiento = new byte[iNumDevices][SENSOR_MOV_DATA_LEN];

        btGatt = new BluetoothGatt[iNumDevices];

        df = new DecimalFormat("###.##");
        sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

        String currentDateandTime = sdf.format(new Date());
        try {
            File file;
            int iNumFichero = 0;
            String sFichero;
            do {
                sFichero = Environment.getExternalStorageDirectory() + "/" + android.os.Build.MODEL + "_" + iNumDevices + "_" + iPeriodo + "_" + iNumFichero + ".txt";
                file = new File(sFichero);
                iNumFichero++;
            } while (file.exists());

            fOut = new FileOutputStream(sFichero, false);
            String sCadena = android.os.Build.MODEL + " " + iNumDevices + " " + iPeriodo + " " + bGPSEnabled + " " + bSendServer + " " + currentDateandTime + "\n";
            fOut.write(sCadena.getBytes());
            fOut.flush();
        } catch (Exception e) {
            Toast.makeText(this, getResources().getString(R.string.ERROR_FICHERO), Toast.LENGTH_LONG).show();
        }

        batInfo = new BatteryInfoBT();

        TimerTask timerTask = new TimerTask() {
            public void run() {
                grabarMedidas();
            }
        };

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, lTiempoGrabacionDatos, lTiempoGrabacionDatos);

        realizarConexiones();

        while(true);
    }

    private void realizarConexiones() {
        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        BluetoothDevice device;
        for (int i = 0; i < iNumDevices; i++) {
            device = adapter.getRemoteDevice(sAddresses[i]);
            btGatt[i] = device.connectGatt(this, true, mBluetoothGattCallback);
        }

        bNetConnected = false;
        if (bSendServer) {
            try {
                ConnectivityManager check = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = check.getActiveNetworkInfo();
                if (!info.isConnected()) {
                    Toast.makeText(this, getResources().getString(R.string.ERROR_RED), Toast.LENGTH_LONG).show();
                } else bNetConnected = true;
            } catch (Exception e) {
                Toast.makeText(this, getResources().getString(R.string.ERROR_RED), Toast.LENGTH_LONG).show();
            }

            if (bNetConnected) {
                SharedPreferences pref = getApplicationContext().getSharedPreferences("Settings", MODE_PRIVATE);
                String sServer = pref.getString("server", "127.0.0.1");
                int iPuerto = pref.getInt("puerto", 8000);

                envioAsync = new EnvioDatosSocket(sServer, iPuerto, SENSOR_MOV_DATA_LEN + 1);
                envioAsync.start();
            }
        }

        if (bLocation) {
            locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            ultimaLocalizacion();
            activarProveedores();
        }
    }

    private void ultimaLocalizacion() {
        try {
            if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                actualizaMejorLocaliz(locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
            }
            if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                actualizaMejorLocaliz(locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER));
            }
        } catch (SecurityException e) {
            Toast.makeText(this, getResources().getString(R.string.LOCATION_FAILED), Toast.LENGTH_SHORT).show();
        }
    }

    private void activarProveedores() {
        try {
            bGPSEnabled = bNetworkEnabled = false;
            if (locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                bGPSEnabled = true;
                locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, lTiempoGPS, 0, locListener, Looper.getMainLooper());
            }
            if (locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                bNetworkEnabled = true;
                locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, lTiempoGPS, 0, locListener, Looper.getMainLooper());
            }
            if (!bGPSEnabled && !bNetworkEnabled)
                Toast.makeText(this, getResources().getString(R.string.LOCATION_DISABLED), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Toast.makeText(this, getResources().getString(R.string.LOCATION_FAILED), Toast.LENGTH_SHORT).show();
        }
    }

    private void actualizaMejorLocaliz(Location localiz) {
        if (localiz != null && (mejorLocaliz == null ||
                localiz.getAccuracy() < 2 * mejorLocaliz.getAccuracy() ||
                localiz.getTime() - mejorLocaliz.getTime() > 20000)) {
            mejorLocaliz = localiz;
        }
    }

    public LocationListener locListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            actualizaMejorLocaliz(location);
            publishSensorValues(LOCALIZACION_LAT, 0, Double.toString(mejorLocaliz.getLatitude()));
            publishSensorValues(LOCALIZACION_LONG, 0, Double.toString(mejorLocaliz.getLongitude()));
        }

        public void onProviderDisabled(String provider) {
            activarProveedores();
        }

        public void onProviderEnabled(String provider) {
            activarProveedores();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            activarProveedores();
        }
    };

    private void getBatteryInfo() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        batInfo.setBatteryLevel(batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1));
        batInfo.setVoltaje(batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1));
        batInfo.setTemperature(batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1));

        BatteryManager mBatteryManager = (BatteryManager)this.getSystemService(Context.BATTERY_SERVICE);
        batInfo.setCurrentAverage(mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE));
        batInfo.setCurrentNow(mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
    }

    public void grabarMedidas() {
        getBatteryInfo();

        long lDatosRecibidosTotal = 0;
        long lDatosPerdidosTotal = 0;
        for (int i = 0; i < iNumDevices; i++) {
            lDatosRecibidosTotal += lDatosRecibidos[i];
            lDatosPerdidosTotal += lDatosPerdidos[i];
        }

        try {
            String sCadena = sdf.format(new Date()) + ":" +
                    batInfo.getBatteryLevel() + ":" +
                    batInfo.getVoltaje() + ":" +
                    batInfo.getTemperature() + ":" +
                    batInfo.getCurrentAverage() + ":" +
                    batInfo.getCurrentNow() + " - " +
                    lDatosRecibidosTotal + " - " +
                    lDatosPerdidosTotal + "\n";
            fOut.write(sCadena.getBytes());
            fOut.flush();
        } catch (Exception e) {
            Log.e("Fichero de resultados", e.getMessage(), e);
        }
    }

    private void cerrarConexiones() {
        bSensing = false;
        grabarMedidas();

        //envioAsync.cancel(true);

        for (int i = 0; i < iNumDevices; i++) {
            btGatt[i].disconnect();
            btGatt[i].close();
        }

        if (bLocation)
            locManager.removeUpdates(locListener);

        try {
            //fLog.close();
            fOut.close();
            envioAsync.finishSend();
        } catch (Exception e) { }
    }

    private int findGattIndex(BluetoothGatt btGatt) {
        int iIndex = 0;
        String sAddress = btGatt.getDevice().getAddress();

        while (sAddresses[iIndex].compareTo(sAddress) != 0)
            iIndex++;

        return iIndex;
    }

    private final BluetoothGattCallback mBluetoothGattCallback;
    {
        mBluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status == BluetoothGatt.GATT_SUCCESS)
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        btGatt[findGattIndex(gatt)].discoverServices();
                    }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Se obtiene el primer sensor a activar
                    int iDevice = findGattIndex(gatt);
                    int firstSensor = findFirstSensor(iDevice);
                    // Se actualiza para saber que ya se ha activado
                    bSensores[iDevice][firstSensor] = false;

                    habilitarServicio(gatt, firstSensor);
                }
            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Se obtiene el primer sensor a activar
                    int iDevice = findGattIndex(gatt);
                    int firstSensor = findFirstSensor(iDevice);

                    if (firstSensor < 4) {
                        // Se actualiza para saber que ya se ha activado
                        bSensores[iDevice][firstSensor] = false;

                        habilitarServicio(gatt, firstSensor);
                    } else {
                        int firstActivar = firstSensorActivar(iDevice);

                        bActivacion[iDevice][firstActivar] = false;
                        activarServicio(gatt, firstActivar);
                    }
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    int iDevice = findGattIndex(gatt);

                    int firstActivar = firstSensorActivar(iDevice);
                    if (firstActivar < 4) {
                        bActivacion[iDevice][firstActivar] = false;

                        activarServicio(gatt, firstActivar);
                    } else {
                        int firstPeriodo = firstSensorPeriodo(iDevice);
                        if (firstPeriodo < 4) {
                            bConfigPeriodo[iDevice][firstPeriodo] = false;
                            configPeriodo(gatt, firstPeriodo);
                        } else {
                            bSensing = true;
                            //adaptadorDatos.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                int iDevice = findGattIndex(gatt);

                /*if (characteristic.getUuid().compareTo(UUIDs.UUID_BAR_DATA) == 0) {
                    barometro = characteristic.getValue();
                    procesaBarometro(barometro, findGattIndex(gatt));
                } else if (characteristic.getUuid().compareTo(UUIDs.UUID_OPT_DATA) == 0) {
                    luz = characteristic.getValue();
                    procesaLuz(luz, findGattIndex(gatt));
                } else if (characteristic.getUuid().compareTo(UUIDs.UUID_MOV_DATA) == 0) {*/
                movimiento[iDevice] = characteristic.getValue();

                    /*String sCadena = String.format("%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%02X:%2X\n",
                                                    movimiento[18],movimiento[17],movimiento[16],movimiento[15],movimiento[14],movimiento[13],
                                                    movimiento[12],movimiento[11],movimiento[10],movimiento[9], movimiento[8], movimiento[7],
                                                    movimiento[6], movimiento[5], movimiento[4], movimiento[3], movimiento[2], movimiento[1],
                                                    movimiento[0]);
                    try {
                        fLog.write(sCadena.getBytes());
                    } catch (Exception e) {}*/

                //envoltorioDatosMovimiento.setDatos(movimiento);
                if (bSendServer)
                    envioAsync.setData((byte)iDevice, movimiento[iDevice]);

                lDatosRecibidos[iDevice]++;

                // Se le suma lMensajesPorSegundo para que envío los datos 1 segundo antes de que se refresque el panel de datos
                procesaMovimiento(movimiento[iDevice], iDevice, ((lDatosRecibidos[iDevice] + lMensajesPorSegundo) % lMensajesParaEnvio) == 0);

                if (bPrimerDato[iDevice]) {
                    bPrimerDato[iDevice] = false;
                    iSecuencia[iDevice] = movimiento[iDevice][SENSOR_MOV_SEC_POS];
                } else {
                    iSecuencia[iDevice]++;
                    if (iSecuencia[iDevice] != movimiento[iDevice][SENSOR_MOV_SEC_POS]) {
                        if (iSecuencia[iDevice] > movimiento[iDevice][SENSOR_MOV_SEC_POS])
                            lDatosPerdidos[iDevice] += (256 - iSecuencia[iDevice] + movimiento[iDevice][SENSOR_MOV_SEC_POS]);
                        else
                            lDatosPerdidos[iDevice] += movimiento[iDevice][SENSOR_MOV_SEC_POS] - iSecuencia[iDevice];

                        iSecuencia[iDevice] = movimiento[iDevice][SENSOR_MOV_SEC_POS];
                    }
                }

                /*} else if (characteristic.getUuid().compareTo(UUIDs.UUID_HUM_DATA) == 0) {
                    humedad = characteristic.getValue();
                    procesaHumedad(humedad, findGattIndex(gatt));
                } else {
                    Log.e("BluetoothLE", "Dato deslocalizado");
                }*/

            }
        };
    }

    private void configPeriodo(BluetoothGatt btGatt, int firstPeriodo) {
        BluetoothGattCharacteristic characteristic;

        characteristic = btGatt.getService(getServerUUID(firstPeriodo)).getCharacteristic(getPeriodoUUID(firstPeriodo));
        characteristic.setValue(iPeriodo * 11 / 110, FORMAT_SINT8, 0);
        btGatt.writeCharacteristic(characteristic);
    }

    private void activarServicio(BluetoothGatt btGatt, int firstActivar) {
        BluetoothGattCharacteristic characteristic;

        characteristic = btGatt.getService(getServerUUID(firstActivar)).getCharacteristic(getConfigUUID(firstActivar));
        byte lowByte = 0;
        switch (firstActivar) {
            case 0:
                if (bMagnetometro) lowByte |= 0b01000000;
                if (bAcelerometro) lowByte |= 0b00111000;
                if (bGiroscopo)    lowByte |= 0b00000111;
                characteristic.setValue(new byte[]{lowByte,1});
                break;
            case 1:
            case 2:
            case 3:
                characteristic.setValue(new byte[]{1});
                break;
        }

        btGatt.writeCharacteristic(characteristic);
    }


    private void habilitarServicio(BluetoothGatt gatt, int firstSensor) {
        BluetoothGattService service;
        BluetoothGattDescriptor descriptor;
        BluetoothGattCharacteristic characteristic;
        int iPosGatt = findGattIndex(gatt);

        service = btGatt[iPosGatt].getService(getServerUUID(firstSensor));
        characteristic = service.getCharacteristic(getDataUUID(firstSensor));
        gatt.setCharacteristicNotification(characteristic, true);

        descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        btGatt[iPosGatt].writeDescriptor(descriptor);
    }

    private UUID getConfigUUID(int iSensor) {
        UUID UUIDConfig = UUIDs.UUID_MOV_CONF;

        switch (iSensor) {
            case 0:
                UUIDConfig = UUIDs.UUID_MOV_CONF;
                break;
            case 1:
                UUIDConfig = UUIDs.UUID_HUM_CONF;
                break;
            case 2:
                UUIDConfig = UUIDs.UUID_BAR_CONF;
                break;
            case 3:
                UUIDConfig = UUIDs.UUID_OPT_CONF;
                break;
        }

        return UUIDConfig;
    }

    private UUID getDataUUID(int iSensor) {
        UUID UUIDData = UUIDs.UUID_MOV_DATA;

        switch (iSensor) {
            case 0:
                UUIDData = UUIDs.UUID_MOV_DATA;
                break;
            case 1:
                UUIDData = UUIDs.UUID_HUM_DATA;
                break;
            case 2:
                UUIDData = UUIDs.UUID_BAR_DATA;
                break;
            case 3:
                UUIDData = UUIDs.UUID_OPT_DATA;
                break;
        }

        return UUIDData;
    }

    private UUID getServerUUID(int iSensor) {
        UUID UUIDServer = UUIDs.UUID_MOV_SERV;

        switch (iSensor) {
            case 0:
                UUIDServer = UUIDs.UUID_MOV_SERV;
                break;
            case 1:
                UUIDServer = UUIDs.UUID_HUM_SERV;
                break;
            case 2:
                UUIDServer = UUIDs.UUID_BAR_SERV;
                break;
            case 3:
                UUIDServer = UUIDs.UUID_OPT_SERV;
                break;
        }

        return UUIDServer;
    }

    private UUID getPeriodoUUID(int iSensor) {
        UUID UUIDServer = UUIDs.UUID_MOV_PERI;

        switch (iSensor) {
            case 0:
                UUIDServer = UUIDs.UUID_MOV_PERI;
                break;
            case 1:
                UUIDServer = UUIDs.UUID_HUM_PERI;
                break;
            case 2:
                UUIDServer = UUIDs.UUID_BAR_PERI;
                break;
            case 3:
                UUIDServer = UUIDs.UUID_OPT_PERI;
                break;
        }

        return UUIDServer;
    }


    private int findFirstSensor(int iDevice) {
        int first = 0;

        while (first < 4 && !bSensores[iDevice][first])
            first++;

        return first;
    }

    private int firstSensorPeriodo(int iDevice) {
        int first = 0;

        while (first < 4 && !bConfigPeriodo[iDevice][first])
            first++;

        return first;
    }

    private int firstSensorActivar(int iDevice) {
        int first = 0;

        while (first < 4 && !bActivacion[iDevice][first])
            first++;

        return first;
    }

    private void procesaMovimiento(byte movimiento[], int iDevice, boolean bEnviarDatos) {
        long aux;
        String sCadena;

        // Giróscopo
        valorGiroX = movimiento[1];
        //valorGiroX &= 0x00000000000000FF;
        valorGiroX = valorGiroX << 8;

        aux = movimiento[0];
        //aux &= 0x00000000000000FF;
        valorGiroX |= aux;
        fValorGiroX = (float) (((float) valorGiroX / 65536.0) * 500.0);

        valorGiroY = movimiento[3];
        //valorGiroY &= 0x00000000000000FF;
        valorGiroY = valorGiroY << 8;

        aux = movimiento[2];
        //aux &= 0x00000000000000FF;
        valorGiroY |= aux;
        fValorGiroY = (float) (((float) valorGiroY / 65536.0) * 500.0);

        valorGiroZ = movimiento[5];
        //valorGiroZ &= 0x00000000000000FF;
        valorGiroZ = valorGiroZ << 8;

        aux = movimiento[4];
        //aux &= 0x00000000000000FF;
        valorGiroZ |= aux;
        fValorGiroZ = (float) (((float) valorGiroZ / 65536.0) * 500.0);

        sCadena = "G -> X: " + df.format(fValorGiroX) + " " + getString(R.string.GyroscopeUnit) + " ";
        sCadena += "   Y: " + df.format(fValorGiroY) + " " + getString(R.string.GyroscopeUnit) + " ";
        sCadena += "   Z: " + df.format(fValorGiroZ) + " " + getString(R.string.GyroscopeUnit);

        if (bEnviarDatos)
            publishSensorValues(GIROSCOPO, iDevice,sCadena);


        // Acelerómetro
        valorAcelX = movimiento[7];
        //valorAcelX &= 0x00000000000000FF;
        valorAcelX = valorAcelX << 8;

        aux = movimiento[6];
        //aux &= 0x00000000000000FF;
        valorAcelX |= aux;
        fValorAcelX = (float) valorAcelX / (32768 / 4);

        valorAcelY = movimiento[9];
        //valorAcelY &= 0x00000000000000FF;
        valorAcelY = valorAcelY << 8;

        aux = movimiento[8];
        //aux &= 0x00000000000000FF;
        valorAcelY |= aux;
        fValorAcelY = (float) valorAcelY / (32768 / 4);

        valorAcelZ = movimiento[11];
        //valorAcelZ &= 0x00000000000000FF;
        valorAcelZ = valorAcelZ << 8;

        aux = movimiento[10];
        //aux &= 0x00000000000000FF;
        valorAcelZ |= aux;
        fValorAcelZ = (float) valorAcelZ / (32768 / 4);

        sCadena = "A -> X: " + df.format(fValorAcelX) + " " + getString(R.string.AccelerometerUnit) + " ";
        sCadena += "   Y: " + df.format(fValorAcelY) + " " + getString(R.string.AccelerometerUnit) + " ";
        sCadena += "   Z: " + df.format(fValorAcelZ) + " " + getString(R.string.AccelerometerUnit);

        if (bEnviarDatos)
            publishSensorValues(ACELEROMETRO, iDevice,sCadena);


        // Magnetómetro
        valorMagX = movimiento[13];
        //valorMagX &= 0x00000000000000FF;
        valorMagX = valorMagX << 8;

        aux = movimiento[12];
        //aux &= 0x00000000000000FF;
        valorMagX |= aux;
        fValorMagX = (float) valorMagX;

        valorMagY = movimiento[15];
        //valorMagY &= 0x00000000000000FF;
        valorMagY = valorMagY << 8;

        aux = movimiento[14];
        //aux &= 0x00000000000000FF;
        valorMagY |= aux;
        fValorMagY = (float) valorMagY;

        valorMagZ = movimiento[17];
        //valorMagZ &= 0x00000000000000FF;
        valorMagZ = valorMagZ << 8;

        aux = movimiento[16];
        //aux &= 0x00000000000000FF;
        valorMagZ |= aux;
        fValorMagZ = (float) valorMagZ;

        sCadena =  "M -> X: " + Float.toString(fValorMagX) + " " + getString(R.string.MagnetometerUnit) + " ";
        sCadena += "   Y: " + Float.toString(fValorMagY) + " " + getString(R.string.MagnetometerUnit) + " ";
        sCadena += "   Z: " + Float.toString(fValorMagZ) + " " + getString(R.string.MagnetometerUnit);
        if (bEnviarDatos)
            publishSensorValues(MAGNETOMETRO, iDevice,sCadena);
    }

    private void procesaHumedad(byte humedad[], int iDevice) {
        long aux;

        valorHumedad = humedad[3];
        valorHumedad &= 0x00000000000000FF;
        valorHumedad = valorHumedad << 8;

        aux = humedad[2];
        aux &= 0x00000000000000FF;
        valorHumedad |= aux;

        valorHumedad *= 100;
        fValorHumedad = valorHumedad / 65536;

        String sCadena = Float.toString(fValorHumedad) + " " + getString(R.string.HumidityUnit);
        //listaDatos.setHumedad(iDevice, sCadena);
        publishSensorValues(HUMEDAD, iDevice,sCadena);
    }

    private void procesaLuz(byte luz[], int iDevice) {
        long auxM, auxE;

        auxM = luz[1];
        auxM = auxM << 8;
        auxM |= luz[0];
        auxM &= 0x0000000000000FFF;

        auxE = (auxM & 0x000000000000F000) >> 12;

        auxE = (auxE == 0)?1:2<<(auxE-1);

        fValorLuz = (float)auxM * (((float) auxE) / 100);

        String sCadena = df.format(fValorLuz) + " " + getString(R.string.LightUnit);
        //listaDatos.setLuz(iDevice, sCadena);
        publishSensorValues(LUZ, iDevice,sCadena);
    }

    private void procesaBarometro(byte barometro[], int iDevice) {
        long aux;

        // Barómetro
        valorBarometro = barometro[5];
        valorBarometro &= 0x00000000000000FF;
        valorBarometro = valorBarometro << 8;

        aux = barometro[4];
        aux &= 0x00000000000000FF;
        valorBarometro |= aux;
        valorBarometro = valorBarometro << 8;

        aux = barometro[3];
        aux &= 0x00000000000000FF;
        valorBarometro |= aux;
        fValorBarometro = valorBarometro / 100;

        String sCadena = df.format(fValorBarometro) + " " + getString(R.string.BarometerUnit);
        // listaDatos.setBarometro(iDevice, sCadena);
        publishSensorValues(BAROMETRO, iDevice,sCadena);


        // Temperatura
        valorTemperatura = barometro[2];
        valorTemperatura &= 0x00000000000000FF;
        valorTemperatura = valorTemperatura << 8;

        aux = barometro[1];
        aux &= 0x00000000000000FF;
        valorTemperatura |= aux;
        valorTemperatura = valorTemperatura << 8;

        aux = barometro[0];
        aux &= 0x00000000000000FF;
        valorTemperatura |= aux;
        fValorTemperatura = valorTemperatura / 100;

        sCadena = df.format(fValorTemperatura) + " " + getString(R.string.TemperatureUnit);
        //listaDatos.setTemperatura(iDevice, sCadena);
        publishSensorValues(TEMPERATURA, iDevice,sCadena);
    }


    @Override
    public void onDestroy() {
        cerrarConexiones();
        super.onDestroy();
    }

    private void publishSensorValues(int iSensor, int iDevice, String sCadena) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra("Sensor", iSensor);
        intent.putExtra("Device", iDevice);
        intent.putExtra("Cadena", sCadena);
        sendBroadcast(intent);
    }
}