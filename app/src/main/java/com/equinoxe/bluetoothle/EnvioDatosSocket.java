package com.equinoxe.bluetoothle;

import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;


public class EnvioDatosSocket extends Thread {
    private OutputStream outputStream = null;
    FileOutputStream fOut = null;
    private Socket socket = null;
    SimpleDateFormat sdf;
    private byte data[];
    private boolean bDataToSend = false;
    private String sServer;
    private int iPuerto;
    private int iTamano;
    String sCadena;

    public EnvioDatosSocket(String sServer, int iPuerto, int iTamano) {
        this.sServer = sServer;
        this.iPuerto = iPuerto;
        this.iTamano = iTamano;
        data = new byte[iTamano];
    }

    public void setData(byte iDevice, byte data[]) {
        synchronized (this) {
            this.data = Arrays.copyOf(data, iTamano);
            this.data[iTamano - 1] = iDevice;
            bDataToSend = true;
        }
    }

    private void finishSend() {
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
            sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String currentDateandTime = sdf.format(new Date()) + "\n";

            socket = new Socket(sServer, iPuerto);
            outputStream = socket.getOutputStream();

            fOut = new FileOutputStream(Environment.getExternalStorageDirectory() + "/LOG_Envio.txt", true);
            fOut.write(currentDateandTime.getBytes());

            while (!socket.isClosed()) {
                if (bDataToSend) {
                    synchronized (this) {
                        try {
                            outputStream.write(data);
                            bDataToSend = false;
                            //outputStream.flush();
                        } catch (Exception e) {
                            sCadena = sdf.format(new Date()) + " While Exception " + e.getMessage() + "\n";
                            fOut.write(sCadena.getBytes());
                            Log.d("EnvioDatosSocket.java", "Error al enviar");

                            // Se cierran las conexiones
                            finishSend();

                            sCadena = sdf.format(new Date()) + " - Reconexión\n";
                            fOut.write(sCadena.getBytes());
                            // Se vuelve a crear la conexión
                            socket = new Socket(sServer, iPuerto);
                            outputStream = socket.getOutputStream();
                        }
                    }
                }
            }
            fOut.close();
        } catch (Exception e) {
            sCadena = sdf.format(new Date()) + " While Exception " + e.getMessage() + "\n";
            try {
                fOut.write(sCadena.getBytes());
            } catch (Exception el) {}
            Log.d("EnvioDatosSocket.java", "Error al crear socket o stream");
        }
    }

    @Override
    protected void finalize() throws Throwable {
        fOut.close();
        finishSend();
        super.finalize();
    }
}
