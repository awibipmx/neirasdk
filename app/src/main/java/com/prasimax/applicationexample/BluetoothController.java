package com.prasimax.applicationexample;


import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.prasimax.neiralibrary.NeiraBluetooth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BluetoothController {
    private final BluetoothRunnable runTask = new BluetoothRunnable();
    private Thread thread;
    private Handler mHandler;
    public static final int INFO = 1, BT_DATAREAD = 2, BT_DATAWRITE = 3, CTRL = 99;
    public static final String INFO_KEY = "INFO", BT_DATA_KEY = "BT_DATA", ERROR_KEY = "ERROR___";

    public void setmHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public boolean isThreadActive() {
        if(thread!=null) return thread.isAlive();
        else return false;
    }

    public Handler getmHandler() {
        return mHandler;
    }

    public boolean startService(BluetoothDevice device){
        if(mHandler == null) return false;

        boolean result = false;
        if(thread == null || !thread.isAlive()){
            runTask.setDevice(device);
            thread = new Thread(runTask);
            thread.start();
            result = true;
        }

        return result;
    }

    public void stopService(){
        if(mHandler == null) return;

        Message message = mHandler.obtainMessage(CTRL);
        Bundle bundle = new Bundle();
        bundle.putString(INFO_KEY, "STOP Service");
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    public synchronized boolean addByteToWrite(final byte[] data){
//        synchronized (thread) {
            if (runTask.dataContainer == null) return false;
            runTask.dataContainer.add(data);
            return true;
//        }
    }

    private class BluetoothRunnable implements Runnable {
        private BluetoothDevice device;

        List<byte[]> dataContainer;

        public BluetoothDevice getDevice() {
            return device;
        }

        public void setDevice(BluetoothDevice device) {
            this.device = device;
        }

        @Override
        public void run() {
            Message message = mHandler.obtainMessage(INFO);
            String exitMessage = "Connecting to: " + device.getName();
            Bundle bundle = new Bundle();
            bundle.putString(INFO_KEY, exitMessage);
            message.setData(bundle);
            mHandler.sendMessage(message);

            boolean run = true;
            int readSizeData = 0;

            try {
                NeiraBluetooth nb = new NeiraBluetooth(this.getDevice());
                nb.connect();
                dataContainer = new ArrayList<>();
                message = new Message();
                message.what = INFO;
                bundle = new Bundle();
                bundle.putString(INFO_KEY, "Connected to: "+nb.getDevice().getName()+".");
                message.setData(bundle);
                mHandler.sendMessage(message);

                while (run && nb.getSocket().isConnected()){
                    if(mHandler.hasMessages(CTRL)){
                        message = mHandler.obtainMessage(CTRL);
                        run = false;
                    }else {
                        if(mHandler.hasMessages(BT_DATAWRITE)){
                            message = mHandler.obtainMessage(BT_DATAWRITE, 77, 77);
//                            byte[] data;
//                            synchronized (this) {
//                                bundle = message.getData();
//                                data = bundle.getByteArray(BT_DATA_KEY);
//                            }

//                            synchronized (this){
                                for (byte[] data: dataContainer
                                     ) {
                                    try {
                                        nb.writeData(data);

                                        message = mHandler.obtainMessage(INFO);
                                        bundle = new Bundle();
                                        bundle.putString(INFO_KEY, "Data Successfully Written");
                                        message.setData(bundle);
                                        mHandler.sendMessage(message);

                                    } catch (InterruptedException e) {

                                        message = mHandler.obtainMessage(INFO);
                                        bundle = new Bundle();
                                        bundle.putString(INFO_KEY, "Write Data Error, data being interrupted by other thread");
                                        message.setData(bundle);
                                        mHandler.sendMessage(message);

                                        e.printStackTrace();
                                    }
                                }
                                dataContainer.clear();
//                            }

//                            if(data != null ){
//                                try {
//                                    nb.writeData(data);
//
//                                    bundle = new Bundle();
//                                    bundle.putString(INFO_KEY, "Data Successfully Written");
//                                    message.setData(bundle);
//                                    mHandler.sendMessage(message);
//
//                                } catch (InterruptedException e) {
//
//                                    bundle = new Bundle();
//                                    bundle.putString(INFO_KEY, "Write Data Error, data being interrupted by other thread");
//                                    message.setData(bundle);
//                                    mHandler.sendMessage(message);
//
//                                    e.printStackTrace();
//                                }
//                            }
                        }else{
                            // Check new read data;
                            readSizeData = nb.getSocket().getInputStream().available();
                            while (readSizeData != 0){
                                byte[] buffer = new byte[readSizeData];
                                readSizeData = nb.readData(buffer);

                                message = mHandler.obtainMessage(BT_DATAREAD);
                                bundle = new Bundle();
                                bundle.putByteArray(BT_DATA_KEY, buffer);
                                message.setData(bundle);
                                mHandler.sendMessage(message);

                                if(readSizeData == -1){
                                    readSizeData = 0;
                                }else {
                                    readSizeData = nb.getSocket().getInputStream().available();
                                }
                            }
                        }
                    }
                }

                if(nb.getSocket().isConnected()) nb.disconnect();


            } catch (IOException e) {
                message = new Message();
                message.what = INFO;
                bundle = new Bundle();
                exitMessage = "Bluetooth IO Exception occurred, failed to connect with: " + device.getName() +"\r\n";
                bundle.putString(INFO_KEY, exitMessage);
                message.setData(bundle);
                mHandler.sendMessage(message);
                e.printStackTrace();
            }

            message = new Message();
            message.what = INFO;
            bundle = new Bundle();
            exitMessage = "Disconnected from: " + device.getName();
            bundle.putString(INFO_KEY, exitMessage);
            message.setData(bundle);
            mHandler.sendMessage(message);

        }
    }

}
