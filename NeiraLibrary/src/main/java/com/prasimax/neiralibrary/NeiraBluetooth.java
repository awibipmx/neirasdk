package com.prasimax.neiralibrary;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.io.IOException;
import java.util.Set;

/**
 * NeiraBluetooth adalah program untuk melakukan komunikasi dengan bluetooth. Kelas ini merupakan
 * ekstensi dari NeiraBluetoothSocket
 *
 * @author deani
 * @author Prasimax - Software Team
 * @version 1.0
 * @since  2018-08-10
 * @see NeiraBluetoothSocket
 * @see BluetoothDevice
 */
public class NeiraBluetooth extends NeiraBluetoothSocket {

    /**
     * Default constructor untuk NeiraBluetooth, masukkan divais bluetooth yang ingin diakses.
     * @param device Bluetooth yang akan diakses
     * @throws IOException dikeluarkan jika createRfcommSocketToServiceRecord tidak bisa dilakukan.
     */
    public NeiraBluetooth(BluetoothDevice device) throws IOException {
        super(device);
    }

    /**
     * Fungsi untuk mengambil daftar BluetoothDevice yang sudah terpair dengan phone saat ini.
     * @return daftar BluetoothDevice yang sudah terpair
     */
    public static Set<BluetoothDevice> getPairedDevice(){
        return BluetoothAdapter.getDefaultAdapter().getBondedDevices();
    }
}