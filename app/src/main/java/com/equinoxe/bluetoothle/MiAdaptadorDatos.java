package com.equinoxe.bluetoothle;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MiAdaptadorDatos extends RecyclerView.Adapter<MiAdaptadorDatos.ViewHolderDatos> {
    private LayoutInflater inflador;
    private BluetoothDataList lista;

    MiAdaptadorDatos(Context context, BluetoothDataList lista) {
        this.lista = lista;
        inflador = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public MiAdaptadorDatos.ViewHolderDatos onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflador.inflate(R.layout.elemento_lista_datos, parent, false);
        return new MiAdaptadorDatos.ViewHolderDatos(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final MiAdaptadorDatos.ViewHolderDatos holder, final int position) {
        BluetoothData info = lista.getBluetoothData(position);

        /*holder.textViewTemperatura.setText(info.getTemperatura());
        holder.textViewHumedad.setText(info.getHumedad());
        holder.textViewBarometro.setText(info.getBarometro());
        holder.textViewLuz.setText(info.getLuz());*/
        holder.textViewAddress.setText(info.getAddress());
        holder.textViewMovimiento1.setText(info.getMovimiento1());
        holder.textViewMovimiento2.setText(info.getMovimiento2());
        holder.textViewMovimiento3.setText(info.getMovimiento3());
        holder.textViewPaquetes.setText(info.getPaquetes());
    }

    @Override
    public int getItemCount() {
        return lista.getSize();
    }

    class ViewHolderDatos extends RecyclerView.ViewHolder {
        TextView textViewAddress;
        /*public TextView textViewTemperatura;
        public TextView textViewHumedad;
        public TextView textViewBarometro;
        public TextView textViewLuz;*/
        TextView textViewMovimiento1;
        TextView textViewMovimiento2;
        TextView textViewMovimiento3;

        TextView textViewPaquetes;

        ViewHolderDatos(View itemView) {
            super(itemView);
            textViewAddress = itemView.findViewById(R.id.textViewAddress);
            /*textViewTemperatura = itemView.findViewById(R.id.textViewTemperatura);
            textViewHumedad = itemView.findViewById(R.id.textViewHumedad);
            textViewBarometro= itemView.findViewById(R.id.textViewBarometro);
            textViewLuz= itemView.findViewById(R.id.textViewLuz);*/
            textViewMovimiento1 = itemView.findViewById(R.id.textViewMovimiento1);
            textViewMovimiento2 = itemView.findViewById(R.id.textViewMovimiento2);
            textViewMovimiento3 = itemView.findViewById(R.id.textViewMovimiento3);

            textViewPaquetes = itemView.findViewById(R.id.textPaquetes);
        }
    }
}
