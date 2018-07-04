package com.equinoxe.bluetoothle;

public class EnvoltorioDatos {
    private byte datos[];
    private int iTamano;

    public EnvoltorioDatos(int iTamano) {
        datos = new byte[iTamano];
        this.iTamano = iTamano;
    }

    public int getTamano() {
        return iTamano;
    }

    public void setDatos(byte datos[]) {
        for (int i = 0; i < iTamano; i++) {
            this.datos[i] = datos[i];
        }
    }

    public byte[] getDatos() {
        return datos;
    }
}
