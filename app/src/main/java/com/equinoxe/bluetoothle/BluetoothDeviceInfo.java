package com.equinoxe.bluetoothle;

import android.bluetooth.BluetoothDevice;


public class BluetoothDeviceInfo {
    private boolean bSelected;
    private String sName;
    private String sAddress;
    private BluetoothDevice device;

    public BluetoothDeviceInfo(boolean bSelected, String sName, String sAddress, BluetoothDevice device) {
        this.bSelected = bSelected;
        this.sName = sName;
        this.sAddress = sAddress;
        this.device = device;
    }

    public boolean isSelected() {
        return bSelected;
    }

    public String getDescription() {
        return sName;
    }

    public String getAddress() {
        return sAddress;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setSelected(boolean bSelected) {
        this.bSelected = bSelected;
    }

    public void setName(String sDescription) {
        this.sName = sDescription;
    }

    public void setAddress(String sAddress) {
        this.sAddress = sAddress;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }
}
