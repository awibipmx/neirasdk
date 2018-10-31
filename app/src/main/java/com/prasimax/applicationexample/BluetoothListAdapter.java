package com.prasimax.applicationexample;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class BluetoothListAdapter extends RecyclerView.Adapter {
    private static final int VIEW_TYPE_BLUETOOTH_DEVICE = 1;

    public BluetoothListAdapter(List<BluetoothDevice> deviceList, BluetoothDeviceListDialog.BluetoothDeviceListDialogListener listener) {
        this.deviceList = deviceList;
        this.listener = listener;
    }

    private List<BluetoothDevice> deviceList;
    private BluetoothDeviceListDialog.BluetoothDeviceListDialogListener listener;

    @Override
    public int getItemViewType(int position) {
        return VIEW_TYPE_BLUETOOTH_DEVICE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_BLUETOOTH_DEVICE) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_bluetooth_device, parent, false);
            return new BluetoothDeviceHolder(view);
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        BluetoothDevice device = deviceList.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_BLUETOOTH_DEVICE:
                ((BluetoothDeviceHolder) holder).bind(device);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    private class BluetoothDeviceHolder extends RecyclerView.ViewHolder{
        TextView bluetoothName, bluetoothAddress;
        BluetoothDevice device;

        public BluetoothDeviceHolder(View itemView) {
            super(itemView);

            bluetoothName = itemView.findViewById(R.id.bluetooth_name);
            bluetoothAddress = itemView.findViewById(R.id.bluetooth_address);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onAcceptedDevice(device);
                }
            });
        }

        void bind(BluetoothDevice device){
            this.device = device;
            bluetoothName.setText(device.getName());
            bluetoothAddress.setText(device.getAddress());
        }
    }
}
