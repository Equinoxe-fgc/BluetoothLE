package com.equinoxe.bluetoothle;

import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EnvioDatosSocket extends Thread {
    private final static byte ID_GPS = 127;
    private OutputStream outputStream = null;
    FileOutputStream fOut = null;
    private Socket socket = null;
    SimpleDateFormat sdf;
    private byte data[];
    private byte dataGPS[];
    byte bytesLat[];
    byte bytesLong[];
    private boolean bDataToSend = false;
    private boolean bGPSToSend = false;
    private String sServer;
    private int iPuerto;
    private int iTamano;
    String sCadena;


    public EnvioDatosSocket(String sServer, int iPuerto, int iTamano) {
        this.sServer = sServer;
        this.iPuerto = iPuerto;
        this.iTamano = iTamano;
        data = new byte[iTamano];
        dataGPS = new byte[20];

        bytesLat = new byte[8];
        bytesLong = new byte[8];
    }

    public void setData(byte iDevice, byte data[]) {
        synchronized (this) {
            for (int i = 1; i < iTamano; i++) {
                this.data[i] = data[i - 1];
            }
            this.data[0] = iDevice;

            /*this.data = Arrays.copyOf(data, iTamano);
            this.data[iTamano - 1] = iDevice;*/
            bDataToSend = true;
        }
    }

    public void setGPS(double dLat, double dLong) {
        synchronized (this) {
            ByteBuffer.wrap(bytesLat).putDouble(dLat);
            ByteBuffer.wrap(bytesLong).putDouble(dLong);

            dataGPS[0] = ID_GPS;
            System.arraycopy(bytesLat, 0, dataGPS, 1, 8);
            System.arraycopy(bytesLong, 0, dataGPS, 9, 8);

            bGPSToSend = true;
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
                if (bDataToSend || bGPSToSend) {
                    synchronized (this) {
                        try {
                            if (bDataToSend) {
                                outputStream.write(data);
                                bDataToSend = false;
                            } else {
                                outputStream.write(dataGPS);
                                bGPSToSend = false;
                            }
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
            sCadena = sdf.format(new Date()) + " Socket cerrado\n";
        } catch (Exception e) {
            sCadena = sdf.format(new Date()) + " While Exception " + e.getMessage() + "\n";
            Log.d("EnvioDatosSocket.java", "Error al crear socket o stream");
        }

        try {
            fOut.write(sCadena.getBytes());
            fOut.close();
        } catch (Exception e) {}
    }

    @Override
    protected void finalize() throws Throwable {
        fOut.close();
        finishSend();
        super.finalize();
    }
}
