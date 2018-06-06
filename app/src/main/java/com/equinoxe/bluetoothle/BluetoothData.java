package com.equinoxe.bluetoothle;

public class BluetoothData {
    private String sHumedad;
    private String sBarometro;
    private String sLuz;
    private String sTemperatura;
    private String sMovimiento1;
    private String sMovimiento2;
    private String sMovimiento3;

    public BluetoothData() {
        this.sHumedad = "";
        this.sBarometro = "";
        this.sLuz = "";
        this.sTemperatura = "";
        this.sMovimiento1 = "";
        this.sMovimiento2 = "";
        this.sMovimiento3 = "";
    }

    public String getHumedad() {
        return sHumedad;
    }

    public String getBarometro() {
        return sBarometro;
    }

    public String getLuz() {
        return sLuz;
    }

    public String getTemperatura() {
        return sTemperatura;
    }

    public String getMovimiento1() {
        return sMovimiento1;
    }

    public String getMovimiento2() {
        return sMovimiento2;
    }

    public String getMovimiento3() {
        return sMovimiento3;
    }

    public void setHumedad(String sHumedad) {
        this.sHumedad = sHumedad;
    }

    public void setBarometro(String sBarometro) {
        this.sBarometro = sBarometro;
    }

    public void setLuz(String sLuz) {
        this.sLuz = sLuz;
    }

    public void setTemperatura(String sTemperatura) {
        this.sTemperatura = sTemperatura;
    }

    public void setMovimiento1(String sMovimiento1) {
        this.sMovimiento1 = sMovimiento1;
    }

    public void setMovimiento2(String sMovimiento2) {
        this.sMovimiento2 = sMovimiento2;
    }

    public void setMovimiento3(String sMovimiento3) {
        this.sMovimiento3 = sMovimiento3;
    }
}
