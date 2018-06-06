package com.equinoxe.bluetoothle;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDataList {
    private List<BluetoothData> vectorData;

    public BluetoothDataList(int iSize) {
        this.vectorData = new ArrayList<>();

        for (int i = 0; i < iSize; i++) {
            vectorData.add(new BluetoothData());
        }
    }

    public BluetoothData getBluetoothData(int iPos) {
        return vectorData.get(iPos);
    }

    public int getSize() {
        return vectorData.size();
    }

    public void setHumedad(int iPos, String sHumedad) {
        vectorData.get(iPos).setHumedad(sHumedad);
    }

    public void setBarometro(int iPos, String sBarometro) {
        vectorData.get(iPos).setBarometro(sBarometro);
    }

    public void setLuz(int iPos, String sLuz) {
        vectorData.get(iPos).setLuz(sLuz);
    }

    public void setTemperatura(int iPos, String sTemperatura) {
        vectorData.get(iPos).setTemperatura(sTemperatura);
    }

    public void setMovimiento1(int iPos, String sMovimiento1) {
        vectorData.get(iPos).setMovimiento1(sMovimiento1);
    }

    public void setMovimiento2(int iPos, String sMovimiento2) {
        vectorData.get(iPos).setMovimiento2(sMovimiento2);
    }

    public void setMovimiento3(int iPos, String sMovimiento3) {
        vectorData.get(iPos).setMovimiento3(sMovimiento3);
    }
}
