package com.equinoxe.bluetoothle;

import android.bluetooth.BluetoothGatt;

public class ContenedorBluetooth {
    private static BluetoothGatt btGatt = null;

    protected ContenedorBluetooth() {

    }

    public static BluetoothGatt getInstance() {
        return btGatt;
    }

    public static void setInstance(BluetoothGatt newBtGatt) {
        btGatt = newBtGatt;
    }
}
