package com.equinoxe.bluetoothle;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;


public class EnvioDatosSocket extends Thread {
    private OutputStream outputStream = null;
    private Socket socket = null;
    private boolean bConnectionOK = true;
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

            /*for (int i = 0; i < data.length; i++)
                this.data[i] = data[i];*/
            bDataToSend = true;
        }
    }

    public boolean isConnectionOK() {
        return bConnectionOK;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(sServer, iPuerto);
            outputStream = socket.getOutputStream();
        } catch (Exception e) {
            bConnectionOK = false;
        }

        if (bConnectionOK)
            while (true) {
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
    }

    @Override
    protected void finalize() throws Throwable {
        if (outputStream != null) outputStream.close();
        if (socket != null) socket.close();
        super.finalize();
    }
}
