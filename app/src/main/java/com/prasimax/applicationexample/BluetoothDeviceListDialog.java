package com.prasimax.applicationexample;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BluetoothDeviceListDialog extends DialogFragment {
    private RecyclerView mBluetoothRecycler;
    private BluetoothListAdapter mBluetoothListAdapter;
    private List<BluetoothDevice> bluetoothList = new ArrayList<BluetoothDevice>();

    public interface BluetoothDeviceListDialogListener {
        public void onDialogNegativeClick(DialogFragment dialog);
        public void onAcceptedDevice(BluetoothDevice device);
    }

    BluetoothDeviceListDialogListener mListener;

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        try {
            mListener = (BluetoothDeviceListDialogListener) activity;
        }catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement BluetoothDeviceListDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.bluetooth_list_dialog, null);

        builder.setTitle("Paired Bluetooth List");

        mBluetoothRecycler = (RecyclerView) view.findViewById(R.id.recyclerview_bluetooth);
        if(mBluetoothRecycler == null) System.out.println("Oii salah lagi");
        mBluetoothListAdapter = new BluetoothListAdapter(bluetoothList, mListener );
        LinearLayoutManager m1LayoutManager = new LinearLayoutManager(getActivity().getApplicationContext());
        m1LayoutManager.setReverseLayout(false);
        m1LayoutManager.setStackFromEnd(true);
        mBluetoothRecycler.setLayoutManager(m1LayoutManager);
        mBluetoothRecycler.setAdapter(mBluetoothListAdapter);

        builder.setView(view)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogNegativeClick(BluetoothDeviceListDialog.this);
                    }
                });

        mBluetoothListAdapter.notifyDataSetChanged();

        return builder.create();
    }

    public void newListDevice(Set<BluetoothDevice> deviceSet){
        bluetoothList.clear();
        bluetoothList.addAll(deviceSet);
    }
}
