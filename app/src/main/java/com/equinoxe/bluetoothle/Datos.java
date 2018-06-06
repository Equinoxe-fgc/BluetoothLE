package com.equinoxe.bluetoothle;

import android.Manifest;
import android.app.Activity;
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
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;


import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;


public class Datos extends AppCompatActivity {
    final static long lTiempoMedidas = 8*60*60*1000;  // 8 horas * 60 minutos * 60 segundos * 1000 milisegundos - Tiempo de muestra en milisegundos

    BluetoothGatt btGatt[] = new BluetoothGatt[8];
    BluetoothDataList listaDatos;
    //private final Handler handler = new Handler();

    private Button btnStopDatos;
    private MiAdaptadorDatos adaptadorDatos;

    private boolean bHumedad, bBarometro, bLuz, bTemperatura, bAcelerometro, bGiroscopo, bMagnetometro;
    private boolean bSensores[][];
    private boolean bActivacion[][];
    private int iPeriodo;
    private int iNumDevices;
    private String sAddresses[] = new String[8];

    byte barometro[] = new byte[4];
    long valorBarometro, valorTemperatura;
    float fValorBarometro, fValorTemperatura;

    byte luz[] = new byte[2];
    float fValorLuz;

    byte movimiento[] = new byte[18];
    long valorGiroX, valorGiroY, valorGiroZ;
    float fValorGiroX, fValorGiroY, fValorGiroZ;
    long valorAcelX, valorAcelY, valorAcelZ;
    float fValorAcelX, fValorAcelY, fValorAcelZ;
    long valorMagX, valorMagY, valorMagZ;
    float fValorMagX, fValorMagY, fValorMagZ;

    byte humedad[] = new byte[4];
    long valorHumedad;
    float fValorHumedad;

    Context context;
    Handler handler;
    long iContadorSegundos;
    int iBatteryLevelStart;
    Date dateStart;

    DecimalFormat df;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datos);

        RecyclerView recyclerViewDatos;
        RecyclerView.LayoutManager layoutManager;

        context = this;
        iContadorSegundos = 2000;   // Poner un número de segundos muy grande para que no se pare antes de que arranque
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        df = new DecimalFormat("###.##");

        Bundle extras = getIntent().getExtras();
        iNumDevices = extras.getInt("NumDevices");

        bSensores = new boolean[iNumDevices][4];
        bActivacion = new boolean[iNumDevices][4];

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

        for (int i = 0; i < iNumDevices; i++) {
            bSensores[iNumDevices][0] = bActivacion[iNumDevices][0] = bAcelerometro || bGiroscopo || bMagnetometro;
            bSensores[iNumDevices][1] = bActivacion[iNumDevices][1] = bHumedad;
            bSensores[iNumDevices][2] = bActivacion[iNumDevices][2] = bBarometro || bTemperatura;
            bSensores[iNumDevices][3] = bActivacion[iNumDevices][3] = bLuz;
        }

        recyclerViewDatos = findViewById(R.id.recycler_viewDatos);
        btnStopDatos = findViewById(R.id.btnStopDatos);

        listaDatos = new BluetoothDataList(iNumDevices);

        adaptadorDatos = new MiAdaptadorDatos(this, listaDatos);
        layoutManager = new LinearLayoutManager(this);

        recyclerViewDatos.setAdapter(adaptadorDatos);
        recyclerViewDatos.setLayoutManager(layoutManager);

        iBatteryLevelStart = getBatteryLevel();
        dateStart = new Date();

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();

        BluetoothDevice device;
        for (int i = 0; i < iNumDevices; i++) {
            device = adapter.getRemoteDevice(sAddresses[i]);
            btGatt[i] = device.connectGatt(this, true, mBluetoothGattCallback);
        }

        handler = new Handler();
        runOnUiThread(new Runnable(){
            @Override
            public void run(){
                iContadorSegundos--;

                if (iContadorSegundos <= 0)
                    btnPararClick(btnStopDatos);
                else {
                    adaptadorDatos.notifyDataSetChanged();
                    handler.postDelayed(this, iPeriodo);
                }
            }
        });
    }


    private int getBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        return batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
    }


    @Override
    public void onBackPressed() {
        for (int i = 0; i < iNumDevices; i++) {
            btGatt[i].disconnect();
            btGatt[i].close();
        }

        super.onBackPressed();
    }

    public  void btnPararClick(View v) {
        int iBatteryLevel = getBatteryLevel();

        verifyStoragePermissions(this);

        //Toast.makeText(this, "Nivel de batería:" + iBatteryLevel + " %", Toast.LENGTH_LONG).show();
        String sCadena;
        String sFichero = Environment.getExternalStorageDirectory() + "/simulBT_BAT.txt";
        try {
            FileOutputStream f = new FileOutputStream(sFichero, true);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String currentDateandTime = sdf.format(new Date());
            String startDateandTime = sdf.format(dateStart);

            sCadena = startDateandTime + ":" + iBatteryLevelStart + " -> " + currentDateandTime + ":" + iBatteryLevel + "\n";
            f.write(sCadena.getBytes());
            f.close();
        } catch (Exception e) {
            Log.e("Fichero de resultados", e.getMessage(), e);
        }

        for (int i = 0; i < iNumDevices; i++) {
            btGatt[i].disconnect();
            btGatt[i].close();
        }

        finish();
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (Build.VERSION.SDK_INT >= 23) {
            int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, 1);
            }
        }
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

                        activarServicio(btGatt[findGattIndex(gatt)], firstActivar);
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

                        iContadorSegundos = lTiempoMedidas / iPeriodo;
                    }
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                if (characteristic.getUuid().compareTo(UUIDs.UUID_BAR_DATA) == 0) {
                    barometro = characteristic.getValue();
                    procesaBarometro(barometro, findGattIndex(gatt));
                } else if (characteristic.getUuid().compareTo(UUIDs.UUID_OPT_DATA) == 0) {
                    luz = characteristic.getValue();
                    procesaLuz(luz, findGattIndex(gatt));
                } else if (characteristic.getUuid().compareTo(UUIDs.UUID_MOV_DATA) == 0) {
                    movimiento = characteristic.getValue();
                    procesaMovimiento(movimiento, findGattIndex(gatt));
                } else if (characteristic.getUuid().compareTo(UUIDs.UUID_HUM_DATA) == 0) {
                    humedad = characteristic.getValue();
                    procesaHumedad(humedad, findGattIndex(gatt));
                }
            }
        };
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

        characteristic = btGatt.getService(getServerUUID(firstActivar)).getCharacteristic(getConfigUUID(firstActivar));
        characteristic.setValue(iPeriodo * 11 / 110, FORMAT_SINT8, 0);
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


    private int findFirstSensor(int iDevice) {
        int first = 0;

        while (first < 4 && !bSensores[iDevice][first])
            first++;

        return first;
    }

    private int firstSensorActivar(int iDevice) {
        int first = 0;

        while (first < 4 && !bActivacion[iDevice][first])
            first++;

        return first;
    }

    private void procesaMovimiento(byte movimiento[], int iDevice) {
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
        listaDatos.setMovimiento1(iDevice, sCadena);


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
        listaDatos.setMovimiento2(iDevice, sCadena);


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
        listaDatos.setMovimiento3(iDevice, sCadena);
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
        listaDatos.setHumedad(iDevice, sCadena);
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
        listaDatos.setLuz(iDevice, sCadena);
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
        listaDatos.setBarometro(iDevice, sCadena);


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
        listaDatos.setTemperatura(iDevice, sCadena);
    }
}
