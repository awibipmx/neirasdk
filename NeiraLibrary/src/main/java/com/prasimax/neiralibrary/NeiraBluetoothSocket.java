package com.prasimax.neiralibrary;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * NeiraBluetoothSocket digunakan sebagai program untuk melakukan komunikasi data bluetooth.
 *
 * @author deani
 * @author Prasimax - Software Team
 * @see BluetoothSocket
 * @see BluetoothDevice
 */
public class NeiraBluetoothSocket {
    BluetoothSocket mSocket;
    BluetoothDevice mDevice;
    private final int maxSizeWrite = 2048;
    private final long maxWriteDelay = 500;
    private final UUID idSPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    /**
     * Merupakan default constructor untuk NeiraBluetoothSocket. Pada saat constructor dijalankan
     * program akan melakukan alokasi untuk BluetoothDevice yang dipakai dan mulai membuat RF
     * Socket untuk keperluan komunikasinya.
     *
     * @param device merupakan BluetoothDevice yang akan dikomunikasikan dengan aplikasi
     * @throws IOException dikeluarkan jika pembuatan socket gagal dilakukan.
     */
    NeiraBluetoothSocket (BluetoothDevice device) throws IOException {
        mDevice = device;
        mSocket = device.createRfcommSocketToServiceRecord(idSPP);
    }


    /**
     * Fungsi ini untuk mendapatkan device yang sudah diset ketika konstruksi atau diset secara
     * manual menggunakan setDevice.
     *
     * @return device yang sudah diset. Null jika tidak ada.
     */
    public BluetoothDevice getDevice() {
        return mDevice;
    }

    /**
     * Fungsi ini untuk menset divais ke dalam aplikasi.
     *
     * @param device yang akan diset
     */
    public void setDevice(BluetoothDevice device) {
        this.mDevice = device;
    }

    /**
     * Fungsi ini untuk mendapatkan BluetoothSocket yang sudah terbuat.
     *
     * @return BluetoothSocket yg sudah di create. Null jika tidak ada.
     */
    public BluetoothSocket getSocket() {
        return mSocket;
    }

    /**
     * Fungsi untuk menset BluetoothSocket yang akan digunakan.
     *
     * @param socket yang akan digunakan.
     */
    public void setSocket(BluetoothSocket socket) {
        this.mSocket = socket;
    }

    /**
     * Fungsi ini digunakan untuk membuka koneksi socket dengan BluetoothDevice yang sudah diset.
     *
     * @throws IOException jika tidak bisa.
     */
    public void connect() throws IOException {
        mSocket = mDevice.createRfcommSocketToServiceRecord(idSPP);
        mSocket.connect();
    }

    /**
     * Fungsi ini menutup koneksi BluetoothSocket.
     *
     * @throws IOException jika tidak bisa.
     */
    public void disconnect() throws IOException {
        mSocket.close();
    }

    /**
     * Fungsi ini untuk melakukan pengecekan status koneksi BluetoothSocket.
     *
     * @return true jika terkoneksi, false jika terputus.
     */
    public boolean isConnected(){
        return mSocket.isConnected();
    }

    /**
     * Melakukan penulisan data ke Bluetooth, dikarenakan keterbatasan hardware dalam
     * penerimaan data maka data yang dikirimkan akan dibagi-bagi sebesar 1024Byte.
     * Sehingga jika data yang dikirimkan sebesar 2048Byte maka data akan dikirimkan
     * per1KB dengan delay 500ms perpengiriman.
     *
     * @param data yang akan ditulis ke Bluetooth, tidak ada batasan besarannya.
     * @return besar data yang berhasil dikirimkan, -1 jika socket belum terkoneksi.
     * @throws IOException jika socket tidak terkoneksi/terputus dalam pengirimannya.
     * @throws InterruptedException jika proses delay tidak bisa dilakukan.
     */
    public int writeData(byte[] data) throws IOException, InterruptedException {
        int result = data.length;
        if(mSocket.isConnected()){
            if (data.length <= maxSizeWrite){
                mSocket.getOutputStream().write(data);
                mSocket.getOutputStream().flush();
            }else{
                byte[][] temp = divideByte(data, maxSizeWrite);
                for (byte[] tdata: temp
                        ) {
                    mSocket.getOutputStream().write(tdata);
                    mSocket.getOutputStream().flush();
                    Thread.sleep(maxWriteDelay);
                }
            }
        }else result = -1;

        return result;
    }

    /**
     * Melakukan pembacaan data yang ada didalam InputStream BluetoothSocket.
     *
     * @param buffer yang digunakan untuk menyimpan data hasil pembacaan.
     * @return jumlah bytes yang sudah dimasukkan ke buffer atau -1 jika
     *          tidak ada data yang dapat dibaca dikarenakan sudah mencapai
     *          akhir dari stream.
     * @throws IOException jika pembacaan gagal dilakukan dikarenakan berbagai macam hal.
     */
    public int readData(byte[] buffer) throws IOException {
        return mSocket.getInputStream().read(buffer);
    }

    /**
     * Fungsi private yang digunakan untuk melakukan pemisahan data bytes.
     *
     * @param source adalah data yamg akan dipisahkan.
     * @param chunksize adalah besar pembagian data.
     * @return hasil data yang sudah dibagi dalam bentuk array.
     */
    private byte[][] divideByte(byte[] source, int chunksize){
        byte[][] ret = new byte[(int)Math.ceil(source.length / (double)chunksize)][chunksize];

        int start = 0;

        for(int i = 0; i < ret.length; i++) {
            if(start + chunksize > source.length) {
                System.arraycopy(source, start, ret[i], 0, source.length - start);
            } else {
                System.arraycopy(source, start, ret[i], 0, chunksize);
            }
            start += chunksize ;
        }


        return ret;
    }
}
