package com.equinoxe.bluetoothle;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

public class MiAdaptadorDatos extends RecyclerView.Adapter<MiAdaptadorDatos.ViewHolderDatos> {
    private LayoutInflater inflador;
    private BluetoothDataList lista;
    private MiAdaptadorDatos.ViewHolderDatos holderDatos;
    View v;

    public MiAdaptadorDatos(Context context, BluetoothDataList lista) {
        this.lista = lista;
        inflador = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setBarometro(int i, String value) {
        lista.setBarometro(i, value);
    }

    @NonNull
    @Override
    public MiAdaptadorDatos.ViewHolderDatos onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        v = inflador.inflate(R.layout.elemento_lista_datos, parent, false);
        holderDatos = new MiAdaptadorDatos.ViewHolderDatos(v);
        return holderDatos;
    }

    @Override
    public void onBindViewHolder(@NonNull final MiAdaptadorDatos.ViewHolderDatos holder, final int position) {
        BluetoothData info = lista.getBluetoothData(position);

        holder.textViewHumedad.setText(info.getHumedad());
        holder.textViewBarometro.setText(info.getBarometro());
        holder.textViewLuz.setText(info.getLuz());
        holder.textViewMovimiento1.setText(info.getMovimiento1());
        holder.textViewMovimiento2.setText(info.getMovimiento3());
        holder.textViewMovimiento3.setText(info.getMovimiento1());
    }

    @Override
    public int getItemCount() {
        return lista.getSize();
    }

    public class ViewHolderDatos extends RecyclerView.ViewHolder {
        public TextView textViewHumedad;
        public TextView textViewBarometro;
        public TextView textViewLuz;
        public TextView textViewMovimiento1;
        public TextView textViewMovimiento2;
        public TextView textViewMovimiento3;

        public ViewHolderDatos(View itemView) {
            super(itemView);
            textViewHumedad = itemView.findViewById(R.id.textViewHumedad);
            textViewBarometro= itemView.findViewById(R.id.textViewBarometro);
            textViewLuz= itemView.findViewById(R.id.textViewLuz);
            textViewMovimiento1 = itemView.findViewById(R.id.textViewMovimiento1);
            textViewMovimiento2 = itemView.findViewById(R.id.textViewMovimiento2);
            textViewMovimiento3 = itemView.findViewById(R.id.textViewMovimiento3);
        }
    }
}
