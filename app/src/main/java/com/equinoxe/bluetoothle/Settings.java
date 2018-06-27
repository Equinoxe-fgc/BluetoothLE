package com.equinoxe.bluetoothle;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class Settings extends AppCompatActivity {
    private TextView txtServer;
    private TextView txtPuerto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        txtServer = findViewById(R.id.txtServidor);
        txtPuerto = findViewById(R.id.txtPuerto);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("Settings", MODE_PRIVATE);
        txtServer.setText(pref.getString("server", "127.0.0.1"));
        String sCadena = "" + pref.getInt("puerto", 8080);
        txtPuerto.setText(sCadena);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_grabar:
                switch (comprobarSettingsOK()) {
                    case 0:
                        SharedPreferences pref = getApplicationContext().getSharedPreferences("Settings", MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("server", txtServer.getText().toString());
                        editor.putInt("puerto", Integer.parseInt(txtPuerto.getText().toString()));
                        editor.apply();

                        Toast.makeText(this, getResources().getText(R.string.Options_saved), Toast.LENGTH_SHORT).show();

                        finish();
                        return true;
                    case 1:
                        Toast.makeText(this, getResources().getText(R.string.Incorrect_IP), Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        Toast.makeText(this, getResources().getText(R.string.Incorrect_Port), Toast.LENGTH_SHORT).show();
                        break;
                }
        }
        return super.onOptionsItemSelected(item);
    }

    private int comprobarSettingsOK() {
        String sServer = txtServer.getText().toString();

        try {
            int iPuerto = Integer.parseInt(txtPuerto.getText().toString());
            if (iPuerto < 0 || iPuerto > 65535)
                return 2;
        } catch (Exception e) {
            return 2;
        }

        return 0;
    }
}
