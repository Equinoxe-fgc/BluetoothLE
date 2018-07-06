package com.equinoxe.bluetoothle;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;


public class EnvioDatosSocket extends Thread {
    private OutputStream outputStream = null;
    private Socket socket = null;
    private byte data[];
    private boolean bDataToSend = false;
    private String sServer;
    private int iPuerto;

    public EnvioDatosSocket(String sServer, int iPuerto, int iTamano) {
        this.sServer = sServer;
        this.iPuerto = iPuerto;
        data = new byte[iTamano];
    }

    public void setData(byte data[]) {
        synchronized (this) {
            this.data = Arrays.copyOf(data, 18);
            bDataToSend = true;
        }
    }

    public void finishSend() {
        try {
            outputStream.close();
            socket.close();
        } catch (Exception e) {
            if (!socket.isClosed())
                try {
                    socket.close();
                } catch (Exception ee) {}
        }
    }

    @Override
    public void run() {
        try {
            socket = new Socket(sServer, iPuerto);
            outputStream = socket.getOutputStream();

            while (!socket.isClosed()) {
                if (bDataToSend) {
                    synchronized (this) {
                        try {
                            outputStream.write(data);
                            bDataToSend = false;
                            outputStream.flush();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d("prueba", "prueba");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (outputStream != null) outputStream.close();
        if (socket != null) socket.close();
        super.finalize();
    }
}
