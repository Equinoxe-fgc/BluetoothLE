package com.equinoxe.bluetoothle;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;


public class EnvioDatosSocket extends Thread {
    private OutputStream outputStream = null;
    private Socket socket = null;
    private boolean bConnectionOK = true;
    private boolean bPrimeraVez = true;
    private byte data[];
    String sServer;
    int iPuerto;

    public EnvioDatosSocket(String sServer, int iPuerto, int iTamano) {
        this.sServer = sServer;
        this.iPuerto = iPuerto;
        data = new byte[iTamano];
    }

    public void setData(byte data[]) {
        for (int i = 0; i < data.length; i++)
            this.data[i] = data[i];
    }

    public boolean isConnectionOK() {
        return bConnectionOK;
    }

    @Override
    public void run() {
        if (bPrimeraVez) {
            bPrimeraVez = false;
            try {
                socket = new Socket(sServer, iPuerto);
                outputStream = socket.getOutputStream();
            } catch (Exception e) {
                bConnectionOK = false;
            }
        }

        try {
            if (bConnectionOK) {
                outputStream.write(data);
                outputStream.flush();
            }
        } catch (IOException e) {   }
    }

    @Override
    protected void finalize() throws Throwable {
        if (outputStream != null) outputStream.close();
        if (socket != null) socket.close();
        super.finalize();
    }
}
