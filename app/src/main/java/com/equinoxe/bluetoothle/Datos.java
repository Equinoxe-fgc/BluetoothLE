package com.equinoxe.bluetoothle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;


public class Datos extends AppCompatActivity {
    BluetoothGatt btGatt;
    List<BluetoothGattService> listServices;
    BluetoothDataList listaDatos;
    private final Handler handler = new Handler();

    private Button btnStopDatos;
    private RecyclerView recyclerViewDatos;
    private MiAdaptadorDatos adaptadorDatos;
    private RecyclerView.LayoutManager layoutManager;

    private boolean bHumedad, bBarometro, bLuz, bTemperatura, bAcelerometro, bGiroscopo, bMagnetometro;
    private int iPeriodo;

    byte barometro[] = new byte[6];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datos);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bundle extras = getIntent().getExtras();
        iPeriodo = extras.getInt("Periodo");
        bHumedad = extras.getBoolean("Humedad");
        bBarometro = extras.getBoolean("Barometro");
        bLuz = extras.getBoolean("Luz");
        bTemperatura = extras.getBoolean("Temperatura");
        bAcelerometro = extras.getBoolean("Acelerometro");
        bGiroscopo = extras.getBoolean("Giroscopo");
        bMagnetometro = extras.getBoolean("Magnetometro");

        recyclerViewDatos = findViewById(R.id.recycler_viewDatos);
        btnStopDatos = findViewById(R.id.btnStopDatos);

        listaDatos = new BluetoothDataList(1);

        adaptadorDatos = new MiAdaptadorDatos(this, listaDatos);
        layoutManager = new LinearLayoutManager(this);

        recyclerViewDatos.setAdapter(adaptadorDatos);
        recyclerViewDatos.setLayoutManager(layoutManager);

        String sAddress1 = extras.getString("Address1");

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager.getAdapter();
        BluetoothDevice device = adapter.getRemoteDevice(sAddress1);
        btGatt = device.connectGatt(this, true, mBluetoothGattCallback);

        handler.removeCallbacks(sendUpdatesToUI);
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {

            handler.postDelayed(this, iPeriodo);
        }
    };

    @Override
    public void onBackPressed() {
        btGatt.disconnect();
        btGatt.close();

        super.onBackPressed();
    }

    public  void btnPararClick(View v) {
        btGatt.disconnect();
        btGatt.close();

        finish();
    }

    private final BluetoothGattCallback mBluetoothGattCallback;
    {
        mBluetoothGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (status == BluetoothGatt.GATT_SUCCESS)
                    if (newState == BluetoothGatt.STATE_CONNECTED) {
                        btGatt.discoverServices();
                    }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);

                BluetoothGattService service;
                BluetoothGattCharacteristic characteristic;
                BluetoothGattDescriptor descriptor;

                if (bBarometro) {
                    service = btGatt.getService(UUIDs.UUID_BAR_SERV);
                    characteristic = service.getCharacteristic(UUIDs.UUID_BAR_DATA);
                    gatt.setCharacteristicNotification(characteristic, true);

                    descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    btGatt.writeDescriptor(descriptor);
                }


            }

            @Override
            public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorWrite(gatt, descriptor, status);

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (bBarometro) {
                        BluetoothGattCharacteristic characteristic = btGatt.getService(UUIDs.UUID_BAR_SERV).getCharacteristic(UUIDs.UUID_BAR_CONF);
                        characteristic.setValue(new byte[]{1});
                        boolean bOK = btGatt.writeCharacteristic(characteristic);

                        characteristic = btGatt.getService(UUIDs.UUID_BAR_SERV).getCharacteristic(UUIDs.UUID_BAR_PERI);
                        characteristic.setValue(iPeriodo * 11 / 110, FORMAT_SINT8, 0);
                        bOK = btGatt.writeCharacteristic(characteristic);
                    }
                }
            }

            /*@Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);

                /*characteristic = btGatt.getService(UUIDs.UUID_BAR_SERV).getCharacteristic(UUIDs.UUID_BAR_CONF);
                characteristic.setValue(new byte[]{1});
                btGatt.writeCharacteristic(characteristic);

                //btGatt.readCharacteristic(characteristic);
                barometro = characteristic.getValue();
            }*/

            /*@Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);

                byte[] b;

                if (status == BluetoothGatt.GATT_SUCCESS) {
                    b = characteristic.getValue();
                    if (b[0] != 0)
                        return;
                }
            }*/

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);

                barometro = characteristic.getValue();
            }
        };
    }
}
