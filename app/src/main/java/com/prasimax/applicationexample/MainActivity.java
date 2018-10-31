package com.prasimax.applicationexample;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.prasimax.neiralibrary.NeiraBluetooth;
import com.prasimax.neiralibrary.NeiraPrinterFunction;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        BluetoothDeviceListDialog.BluetoothDeviceListDialogListener,
        InputFunctionDialog.InputFunctionDialogInterface{

    BluetoothController btCtrl;
    BluetoothDevice mDevice;
    private RecyclerView mMessageRecycler;
    private MessageListAdapter mMessageAdapter;
    private List<BaseMessage> messageList;
    private DialogFragment bluetoothDialog;
    private final String neiraTerminal = "Neira System";

    @SuppressLint("HandlerLeak")
    Handler messageHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            Bundle bundle;
            switch (msg.what){
                case BluetoothController.INFO:
                    bundle = msg.getData();
                    displayTerminalMessage(neiraTerminal, bundle.getString(BluetoothController.INFO_KEY));
                    break;
                case BluetoothController.BT_DATAREAD:
                    bundle = msg.getData();
                    byte[] data=bundle.getByteArray(BluetoothController.BT_DATA_KEY);
                    displayTerminalMessage(mDevice.getName(), new String (data), data);
                    break;
            }
            mMessageAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Button btn = (Button) findViewById(R.id.send_button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendButton(v);
            }
        });

        messageList = new ArrayList<BaseMessage>();
        mMessageRecycler = (RecyclerView) findViewById(R.id.recyclerview_message_list);
        mMessageAdapter = new MessageListAdapter( this, messageList);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        mLayoutManager.setReverseLayout(false);
        mLayoutManager.setStackFromEnd(true);
        mMessageRecycler.setLayoutManager(mLayoutManager);
        mMessageRecycler.setAdapter(mMessageAdapter);

        bluetoothDialog = new BluetoothDeviceListDialog();

        btCtrl = new BluetoothController();
        btCtrl.setmHandler(messageHandler);

        prepareTerminalMessage();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Toast toast;
        switch (item.getItemId()){
            case R.id.action_bt_connect:
                showBluetoothDialog();
                return true;
            case R.id.action_bt_disconnect:
                btCtrl.stopService();
                return true;
            case R.id.enableHexConversion:
                if(item.isChecked()){
                    enableHexConversion = false;
                }else{
                    enableHexConversion = true;
                }
                item.setChecked(enableHexConversion);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        NavigationView nav = findViewById(R.id.nav_view);

        if(item.getItemId() == R.id.nav_printer_function || item.getItemId() == R.id.nav_sample_receipt){
            openGroupMenuItem(nav, item.getItemId());
            return true;
        }else if(item.getItemId() == R.id.nav_custombyte){
            InputFunctionDialog dialogCustomHex = InputFunctionDialog.createNewDefaultDialog(this, item,
                    "HEX DATA:", "", false);
            dialogCustomHex.setCustom(true);
            dialogCustomHex.show(getSupportFragmentManager(),(String) item.getTitle());
        }else{
            switch (item.getItemId()){
                case R.id.nav_pf_PrinterInit:
                    sendToBluetooth(NeiraPrinterFunction.PrinterInit());
                    break;
                case R.id.nav_pf_PrintSelfTest:
                    sendToBluetooth(NeiraPrinterFunction.PrintSelfTest());
                    break;
                case R.id.nav_pf_LineFeed:
                    sendToBluetooth(new byte[] {NeiraPrinterFunction.LineFeed(), 0x00});
                    break;
                case R.id.nav_pf_PrintAndFeedPaper:
                    InputFunctionDialog.createNewDefaultDialog(this, item,
                            "Multiplier", "", false)
                            .show(getSupportFragmentManager(),(String) item.getTitle());
                    break;
                case R.id.nav_pf_SetBeepTimes:
                    InputFunctionDialog.createNewDefaultDialog(this, item,
                            "Beep Times", "Delay", true)
                            .show(getSupportFragmentManager(),(String) item.getTitle());
                    break;
                case R.id.nav_pf_SetLeftMargin:
                    InputFunctionDialog.createNewDefaultDialog(this, item,
                            "Size", "", false)
                            .show(getSupportFragmentManager(),(String) item.getTitle());
                    break;
                case R.id.nav_pf_SetPrintWidth:
                    InputFunctionDialog.createNewDefaultDialog(this, item,
                            "Width", "", false)
                            .show(getSupportFragmentManager(),(String) item.getTitle());
                    break;
                case R.id.nav_pf_SetLineSpacingToDefault:
                    sendToBluetooth(NeiraPrinterFunction.SetLineSpacingToDefault());
                    break;
                case R.id.nav_pf_SetLineSpacing:
                    InputFunctionDialog.createNewDefaultDialog(this, item,
                            "Width", "", false)
                            .show(getSupportFragmentManager(),(String) item.getTitle());
                    break;
                case R.id.nav_pf_SetBold:
                    InputFunctionDialog.createNewDefaultDialog(this, item,
                            "Size", "", false)
                            .show(getSupportFragmentManager(),(String) item.getTitle());
                    break;
//                case R.id.nav_pf_SetFontSize:
//                    InputFunctionDialog.createNewDefaultDialog(this, item,
//                            "Size 1-3", "", false)
//                            .show(getSupportFragmentManager(),(String) item.getTitle());
//                    break;
                case R.id.nav_pf_SetPrinterSpeed:
                    InputFunctionDialog.createNewDefaultDialog(this, item,
                            "Speed 0(default) - 1 (lowest)", "", false)
                            .show(getSupportFragmentManager(), (String) item.getTitle());
                    break;
                case R.id.nav_pf_SetQRCode:
                    InputFunctionDialog dialogCustomString = InputFunctionDialog.createNewDefaultDialog(this, item,
                            "String QRCode:", "", false);
                    dialogCustomString.setCustom(true);
                    dialogCustomString.setAsString(true);
                    dialogCustomString.show(getSupportFragmentManager(),(String) item.getTitle());
                    break;

                case R.id.nav_pf_PrintQRCode:
                    sendToBluetooth(NeiraPrinterFunction.PrintQRCode());
                    break;
                    /**************/
                case R.id.nav_sr_receipt1:
                    sendReceipt1();
                    break;
                case R.id.nav_sr_receipt2:
                    sendReceipt2();
                    break;
                    default:
                        //Close Drawer: karena item tidak disupport untuk dicontrol.
                        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                        drawer.closeDrawer(GravityCompat.START);
                        return true;
            }
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void sendReceipt1(){
        String tmp = "\nToko Prasimart\n";
        List<byte[]> dataList = new ArrayList<>();

        //Set Up Merchant Name [Header]
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(3));
        dataList.add(NeiraPrinterFunction.SetBold(true));
        dataList.add(NeiraPrinterFunction.SetAlignMode((byte)1));
        dataList.add(tmp.getBytes());

        //Set Up Merchant Address [Header]
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(2));
        dataList.add(NeiraPrinterFunction.SetLineSpacing(10));
        dataList.add(NeiraPrinterFunction.SetAlignMode((byte) 2));
        tmp = "JL. Margonda 494D, Depok\n\n\n";
        dataList.add(tmp.getBytes());

        //Set Up Horizontal Line
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(1));
        dataList.add(NeiraPrinterFunction.SetBold(true));
        dataList.add(NeiraPrinterFunction.SetLineSpacing(10));
        tmp = "------------------------------------------------";
        dataList.add(tmp.getBytes());

        //Set Up Receipt Details
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(1));
        dataList.add(NeiraPrinterFunction.SetLineSpacing(10));
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.MEDIUM);
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        Date dt = new Date(System.currentTimeMillis());
        Random rand = new Random();

        tmp = "\n  " + df.format(dt) + "  |  " +  tf.format(dt) + "  |  No." + Integer.toString(rand.nextInt(999999998)+1) + "\n";
        dataList.add(tmp.getBytes());

        //Set Up Horizontal Line
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(1));
        dataList.add(NeiraPrinterFunction.SetBold(true));
        dataList.add(NeiraPrinterFunction.SetLineSpacing(10));
        tmp = "------------------------------------------------";
        dataList.add(tmp.getBytes());

        //Set Up Receipt Contents
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(1));
        dataList.add(NeiraPrinterFunction.SetLineSpacing(10));
        dataList.add(NeiraPrinterFunction.SetLeftMargin(4));
        tmp =   "Item 1                       | 1 |    1000\n" +
                "Item 2                       | 1 |    2000\n" +
                "Item 3                       | 1 |    3000\n" +
                "Item 4                       | 1 |    4000\n" +
                "Item 5                       | 1 |    5000\n" +
                "Item 6                       | 1 |    6000\n" +
                "\n" +
                "\n" +
                "                      --------------------\n" +
                "Harga Jual                           21000\n" +
                "Diskon 10%                            2100\n" +
                "                      --------------------\n" +
                "Total                                18900\n" +
                "Tunai                                20000\n" +
                "                      --------------------\n" +
                "Kembali                               1100\n" +
                 "\n\n\n";
        dataList.add(tmp.getBytes());

        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.StoreQRCodeData("http://www.prasimax.com/"));
        dataList.add(NeiraPrinterFunction.PrintQRCode());

        //Set Up Footer
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(1));
        dataList.add(NeiraPrinterFunction.SetLineSpacing(10));
        dataList.add(NeiraPrinterFunction.SetFontType(49)); //Set to Italic
        dataList.add(NeiraPrinterFunction.SetAlignMode((byte) 1));
        tmp =   "\n\nTerima Kasih Atas Kunjungan Anda\n" +
                "Selamat Belanja Kembali .....\n\n\n\n\n\n\n";
        dataList.add(tmp.getBytes());

        dataList.add(NeiraPrinterFunction.SetBeepTimes(2,1));

        byte[] image =
                {
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x04, (byte) 0x04, (byte) 0x06, (byte) 0x06, (byte) 0x02, (byte) 0x08,
                (byte) 0xFE, (byte) 0xC4, (byte) 0x00, (byte) 0x28, (byte) 0x10, (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x04, (byte) 0x02, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x02,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x02, (byte) 0x04, (byte) 0x10, (byte) 0x04, (byte) 0x06, (byte) 0x12, (byte) 0x20, (byte) 0x30,
                (byte) 0x12, (byte) 0x40, (byte) 0x32, (byte) 0x50, (byte) 0x22, (byte) 0x60, (byte) 0x70, (byte) 0x14, (byte) 0x22, (byte) 0x42, (byte) 0xFE, (byte) 0xDA, (byte) 0x00, (byte) 0x0C, (byte) 0x02, (byte) 0x00,
                (byte) 0x00, (byte) 0x02, (byte) 0x11, (byte) 0x02, (byte) 0x10, (byte) 0x00, (byte) 0x3E, (byte) 0x00, (byte) 0xF0, (byte) 0x92, (byte) 0x10, (byte) 0x84, (byte) 0x00, (byte) 0x20, (byte) 0x08, (byte) 0x41,
                (byte) 0x03, (byte) 0x11, (byte) 0x84, (byte) 0x00, (byte) 0x20, (byte) 0x08, (byte) 0x40, (byte) 0x02, (byte) 0x10, (byte) 0xB2, (byte) 0x84, (byte) 0xBA, (byte) 0x02, (byte) 0x08, (byte) 0x58, (byte) 0x00,
                (byte) 0x6C, (byte) 0x1A, (byte) 0x9C, (byte) 0x8C, (byte) 0x4E, (byte) 0x42, (byte) 0x6C, (byte) 0xD6, (byte) 0x0A, (byte) 0x39, (byte) 0x5D, (byte) 0x03, (byte) 0x16, (byte) 0xEC, (byte) 0x8C, (byte) 0xF8,
                (byte) 0x28, (byte) 0xB4, (byte) 0x60, (byte) 0x4E, (byte) 0x7E, (byte) 0x90, (byte) 0xE4, (byte) 0xCB, (byte) 0x47, (byte) 0x1F, (byte) 0x15, (byte) 0x70, (byte) 0xA4, (byte) 0x4C, (byte) 0x84, (byte) 0x9E,
                (byte) 0x8A, (byte) 0x70, (byte) 0x4E, (byte) 0xEE, (byte) 0x22, (byte) 0xAC, (byte) 0xA0, (byte) 0xC2, (byte) 0x5A, (byte) 0x24, (byte) 0xE8, (byte) 0xF0, (byte) 0xF2, (byte) 0x20, (byte) 0x16, (byte) 0x08,
                (byte) 0x47, (byte) 0x0B, (byte) 0x59, (byte) 0x60, (byte) 0x76, (byte) 0x90, (byte) 0x5A, (byte) 0x3A, (byte) 0x12, (byte) 0xE4, (byte) 0x92, (byte) 0xCE, (byte) 0x12, (byte) 0xD8, (byte) 0xFC, (byte) 0x03,
                (byte) 0xB1, (byte) 0x09, (byte) 0x71, (byte) 0xB0, (byte) 0x84, (byte) 0xA0, (byte) 0xD0, (byte) 0x94, (byte) 0xA0, (byte) 0x62, (byte) 0xAE, (byte) 0xB6, (byte) 0x8E, (byte) 0x9C, (byte) 0x3C, (byte) 0xA2,
                (byte) 0xDA, (byte) 0x98, (byte) 0xC4, (byte) 0x0A, (byte) 0xA0, (byte) 0x6A, (byte) 0xD4, (byte) 0x85, (byte) 0x0B, (byte) 0x55, (byte) 0xCB, (byte) 0x26, (byte) 0xAC, (byte) 0x9B, (byte) 0xA1, (byte) 0x67,
                (byte) 0x0B, (byte) 0x08, (byte) 0xA6, (byte) 0x9A, (byte) 0x14, (byte) 0x10, (byte) 0x84, (byte) 0x24, (byte) 0x01, (byte) 0x43, (byte) 0x11, (byte) 0x81, (byte) 0x04, (byte) 0x20, (byte) 0x08, (byte) 0x00,
                (byte) 0x42, (byte) 0x10, (byte) 0x80, (byte) 0x04, (byte) 0x20, (byte) 0x08, (byte) 0x00, (byte) 0x42, (byte) 0x16, (byte) 0xC0, (byte) 0x2E, (byte) 0x4A, (byte) 0x2C, (byte) 0x80, (byte) 0xAA, (byte) 0xCF,
                (byte) 0x17, (byte) 0xC1, (byte) 0xAA, (byte) 0x70, (byte) 0xC5, (byte) 0x27, (byte) 0xBD, (byte) 0x69, (byte) 0x48, (byte) 0xF2, (byte) 0xE4, (byte) 0x72, (byte) 0x00, (byte) 0x6C, (byte) 0x1A, (byte) 0xBA,
                (byte) 0x36, (byte) 0x33, (byte) 0x57, (byte) 0x69, (byte) 0xE0, (byte) 0x26, (byte) 0xA2, (byte) 0xB6, (byte) 0xC4, (byte) 0xE0, (byte) 0xEC, (byte) 0xB0, (byte) 0xFA, (byte) 0x3C, (byte) 0x4A, (byte) 0xC4,
                (byte) 0x84, (byte) 0xCC, (byte) 0x60, (byte) 0x2A, (byte) 0xB2, (byte) 0x20, (byte) 0x26, (byte) 0xA3, (byte) 0x5F, (byte) 0x4E, (byte) 0x44, (byte) 0xE6, (byte) 0x9F, (byte) 0xDD, (byte) 0x29, (byte) 0xDF,
                (byte) 0x86, (byte) 0xCC, (byte) 0x24, (byte) 0x84, (byte) 0x78, (byte) 0x0A, (byte) 0xD6, (byte) 0x70, (byte) 0xFC, (byte) 0x2E, (byte) 0x29, (byte) 0x69, (byte) 0xC9, (byte) 0x10, (byte) 0xE6, (byte) 0x78,
                (byte) 0xC4, (byte) 0x0C, (byte) 0x2A, (byte) 0x9C, (byte) 0xD0, (byte) 0x38, (byte) 0xD2, (byte) 0xDA, (byte) 0xE4, (byte) 0x90, (byte) 0x8C, (byte) 0x30, (byte) 0xA8, (byte) 0xA4, (byte) 0xA3, (byte) 0x4B,
                (byte) 0x4B, (byte) 0x28, (byte) 0x1E, (byte) 0x2C, (byte) 0xFB, (byte) 0x29, (byte) 0xCD, (byte) 0x82, (byte) 0x42, (byte) 0x4C, (byte) 0x28, (byte) 0x6E, (byte) 0xC8, (byte) 0x3E, (byte) 0xCA, (byte) 0x50,
                (byte) 0x56, (byte) 0x12, (byte) 0x88, (byte) 0x89, (byte) 0x1D, (byte) 0xD9, (byte) 0xC6, (byte) 0x1C, (byte) 0x92, (byte) 0x96, (byte) 0xA2, (byte) 0xAA, (byte) 0xA2, (byte) 0xD2, (byte) 0xB2, (byte) 0xC8,
                (byte) 0x8E, (byte) 0x96, (byte) 0x7C, (byte) 0x92, (byte) 0x94, (byte) 0x5A, (byte) 0x53, (byte) 0x53, (byte) 0x32, (byte) 0xDC, (byte) 0x9E, (byte) 0x65, (byte) 0xE9, (byte) 0x2B, (byte) 0x4E, (byte) 0x66,
                (byte) 0x42, (byte) 0x38, (byte) 0x7A, (byte) 0xEA, (byte) 0x86, (byte) 0xB2, (byte) 0x28, (byte) 0xC6, (byte) 0x36, (byte) 0xBC, (byte) 0x86, (byte) 0x81, (byte) 0xF7, (byte) 0x29, (byte) 0x4C, (byte) 0xB6,
                (byte) 0xBC, (byte) 0x30, (byte) 0x46, (byte) 0xE0, (byte) 0x1C, (byte) 0x94, (byte) 0x62, (byte) 0x46, (byte) 0x74, (byte) 0x30, (byte) 0x46, (byte) 0x5C, (byte) 0x07, (byte) 0xDF, (byte) 0x93, (byte) 0x8F,
                (byte) 0x7E, (byte) 0x36, (byte) 0xC7, (byte) 0xE3, (byte) 0x79, (byte) 0x6C, (byte) 0x1E, (byte) 0x94, (byte) 0x72, (byte) 0x90, (byte) 0xE6, (byte) 0x28, (byte) 0xB4, (byte) 0xAC, (byte) 0xAC, (byte) 0x1E,
                (byte) 0xDC, (byte) 0x9E, (byte) 0x64, (byte) 0xD9, (byte) 0x57, (byte) 0xA7, (byte) 0xA5, (byte) 0x8E, (byte) 0x3E, (byte) 0x2C, (byte) 0xFA, (byte) 0x2E, (byte) 0x50, (byte) 0xC6, (byte) 0x64, (byte) 0xD2,
                (byte) 0x14, (byte) 0xCC, (byte) 0xE0, (byte) 0x86, (byte) 0xE5, (byte) 0x4F, (byte) 0x1B, (byte) 0xCE, (byte) 0xE0, (byte) 0x99, (byte) 0x19, (byte) 0xFF, (byte) 0x01, (byte) 0x36, (byte) 0x60, (byte) 0x24,
                (byte) 0x56, (byte) 0x9E, (byte) 0xCC, (byte) 0xDE, (byte) 0xDC, (byte) 0xF0, (byte) 0xC6, (byte) 0x10, (byte) 0x72, (byte) 0x08, (byte) 0xC8, (byte) 0x72, (byte) 0x77, (byte) 0x05, (byte) 0x2D, (byte) 0x1D,
                (byte) 0xCE, (byte) 0x1A, (byte) 0x92, (byte) 0xE0, (byte) 0x34, (byte) 0x0E, (byte) 0xB8, (byte) 0xC4, (byte) 0xF2, (byte) 0xAC, (byte) 0x4C, (byte) 0xAB, (byte) 0x47, (byte) 0x7F, (byte) 0x03, (byte) 0x90,
                (byte) 0xCB, (byte) 0x47, (byte) 0xF7, (byte) 0xFF, (byte) 0x14, (byte) 0xE8, (byte) 0x0A, (byte) 0xCE, (byte) 0x82, (byte) 0x9A, (byte) 0x32, (byte) 0xEE, (byte) 0x90, (byte) 0x7C, (byte) 0x94, (byte) 0x3E,
                (byte) 0xE6, (byte) 0xA2, (byte) 0xE4, (byte) 0x8C, (byte) 0x9E, (byte) 0x95, (byte) 0x7D, (byte) 0x95, (byte) 0x8B, (byte) 0x38, (byte) 0xCC, (byte) 0x2C, (byte) 0xA4, (byte) 0xB8, (byte) 0x34, (byte) 0x78,
                (byte) 0x52, (byte) 0x94, (byte) 0x4A, (byte) 0x99, (byte) 0xF1, (byte) 0x11, (byte) 0xCC, (byte) 0x2F, (byte) 0x4F, (byte) 0x89, (byte) 0x8F, (byte) 0x8A, (byte) 0x74, (byte) 0x5E, (byte) 0xA6, (byte) 0x24,
                (byte) 0x8E, (byte) 0x3E, (byte) 0x2C, (byte) 0xFA, (byte) 0x26, (byte) 0x4A, (byte) 0xBA, (byte) 0x4C, (byte) 0x90, (byte) 0x92, (byte) 0x96, (byte) 0x14, (byte) 0x42, (byte) 0x9A, (byte) 0xD3, (byte) 0xBF,
                (byte) 0x37, (byte) 0x00, (byte) 0xD8, (byte) 0xDE, (byte) 0x46, (byte) 0x1C, (byte) 0xC4, (byte) 0xA0, (byte) 0x6A, (byte) 0x72, (byte) 0x9E, (byte) 0x91, (byte) 0xCD, (byte) 0xCE, (byte) 0x42, (byte) 0x4B,
                (byte) 0x25, (byte) 0x45, (byte) 0x74, (byte) 0x58, (byte) 0xFC, (byte) 0xDE, (byte) 0x12, (byte) 0xCA, (byte) 0x7E, (byte) 0x88, (byte) 0x10, (byte) 0xB0, (byte) 0x30, (byte) 0x20, (byte) 0x0A, (byte) 0x0A,
                (byte) 0xB2, (byte) 0x98, (byte) 0x84, (byte) 0xCC, (byte) 0x84, (byte) 0x9E, (byte) 0xB7, (byte) 0x1F, (byte) 0x51, (byte) 0x7E, (byte) 0x46, (byte) 0x52, (byte) 0xD8, (byte) 0xAA, (byte) 0x16, (byte) 0x48,
                (byte) 0x58, (byte) 0x50, (byte) 0x9A, (byte) 0xD0, (byte) 0xE8, (byte) 0x10, (byte) 0x85, (byte) 0x25, (byte) 0x01, (byte) 0x43, (byte) 0x10, (byte) 0x80, (byte) 0x04, (byte) 0x90, (byte) 0x16, (byte) 0x02,
                (byte) 0xDC, (byte) 0xA0, (byte) 0x38, (byte) 0x5C, (byte) 0x1C, (byte) 0x98, (byte) 0x8C, (byte) 0x80, (byte) 0x0A, (byte) 0xA2, (byte) 0x58, (byte) 0x94, (byte) 0xBC, (byte) 0x50, (byte) 0x92, (byte) 0x53,
                (byte) 0x85, (byte) 0x25, (byte) 0x1B, (byte) 0x9E, (byte) 0x46, (byte) 0xCA, (byte) 0x4A, (byte) 0xC6, (byte) 0xF0, (byte) 0x12, (byte) 0xBC, (byte) 0xAE, (byte) 0x02, (byte) 0x32, (byte) 0xB3, (byte) 0x43,
                (byte) 0x28, (byte) 0xE0, (byte) 0x26, (byte) 0xA2, (byte) 0x52, (byte) 0x14, (byte) 0x22, (byte) 0xDC, (byte) 0xF8, (byte) 0x54, (byte) 0x8E, (byte) 0xDA, (byte) 0x62, (byte) 0x92, (byte) 0x52, (byte) 0x3C,
                (byte) 0x06, (byte) 0xD8, (byte) 0x4A, (byte) 0x2C, (byte) 0x7A, (byte) 0x4A, (byte) 0x58, (byte) 0x30, (byte) 0xF3, (byte) 0x89, (byte) 0xFB, (byte) 0x2C, (byte) 0xEE, (byte) 0x0E, (byte) 0x4E, (byte) 0x54,
                (byte) 0x5A, (byte) 0x4E, (byte) 0xC2, (byte) 0x2C, (byte) 0xAE, (byte) 0x6E, (byte) 0xD1, (byte) 0x03, (byte) 0xA6, (byte) 0xB6, (byte) 0x48, (byte) 0xF2, (byte) 0x3C, (byte) 0x04, (byte) 0x3C, (byte) 0x5A,
                (byte) 0xEC, (byte) 0x12, (byte) 0xC8, (byte) 0x46, (byte) 0x80, (byte) 0xFA, (byte) 0x2A, (byte) 0x4A, (byte) 0xCC, (byte) 0xA0, (byte) 0x26, (byte) 0x90, (byte) 0xB6, (byte) 0xE4, (byte) 0x9E, (byte) 0x64,
                (byte) 0x3F, (byte) 0xD3, (byte) 0x9D, (byte) 0x9B, (byte) 0x4A, (byte) 0xF2, (byte) 0xD2, (byte) 0x60, (byte) 0x38, (byte) 0xF2, (byte) 0xC2, (byte) 0xB6, (byte) 0x76, (byte) 0xE0, (byte) 0x60, (byte) 0xC6,
                (byte) 0xE8, (byte) 0xF0, (byte) 0xA8, (byte) 0xC8, (byte) 0xA6, (byte) 0xEC, (byte) 0x9A, (byte) 0x46, (byte) 0x58, (byte) 0x4A, (byte) 0x7E, (byte) 0x50, (byte) 0x3E, (byte) 0x64, (byte) 0x62, (byte) 0x68,
                (byte) 0x9E, (byte) 0xCE, (byte) 0xA6, (byte) 0x98, (byte) 0xB6, (byte) 0xE4, (byte) 0x1E, (byte) 0x64, (byte) 0x66, (byte) 0xD3, (byte) 0xD9, (byte) 0xF5, (byte) 0xFD, (byte) 0x94, (byte) 0xB8, (byte) 0x9E,
                (byte) 0x46, (byte) 0xD4, (byte) 0x4C, (byte) 0xDE, (byte) 0xE0, (byte) 0xC2, (byte) 0xBE, (byte) 0xFE, (byte) 0xA6, (byte) 0x90, (byte) 0x58, (byte) 0xAA, (byte) 0xBC, (byte) 0x54, (byte) 0x3C, (byte) 0x18,
                (byte) 0xE8, (byte) 0x9A, (byte) 0x28, (byte) 0xA0, (byte) 0xC6, (byte) 0x02, (byte) 0x62, (byte) 0xFC, (byte) 0xE2, (byte) 0xF4, (byte) 0x76, (byte) 0x3E, (byte) 0x6C, (byte) 0x2C, (byte) 0xC6, (byte) 0x26,
                (byte) 0xD6, (byte) 0xB5, (byte) 0xD7, (byte) 0xB9, (byte) 0x53, (byte) 0xE6, (byte) 0xFC, (byte) 0x1E, (byte) 0x86, (byte) 0x32, (byte) 0x7E, (byte) 0x58, (byte) 0x86, (byte) 0xDE, (byte) 0xA4, (byte) 0x6C,
                (byte) 0x36, (byte) 0x86, (byte) 0xB4, (byte) 0xD6, (byte) 0x28, (byte) 0xE2, (byte) 0x8C, (byte) 0xC0, (byte) 0x66, (byte) 0xBA, (byte) 0x02, (byte) 0x2E, (byte) 0x3E, (byte) 0x60, (byte) 0x96, (byte) 0xC8,
                (byte) 0x76, (byte) 0x82, (byte) 0x94, (byte) 0x86, (byte) 0x3A, (byte) 0x4C, (byte) 0x02, (byte) 0x22, (byte) 0xC6, (byte) 0xDC, (byte) 0xED, (byte) 0xC1, (byte) 0x17, (byte) 0x7C, (byte) 0x86, (byte) 0x20,
                (byte) 0xF8, (byte) 0xA6, (byte) 0xD6, (byte) 0x00, (byte) 0x08, (byte) 0x72, (byte) 0xCE, (byte) 0x10, (byte) 0x26, (byte) 0x24, (byte) 0x9E, (byte) 0x64, (byte) 0x70, (byte) 0x9A, (byte) 0x66, (byte) 0xB4,
                (byte) 0x1C, (byte) 0x6C, (byte) 0x56, (byte) 0x0E, (byte) 0x36, (byte) 0x9E, (byte) 0xCC, (byte) 0xCA, (byte) 0x7E, (byte) 0x64, (byte) 0xA4, (byte) 0x4A, (byte) 0x8C, (byte) 0x50, (byte) 0x88, (byte) 0xAC,
                (byte) 0xC2, (byte) 0x7A, (byte) 0xAA, (byte) 0xAC, (byte) 0xBA, (byte) 0xAA, (byte) 0xA2, (byte) 0x48, (byte) 0x8E, (byte) 0x72, (byte) 0x86, (byte) 0x38, (byte) 0x90, (byte) 0x3E, (byte) 0x82, (byte) 0xA2,
                (byte) 0x6C, (byte) 0x96, (byte) 0x00, (byte) 0xE6, (byte) 0x72, (byte) 0xDC, (byte) 0x74, (byte) 0xA8, (byte) 0xA8, (byte) 0x0C, (byte) 0x68, (byte) 0x76, (byte) 0x12, (byte) 0x40, (byte) 0x1C, (byte) 0xCE,
                (byte) 0x36, (byte) 0xC8, (byte) 0xE0, (byte) 0x5C, (byte) 0x32, (byte) 0xD6, (byte) 0x62, (byte) 0xF2, (byte) 0x54, (byte) 0xCC, (byte) 0xB6, (byte) 0xF6, (byte) 0xC6, (byte) 0x0E, (byte) 0xBE, (byte) 0x08,
                (byte) 0x1E, (byte) 0x32, (byte) 0x16, (byte) 0xD4, (byte) 0x14, (byte) 0xD6, (byte) 0x1A, (byte) 0x5A, (byte) 0xF2, (byte) 0x6E, (byte) 0xAE, (byte) 0x9E, (byte) 0x9C, (byte) 0xB8, (byte) 0xFC, (byte) 0xC0,
                (byte) 0xEC, (byte) 0xB0, (byte) 0xFC, (byte) 0x5A, (byte) 0x76, (byte) 0x3E, (byte) 0xCA, (byte) 0x8C, (byte) 0xD4, (byte) 0xCC, (byte) 0xC0, (byte) 0x18, (byte) 0x0A, (byte) 0x94, (byte) 0x72, (byte) 0x34,
                (byte) 0xE0, (byte) 0x9C, (byte) 0xA2, (byte) 0x32, (byte) 0x70, (byte) 0x7A, (byte) 0x40, (byte) 0x4A, (byte) 0x28, (byte) 0xF4, (byte) 0x72, (byte) 0xA4, (byte) 0x60, (byte) 0x64, (byte) 0xDA, (byte) 0xD2,
                (byte) 0x4C, (byte) 0x54, (byte) 0x38, (byte) 0x18, (byte) 0x22, (byte) 0x3C, (byte) 0xDA, (byte) 0xBE, (byte) 0xA4, (byte) 0x6E, (byte) 0x0E, (byte) 0xE4, (byte) 0xB6, (byte) 0x94, (byte) 0x68, (byte) 0x0A,
                (byte) 0x98, (byte) 0xC6, (byte) 0x7A, (byte) 0x24, (byte) 0x12, (byte) 0xDC, (byte) 0xD2, (byte) 0xA2, (byte) 0x3C, (byte) 0x0E, (byte) 0xF2, (byte) 0x0C, (byte) 0x8E, (byte) 0x7C, (byte) 0x28, (byte) 0xB2,
                (byte) 0x1C, (byte) 0x0E, (byte) 0xC4, (byte) 0x68, (byte) 0x30, (byte) 0x20, (byte) 0x6C, (byte) 0xC8, (byte) 0x5A, (byte) 0x62, (byte) 0x72, (byte) 0x38, (byte) 0x98, (byte) 0xFE, (byte) 0x00, (byte) 0x8C,
                (byte) 0xCE, (byte) 0x0E, (byte) 0xB8, (byte) 0xB0, (byte) 0xE2, (byte) 0xEE, (byte) 0xD8, (byte) 0xDA, (byte) 0x26, (byte) 0x82, (byte) 0xE2, (byte) 0x28, (byte) 0x3A, (byte) 0xB8, (byte) 0xE0, (byte) 0x76,
                (byte) 0xEE, (byte) 0xBE, (byte) 0x26, (byte) 0x06, (byte) 0x34, (byte) 0xFC, (byte) 0xC2, (byte) 0xAE, (byte) 0xB4, (byte) 0x06, (byte) 0x82, (byte) 0x96, (byte) 0x12, (byte) 0xEE, (byte) 0x92, (byte) 0x7C,
                (byte) 0x95, (byte) 0x97, (byte) 0x93, (byte) 0xBA, (byte) 0xAE, (byte) 0x8A, (byte) 0x6E, (byte) 0xF6, (byte) 0x8A, (byte) 0xAE, (byte) 0x6A, (byte) 0x66, (byte) 0xE6, (byte) 0x72, (byte) 0x12, (byte) 0xCA,
                (byte) 0x72, (byte) 0xF4, (byte) 0xC6, (byte) 0x3E, (byte) 0xBA, (byte) 0x78, (byte) 0xA6, (byte) 0xD6, (byte) 0x14, (byte) 0xA0, (byte) 0xF6, (byte) 0x76, (byte) 0x9A, (byte) 0x58, (byte) 0xAE, (byte) 0x30,
                (byte) 0x1C, (byte) 0xB8, (byte) 0xDA, (byte) 0xD0, (byte) 0xC8, (byte) 0x06, (byte) 0xD0, (byte) 0x80, (byte) 0x83, (byte) 0xF7, (byte) 0x1F, (byte) 0x74, (byte) 0xAA, (byte) 0xE2, (byte) 0xFA, (byte) 0xDA,
                (byte) 0xC8, (byte) 0x34, (byte) 0x1A, (byte) 0x90, (byte) 0xC4, (byte) 0xF0, (byte) 0x92, (byte) 0xF4, (byte) 0x50, (byte) 0x56, (byte) 0xAC, (byte) 0x2E, (byte) 0x24, (byte) 0x24, (byte) 0xDE, (byte) 0x2C,
                (byte) 0x8E, (byte) 0xB2, (byte) 0x88, (byte) 0xDC, (byte) 0x6C, (byte) 0x4E, (byte) 0x88, (byte) 0x9C, (byte) 0xB4, (byte) 0xAE, (byte) 0x5A, (byte) 0x54, (byte) 0x68, (byte) 0xFA, (byte) 0x26, (byte) 0xA4,
                (byte) 0xA5, (byte) 0x7D, (byte) 0x45, (byte) 0x9A, (byte) 0x66, (byte) 0x4A, (byte) 0xE0, (byte) 0x32, (byte) 0xF8, (byte) 0x5C, (byte) 0x38, (byte) 0x64, (byte) 0x8E, (byte) 0xEA, (byte) 0xDE, (byte) 0xEA,
                (byte) 0x36, (byte) 0x54, (byte) 0x7E, (byte) 0xAC, (byte) 0xD0, (byte) 0x32, (byte) 0xD2, (byte) 0xB8, (byte) 0xFE, (byte) 0x00, (byte) 0x24, (byte) 0x8E, (byte) 0xB2, (byte) 0xDA, (byte) 0xE2, (byte) 0x66,
                (byte) 0x60, (byte) 0x72, (byte) 0x90, (byte) 0xFA, (byte) 0xD8, (byte) 0x18, (byte) 0xC6, (byte) 0x51, (byte) 0x65, (byte) 0x01, (byte) 0x51, (byte) 0x4C, (byte) 0xE6, (byte) 0x92, (byte) 0xB2, (byte) 0x48,
                (byte) 0xF8, (byte) 0xF0, (byte) 0xA6, (byte) 0xD6, (byte) 0xBA, (byte) 0x0C, (byte) 0x90, (byte) 0xB8, (byte) 0xDE, (byte) 0x02, (byte) 0x3E, (byte) 0xCA, (byte) 0x2A, (byte) 0x5A, (byte) 0x44, (byte) 0xE6,
                (byte) 0x12, (byte) 0x08, (byte) 0xC2, (byte) 0xCE, (byte) 0x72, (byte) 0xFC, (byte) 0x3A, (byte) 0xA4, (byte) 0xE4, (byte) 0x04, (byte) 0x3A, (byte) 0x5C, (byte) 0xC2, (byte) 0x2A, (byte) 0x82, (byte) 0xD5,
                (byte) 0x85, (byte) 0xAB, (byte) 0x58, (byte) 0xC8, (byte) 0x3C, (byte) 0x96, (byte) 0x06, (byte) 0x0C, (byte) 0x2E, (byte) 0x78, (byte) 0x98, (byte) 0x86, (byte) 0x2A, (byte) 0xA4, (byte) 0xA6, (byte) 0x4A,
                (byte) 0x8C, (byte) 0xB6, (byte) 0x72, (byte) 0x42, (byte) 0xC8, (byte) 0x58, (byte) 0x54, (byte) 0xAC, (byte) 0x68, (byte) 0xF6, (byte) 0x08, (byte) 0x42, (byte) 0x12, (byte) 0x20, (byte) 0x36, (byte) 0x00,
                (byte) 0x74, (byte) 0x8C, (byte) 0xB8, (byte) 0x2A, (byte) 0x9A, (byte) 0x42, (byte) 0x56, (byte) 0x4F, (byte) 0xCD, (byte) 0xB9, (byte) 0x2A, (byte) 0xBE, (byte) 0x32, (byte) 0x14, (byte) 0xDA, (byte) 0x62,
                (byte) 0x42, (byte) 0x72, (byte) 0x96, (byte) 0x90, (byte) 0xB6, (byte) 0xDC, (byte) 0x4C, (byte) 0x5E, (byte) 0xE0, (byte) 0xB2, (byte) 0x9C, (byte) 0xE8, (byte) 0x9A, (byte) 0x12, (byte) 0xAA, (byte) 0x1E,
                (byte) 0xDE, (byte) 0x06, (byte) 0x52, (byte) 0x36, (byte) 0x98, (byte) 0xA0, (byte) 0x32, (byte) 0xCA, (byte) 0xD0, (byte) 0x84, (byte) 0x7C, (byte) 0x76, (byte) 0x68, (byte) 0xA6, (byte) 0xDA, (byte) 0x29,
                (byte) 0x65, (byte) 0x8F, (byte) 0x68, (byte) 0x0C, (byte) 0x68, (byte) 0xCA, (byte) 0x88, (byte) 0xE8, (byte) 0x16, (byte) 0x58, (byte) 0xC4, (byte) 0xA2, (byte) 0xAE, (byte) 0x3E, (byte) 0x1A, (byte) 0xE4,
                (byte) 0x98, (byte) 0x02, (byte) 0x4C, (byte) 0xCC, (byte) 0xE8, (byte) 0xD2, (byte) 0x46, (byte) 0xE8, (byte) 0x56, (byte) 0x4E, (byte) 0xD6, (byte) 0xB8, (byte) 0xD0, (byte) 0x60, (byte) 0xA0, (byte) 0x64,
                (byte) 0x92, (byte) 0x14, (byte) 0xFA, (byte) 0xDE, (byte) 0x66, (byte) 0xD2, (byte) 0xD6, (byte) 0xDF, (byte) 0x11, (byte) 0x34, (byte) 0x86, (byte) 0x76, (byte) 0x8E, (byte) 0x6C, (byte) 0x84, (byte) 0x9D,
                (byte) 0x5F, (byte) 0x9F, (byte) 0xC0, (byte) 0x28, (byte) 0xAA, (byte) 0x38, (byte) 0x6A, (byte) 0x60, (byte) 0x80, (byte) 0x70, (byte) 0x42, (byte) 0xDE, (byte) 0x4C, (byte) 0x1C, (byte) 0x7D, (byte) 0x4B,
                (byte) 0x4B, (byte) 0xD8, (byte) 0xFC, (byte) 0x40, (byte) 0x70, (byte) 0xAE, (byte) 0x7A, (byte) 0xCA, (byte) 0xF4, (byte) 0x76, (byte) 0x44, (byte) 0xF2, (byte) 0x70, (byte) 0xA6, (byte) 0xC4, (byte) 0x49,
                (byte) 0x75, (byte) 0xD0, (byte) 0x92, (byte) 0xF2, (byte) 0x3B, (byte) 0xC7, (byte) 0x75, (byte) 0xA5, (byte) 0xA7, (byte) 0x19, (byte) 0xA0, (byte) 0xB4, (byte) 0xC6, (byte) 0xDC, (byte) 0x72, (byte) 0x74,
                (byte) 0xC6, (byte) 0x88, (byte) 0xFA, (byte) 0x0C, (byte) 0x7F, (byte) 0xA9, (byte) 0x2D, (byte) 0x75, (byte) 0xC3, (byte) 0xBE, (byte) 0xB4, (byte) 0xE2, (byte) 0xA2, (byte) 0xA4, (byte) 0xC6, (byte) 0x3E,
                (byte) 0xF6, (byte) 0x6C, (byte) 0xF0, (byte) 0xB6, (byte) 0xD8, (byte) 0x2E, (byte) 0x2D, (byte) 0x69, (byte) 0x01, (byte) 0xED, (byte) 0xD3, (byte) 0x9B, (byte) 0x4F, (byte) 0x33, (byte) 0xF9, (byte) 0x0F,
                (byte) 0x3D, (byte) 0x6F, (byte) 0xB2, (byte) 0xD6, (byte) 0xE6, (byte) 0x5E, (byte) 0x7A, (byte) 0xFE, (byte) 0x00, (byte) 0xB2, (byte) 0x4D, (byte) 0x8F, (byte) 0xA9, (byte) 0xA5, (byte) 0xE9, (byte) 0xC9,
                (byte) 0x69, (byte) 0x06, (byte) 0xA2, (byte) 0xC6, (byte) 0x1A, (byte) 0x42, (byte) 0x4E, (byte) 0x9A, (byte) 0x48, (byte) 0xFA, (byte) 0x96, (byte) 0x80, (byte) 0x4E, (byte) 0x10, (byte) 0xD9, (byte) 0x21,
                (byte) 0x9F, (byte) 0xBD, (byte) 0xA1, (byte) 0xA7, (byte) 0x7F, (byte) 0x7E, (byte) 0x92, (byte) 0x8C, (byte) 0x28, (byte) 0xBA, (byte) 0xA6, (byte) 0xA6, (byte) 0x84, (byte) 0x10, (byte) 0x08, (byte) 0x4C,
                (byte) 0xB5, (byte) 0x55, (byte) 0x25, (byte) 0x37, (byte) 0x39, (byte) 0xE6, (byte) 0x90, (byte) 0x55, (byte) 0xD6, (byte) 0x32, (byte) 0x72, (byte) 0x5E, (byte) 0x98, (byte) 0xEA, (byte) 0x2E, (byte) 0xC0,
                (byte) 0x64, (byte) 0xB8, (byte) 0xC8, (byte) 0xC6, (byte) 0x42, (byte) 0x81, (byte) 0x15, (byte) 0xDD, (byte) 0x2B, (byte) 0x5B, (byte) 0xCF, (byte) 0x2D, (byte) 0xC8, (byte) 0xD8, (byte) 0x44, (byte) 0xBE,
                (byte) 0x6A, (byte) 0xC4, (byte) 0x0C, (byte) 0x5C, (byte) 0xBC, (byte) 0x6E, (byte) 0xC0, (byte) 0x05, (byte) 0xAF, (byte) 0xC9, (byte) 0x3F, (byte) 0x7B, (byte) 0x14, (byte) 0x9E, (byte) 0xF7, (byte) 0x77,
                (byte) 0x0C, (byte) 0x26, (byte) 0x04, (byte) 0x72, (byte) 0xF2, (byte) 0x4E, (byte) 0x3C, (byte) 0x96, (byte) 0x76, (byte) 0x58, (byte) 0x76, (byte) 0xEC, (byte) 0x1A, (byte) 0x4B, (byte) 0x9B, (byte) 0x95,
                (byte) 0xD5, (byte) 0x8F, (byte) 0xB4, (byte) 0x5A, (byte) 0xE2, (byte) 0x1C, (byte) 0xDA, (byte) 0x66, (byte) 0x30, (byte) 0xF0, (byte) 0xF6, (byte) 0xCE, (byte) 0xF2, (byte) 0x68, (byte) 0xC9, (byte) 0xC9,
                (byte) 0x1D, (byte) 0x4F, (byte) 0xD9, (byte) 0x7A, (byte) 0x84, (byte) 0x1C, (byte) 0xDF, (byte) 0x91, (byte) 0x1A, (byte) 0xC4, (byte) 0x76, (byte) 0x88, (byte) 0xE4, (byte) 0xE2, (byte) 0x40, (byte) 0x70,
                (byte) 0x70, (byte) 0xCC, (byte) 0x28, (byte) 0x86, (byte) 0xE4, (byte) 0xC1, (byte) 0x11, (byte) 0x25, (byte) 0x8C, (byte) 0xE4, (byte) 0xA8, (byte) 0xF4, (byte) 0xBE, (byte) 0xEC, (byte) 0xE2, (byte) 0xDA,
                (byte) 0x7A, (byte) 0x62, (byte) 0xC4, (byte) 0xBE, (byte) 0x57, (byte) 0x4D, (byte) 0xFB, (byte) 0xF3, (byte) 0xE1, (byte) 0x72, (byte) 0x3C, (byte) 0x38, (byte) 0x10, (byte) 0x10, (byte) 0xE5, (byte) 0x80,
                (byte) 0x48, (byte) 0x06, (byte) 0x96, (byte) 0xF8, (byte) 0x3E, (byte) 0x44, (byte) 0x16, (byte) 0xB8, (byte) 0x76, (byte) 0x28, (byte) 0xDA, (byte) 0x2C, (byte) 0x0A, (byte) 0x5C, (byte) 0xF8, (byte) 0x3E,
                (byte) 0x66, (byte) 0x5C, (byte) 0x1A, (byte) 0xC2, (byte) 0xB2, (byte) 0x60, (byte) 0xAA, (byte) 0xE0, (byte) 0x72, (byte) 0xB2, (byte) 0xD5, (byte) 0x07, (byte) 0x87, (byte) 0x8F, (byte) 0xCD, (byte) 0x3B,
                (byte) 0xF0, (byte) 0xAC, (byte) 0x5E, (byte) 0x42, (byte) 0x6C, (byte) 0x09, (byte) 0x21, (byte) 0xB8, (byte) 0xB4, (byte) 0x90, (byte) 0x96, (byte) 0x74, (byte) 0x5A, (byte) 0x56, (byte) 0x5C, (byte) 0xA0,
                (byte) 0x82, (byte) 0x12, (byte) 0xE2, (byte) 0xEC, (byte) 0xF0, (byte) 0xC8, (byte) 0x30, (byte) 0xDC, (byte) 0xEC, (byte) 0x3A, (byte) 0xD2, (byte) 0xCC, (byte) 0x9E, (byte) 0x8E, (byte) 0xD2, (byte) 0x96,
                (byte) 0x48, (byte) 0x81, (byte) 0xE7, (byte) 0xE7, (byte) 0xC1, (byte) 0x5F, (byte) 0xCE, (byte) 0xF2, (byte) 0x36, (byte) 0x22, (byte) 0xF2, (byte) 0x50, (byte) 0x6A, (byte) 0xF7, (byte) 0x5F, (byte) 0x2A,
                (byte) 0x9C, (byte) 0x22, (byte) 0x82, (byte) 0xDA, (byte) 0x5C, (byte) 0x20, (byte) 0x26, (byte) 0x18, (byte) 0x6C, (byte) 0x0E, (byte) 0x26, (byte) 0xF4, (byte) 0x4C, (byte) 0x38, (byte) 0x48, (byte) 0x7A,
                (byte) 0x42, (byte) 0x96, (byte) 0x3C, (byte) 0xBE, (byte) 0xAA, (byte) 0x38, (byte) 0x60, (byte) 0x6A, (byte) 0xDD, (byte) 0x41, (byte) 0xE3, (byte) 0xF1, (byte) 0x7B, (byte) 0xA4, (byte) 0x46, (byte) 0x80,
                (byte) 0xC2, (byte) 0xF6, (byte) 0x86, (byte) 0x14, (byte) 0x2F, (byte) 0xDB, (byte) 0x74, (byte) 0x68, (byte) 0x86, (byte) 0x28, (byte) 0x9E, (byte) 0x30, (byte) 0xE2, (byte) 0x80, (byte) 0x82, (byte) 0x9E,
                (byte) 0x20, (byte) 0xAC, (byte) 0x0E, (byte) 0x6E, (byte) 0x42, (byte) 0xC0, (byte) 0x00, (byte) 0x78, (byte) 0x00, (byte) 0xF2, (byte) 0x0E, (byte) 0xF6, (byte) 0x0A, (byte) 0x87, (byte) 0x55, (byte) 0xD3,
                (byte) 0xCF, (byte) 0xF9, (byte) 0x27, (byte) 0x8E, (byte) 0x76, (byte) 0xC8, (byte) 0x1A, (byte) 0x8A, (byte) 0x5C, (byte) 0x3E, (byte) 0xEA, (byte) 0x51, (byte) 0x6D, (byte) 0xD5, (byte) 0x94, (byte) 0xB6,
                (byte) 0x36, (byte) 0x8A, (byte) 0x0C, (byte) 0x4A, (byte) 0x1C, (byte) 0x70, (byte) 0xC8, (byte) 0x76, (byte) 0x64, (byte) 0x6A, (byte) 0x5A, (byte) 0x80, (byte) 0x4E, (byte) 0xFC, (byte) 0x60, (byte) 0x72,
                (byte) 0xFA, (byte) 0x8C, (byte) 0xBC, (byte) 0x3A, (byte) 0xA9, (byte) 0x3D, (byte) 0x35, (byte) 0x63, (byte) 0x07, (byte) 0x37, (byte) 0x6E, (byte) 0xFA, (byte) 0xAE, (byte) 0x8C, (byte) 0xA8, (byte) 0x6C,
                (byte) 0x9A, (byte) 0x38, (byte) 0x84, (byte) 0x2B, (byte) 0x17, (byte) 0x32, (byte) 0xEC, (byte) 0x58, (byte) 0xF6, (byte) 0xD4, (byte) 0xF4, (byte) 0x78, (byte) 0x94, (byte) 0x55, (byte) 0xBD, (byte) 0x8E,
                (byte) 0xDA, (byte) 0xE2, (byte) 0x42, (byte) 0x06, (byte) 0x34, (byte) 0xCA, (byte) 0x12, (byte) 0x1A, (byte) 0xF8, (byte) 0xDC, (byte) 0x39, (byte) 0x9B, (byte) 0xE7, (byte) 0xEF, (byte) 0x09, (byte) 0xF3,
                (byte) 0x04, (byte) 0x52, (byte) 0xFA, (byte) 0xA2, (byte) 0x4E, (byte) 0x3A, (byte) 0x9C, (byte) 0xEE, (byte) 0xCC, (byte) 0x78, (byte) 0xC7, (byte) 0xA3, (byte) 0xB8, (byte) 0x6C, (byte) 0x7A, (byte) 0xAE,
                (byte) 0xAE, (byte) 0xCE, (byte) 0xCE, (byte) 0x84, (byte) 0x24, (byte) 0x55, (byte) 0xDB, (byte) 0xC8, (byte) 0xF0, (byte) 0x52, (byte) 0xC8, (byte) 0x7E, (byte) 0x76, (byte) 0xCC, (byte) 0xB0, (byte) 0xFE,
                (byte) 0x13, (byte) 0xF9, (byte) 0x7B, (byte) 0x25, (byte) 0xDB, (byte) 0xB2, (byte) 0x8A, (byte) 0xDE, (byte) 0x78, (byte) 0xB6, (byte) 0x1A, (byte) 0x94, (byte) 0xB4, (byte) 0xF0, (byte) 0x44, (byte) 0xEC,
                (byte) 0x82, (byte) 0xD3, (byte) 0x31, (byte) 0xE7, (byte) 0x3A, (byte) 0xAA, (byte) 0x4E, (byte) 0xAA, (byte) 0xEA, (byte) 0x7C, (byte) 0x3C, (byte) 0xD4, (byte) 0x90, (byte) 0xCF, (byte) 0x5F, (byte) 0x04,
                (byte) 0xBE, (byte) 0x90, (byte) 0x0A, (byte) 0xDA, (byte) 0x7A, (byte) 0x7C, (byte) 0xA2, (byte) 0xCC, (byte) 0x57, (byte) 0x2B, (byte) 0x43, (byte) 0x1B, (byte) 0x88, (byte) 0xC2, (byte) 0x68, (byte) 0x94,
                (byte) 0x98, (byte) 0x2A, (byte) 0x7A, (byte) 0xAA, (byte) 0xA8, (byte) 0x3A, (byte) 0xA8, (byte) 0x1E, (byte) 0xE1, (byte) 0x51, (byte) 0xAB, (byte) 0xB9, (byte) 0xF0, (byte) 0xE2, (byte) 0xB2, (byte) 0x8C,
                (byte) 0xD4, (byte) 0x5C, (byte) 0x52, (byte) 0x8A, (byte) 0x72, (byte) 0x8B, (byte) 0x17, (byte) 0x98, (byte) 0x88, (byte) 0x1C, (byte) 0x16, (byte) 0xAA, (byte) 0xAA, (byte) 0xC2, (byte) 0xE6, (byte) 0x56,
                (byte) 0x33, (byte) 0xBF, (byte) 0xBF, (byte) 0xD8, (byte) 0x12, (byte) 0x52, (byte) 0x30, (byte) 0xB2, (byte) 0x16, (byte) 0x16, (byte) 0xCC, (byte) 0x08, (byte) 0xAA, (byte) 0xE2, (byte) 0xB6, (byte) 0x0D,
                (byte) 0xE9, (byte) 0x19, (byte) 0xDD, (byte) 0x39, (byte) 0xD1, (byte) 0x46, (byte) 0xC4, (byte) 0x20, (byte) 0xD8, (byte) 0x22, (byte) 0x84, (byte) 0xB8, (byte) 0x28, (byte) 0xFB, (byte) 0xC9, (byte) 0x4E,
                (byte) 0x5E, (byte) 0x2A, (byte) 0x76, (byte) 0xEA, (byte) 0xBA, (byte) 0x16, (byte) 0x4C, (byte) 0x60, (byte) 0xF6, (byte) 0xD8, (byte) 0x9B, (byte) 0x45, (byte) 0x5B, (byte) 0xA4, (byte) 0xA4, (byte) 0x4E,
                (byte) 0x74, (byte) 0x14, (byte) 0xBE, (byte) 0xBC, (byte) 0x9A, (byte) 0x3E, (byte) 0x0F, (byte) 0x55, (byte) 0xE9, (byte) 0x2C, (byte) 0x38, (byte) 0x43, (byte) 0x29, (byte) 0x35, (byte) 0xB8, (byte) 0x96,
                (byte) 0x86, (byte) 0x0E, (byte) 0x94, (byte) 0xCC, (byte) 0x8C, (byte) 0x7F, (byte) 0xA7, (byte) 0xF4, (byte) 0x54, (byte) 0xFE, (byte) 0x00, (byte) 0x64, (byte) 0xF6, (byte) 0xA2, (byte) 0x24, (byte) 0x44,
                (byte) 0x5E, (byte) 0x1E, (byte) 0xA1, (byte) 0x5F, (byte) 0xD7, (byte) 0x27, (byte) 0x47, (byte) 0x4D, (byte) 0x6F, (byte) 0xA3, (byte) 0xA1, (byte) 0x03, (byte) 0xC5, (byte) 0xC7, (byte) 0xF7, (byte) 0xAE,
                (byte) 0xFA, (byte) 0xEC, (byte) 0x07, (byte) 0xEB, (byte) 0xEF, (byte) 0xAD, (byte) 0xBB, (byte) 0xD2, (byte) 0x30, (byte) 0x54, (byte) 0x8E, (byte) 0xF0, (byte) 0xC5, (byte) 0xFB, (byte) 0xF0, (byte) 0x36,
                (byte) 0x8C, (byte) 0x1C, (byte) 0xBC, (byte) 0x8C, (byte) 0xE6, (byte) 0x84, (byte) 0xB8, (byte) 0xDA, (byte) 0x74, (byte) 0x82, (byte) 0xC0, (byte) 0x0E, (byte) 0x24, (byte) 0xD9, (byte) 0x17, (byte) 0xB9,
                (byte) 0xDB, (byte) 0x85, (byte) 0xC7, (byte) 0x79, (byte) 0x43, (byte) 0x3E, (byte) 0x0E, (byte) 0x32, (byte) 0xD4, (byte) 0x72, (byte) 0x43, (byte) 0x79, (byte) 0xA5, (byte) 0xF3, (byte) 0x33, (byte) 0x2D,
                (byte) 0xE4, (byte) 0xD8, (byte) 0x18, (byte) 0x4C, (byte) 0x17, (byte) 0x4B, (byte) 0xE6, (byte) 0x82, (byte) 0xDE, (byte) 0x30, (byte) 0x82, (byte) 0x12, (byte) 0xBA, (byte) 0xEA, (byte) 0x82, (byte) 0xE6,
                (byte) 0x92, (byte) 0x78, (byte) 0x8E, (byte) 0xC8, (byte) 0x44, (byte) 0x6E, (byte) 0x04, (byte) 0xD0, (byte) 0x46, (byte) 0xE6, (byte) 0x8C, (byte) 0x80, (byte) 0xCE, (byte) 0x40, (byte) 0xE4, (byte) 0x50,
                (byte) 0x02, (byte) 0x6A, (byte) 0xBB, (byte) 0xCF, (byte) 0x04, (byte) 0xAF, (byte) 0x6B, (byte) 0x9D, (byte) 0xC2, (byte) 0x9C, (byte) 0xF2, (byte) 0x52, (byte) 0x65, (byte) 0x45, (byte) 0xE4, (byte) 0x9A,
                (byte) 0xFC, (byte) 0xC0, (byte) 0xEA, (byte) 0xBA, (byte) 0x66, (byte) 0xBA, (byte) 0x5C, (byte) 0xE8, (byte) 0x5E, (byte) 0x0A, (byte) 0xFA, (byte) 0xC2, (byte) 0x32, (byte) 0xDE, (byte) 0xC2, (byte) 0x54,
                (byte) 0x3E, (byte) 0xBA, (byte) 0xBC, (byte) 0x9E, (byte) 0x30, (byte) 0x50, (byte) 0x4C, (byte) 0x22, (byte) 0x98, (byte) 0xD3, (byte) 0xF9, (byte) 0x41, (byte) 0xD8, (byte) 0x00, (byte) 0x4D, (byte) 0x2B,
                (byte) 0xAF, (byte) 0xB0, (byte) 0x86, (byte) 0x10, (byte) 0xC7, (byte) 0x3D, (byte) 0x14, (byte) 0x76, (byte) 0xBA, (byte) 0xFC, (byte) 0x9C, (byte) 0x8E, (byte) 0x3E, (byte) 0x72, (byte) 0xA2, (byte) 0x76,
                (byte) 0x28, (byte) 0x6A, (byte) 0xE2, (byte) 0x64, (byte) 0x7E, (byte) 0x34, (byte) 0xF8, (byte) 0x88, (byte) 0x04, (byte) 0x76, (byte) 0xB2, (byte) 0xDA, (byte) 0xAA, (byte) 0xEE, (byte) 0x72, (byte) 0xE2,
                (byte) 0x3C, (byte) 0xBD, (byte) 0xFB, (byte) 0x95, (byte) 0x00, (byte) 0xB4, (byte) 0x5D, (byte) 0x45, (byte) 0xB5, (byte) 0x00, (byte) 0xEE, (byte) 0x1C, (byte) 0x61, (byte) 0x75, (byte) 0x3C, (byte) 0x8A,
                (byte) 0xB8, (byte) 0xF4, (byte) 0x76, (byte) 0x6E, (byte) 0x3A, (byte) 0x8A, (byte) 0xD6, (byte) 0x46, (byte) 0x0C, (byte) 0x04, (byte) 0x96, (byte) 0xEA, (byte) 0x7C, (byte) 0x54, (byte) 0x34, (byte) 0x2C,
                (byte) 0x0C, (byte) 0x8E, (byte) 0x36, (byte) 0xC2, (byte) 0xDC, (byte) 0x16, (byte) 0x80, (byte) 0xC8, (byte) 0x0F, (byte) 0x0D, (byte) 0x0F, (byte) 0xC9, (byte) 0x40, (byte) 0xB4, (byte) 0xFE, (byte) 0x83,
                (byte) 0x7B, (byte) 0xC9, (byte) 0x7D, (byte) 0x63, (byte) 0xF1, (byte) 0x3F, (byte) 0x94, (byte) 0x28, (byte) 0xEC, (byte) 0xB2, (byte) 0x4A, (byte) 0x5A, (byte) 0x68, (byte) 0xE0, (byte) 0x02, (byte) 0x38,
                (byte) 0x5A, (byte) 0x9E, (byte) 0x32, (byte) 0x16, (byte) 0xB8, (byte) 0x58, (byte) 0x28, (byte) 0x7E, (byte) 0x2C, (byte) 0x34, (byte) 0xB2, (byte) 0xC0, (byte) 0xB0, (byte) 0xF6, (byte) 0xF4, (byte) 0x44,
                (byte) 0x53, (byte) 0xC3, (byte) 0x2F, (byte) 0x9A, (byte) 0x7A, (byte) 0xAE, (byte) 0x9A, (byte) 0x26, (byte) 0xC5, (byte) 0xF9, (byte) 0x25, (byte) 0x13, (byte) 0x01, (byte) 0xEB, (byte) 0x82, (byte) 0x82,
                (byte) 0xA4, (byte) 0x72, (byte) 0x6A, (byte) 0xCA, (byte) 0x74, (byte) 0xEA, (byte) 0xDE, (byte) 0xFA, (byte) 0xEA, (byte) 0x34, (byte) 0x68, (byte) 0x9E, (byte) 0x36, (byte) 0xEC, (byte) 0xE6, (byte) 0xE0,
                (byte) 0xB4, (byte) 0xCC, (byte) 0x3E, (byte) 0x44, (byte) 0x1C, (byte) 0xC2, (byte) 0xA2, (byte) 0xAE, (byte) 0x37, (byte) 0xC9, (byte) 0xE3, (byte) 0x68, (byte) 0xE0, (byte) 0x00, (byte) 0x10, (byte) 0xE4,
                (byte) 0x13, (byte) 0x3D, (byte) 0x1B, (byte) 0x71, (byte) 0x97, (byte) 0xDF, (byte) 0xAC, (byte) 0xA8, (byte) 0x20, (byte) 0x8C, (byte) 0xC4, (byte) 0x46, (byte) 0x56, (byte) 0xEE, (byte) 0xE2, (byte) 0x56,
                (byte) 0x66, (byte) 0x66, (byte) 0x06, (byte) 0x6C, (byte) 0x3C, (byte) 0x8E, (byte) 0x0A, (byte) 0xB0, (byte) 0xC0, (byte) 0xCC, (byte) 0x70, (byte) 0xBA, (byte) 0x2A, (byte) 0xCE, (byte) 0x28, (byte) 0xB3,
                (byte) 0xC5, (byte) 0xA3, (byte) 0xE3, (byte) 0xC0, (byte) 0x64, (byte) 0x5C, (byte) 0x74, (byte) 0x9C, (byte) 0xF4, (byte) 0x93, (byte) 0x91, (byte) 0x65, (byte) 0x23, (byte) 0x1F, (byte) 0xAA, (byte) 0x9C,
                (byte) 0xB6, (byte) 0xB4, (byte) 0xC8, (byte) 0x62, (byte) 0x94, (byte) 0x06, (byte) 0x4E, (byte) 0xF8, (byte) 0xAA, (byte) 0xF6, (byte) 0xB5, (byte) 0x63, (byte) 0x2D, (byte) 0x34, (byte) 0xF2, (byte) 0x60,
                (byte) 0x90, (byte) 0x82, (byte) 0x54, (byte) 0x58, (byte) 0x50, (byte) 0x7C, (byte) 0x9A, (byte) 0x1B, (byte) 0x83, (byte) 0xE3, (byte) 0x22, (byte) 0x74, (byte) 0xD8, (byte) 0xB0, (byte) 0xF8, (byte) 0x0C,
                (byte) 0x7C, (byte) 0xAA, (byte) 0x52, (byte) 0xD1, (byte) 0x5F, (byte) 0x2B, (byte) 0x5C, (byte) 0x58, (byte) 0xE8, (byte) 0x0C, (byte) 0x52, (byte) 0x68, (byte) 0xD2, (byte) 0x7A, (byte) 0xF0, (byte) 0xBE,
                (byte) 0x16, (byte) 0xE6, (byte) 0xB3, (byte) 0xDD, (byte) 0x77, (byte) 0x0D, (byte) 0xF1, (byte) 0x55, (byte) 0x43, (byte) 0x01, (byte) 0x0F, (byte) 0x39, (byte) 0xDB, (byte) 0xBD, (byte) 0x6F, (byte) 0xE3,
                (byte) 0x1F, (byte) 0xBB, (byte) 0x1E, (byte) 0x5E, (byte) 0x8A, (byte) 0xA6, (byte) 0xD4, (byte) 0x9A, (byte) 0x7E, (byte) 0x50, (byte) 0x69, (byte) 0x89, (byte) 0x79, (byte) 0x6F, (byte) 0x34, (byte) 0x84,
                (byte) 0xD2, (byte) 0xB8, (byte) 0xD8, (byte) 0x64, (byte) 0x4C, (byte) 0x5E, (byte) 0x28, (byte) 0x9E, (byte) 0xF4, (byte) 0x76, (byte) 0x42, (byte) 0xE8, (byte) 0x71, (byte) 0x7D, (byte) 0x13, (byte) 0x4F,
                (byte) 0x9F, (byte) 0xD5, (byte) 0xF3, (byte) 0x43, (byte) 0xF7, (byte) 0xFD, (byte) 0xD3, (byte) 0xEF, (byte) 0xAD, (byte) 0x5C, (byte) 0x24, (byte) 0xAC, (byte) 0xBE, (byte) 0x22, (byte) 0x2E, (byte) 0x86,
                (byte) 0xA8, (byte) 0xE0, (byte) 0x9F, (byte) 0x17, (byte) 0x8F, (byte) 0x16, (byte) 0xC6, (byte) 0x20, (byte) 0x0E, (byte) 0x6A, (byte) 0x86, (byte) 0x90, (byte) 0x06, (byte) 0x9A, (byte) 0xCC, (byte) 0x72,
                (byte) 0x3C, (byte) 0x14, (byte) 0xAC, (byte) 0xCE, (byte) 0xAE, (byte) 0x02, (byte) 0xD4, (byte) 0xE4, (byte) 0xFC, (byte) 0x33, (byte) 0xB1, (byte) 0x83, (byte) 0xE1, (byte) 0x5B, (byte) 0xDF, (byte) 0x11,
                (byte) 0xC1, (byte) 0xFA, (byte) 0xAC, (byte) 0x9E, (byte) 0x78, (byte) 0x8C, (byte) 0xB8, (byte) 0xCA, (byte) 0x80, (byte) 0x56, (byte) 0x9B, (byte) 0x7B, (byte) 0x29, (byte) 0xD0, (byte) 0xFA, (byte) 0xB8,
                (byte) 0x86, (byte) 0xA2, (byte) 0x98, (byte) 0x56, (byte) 0x5A, (byte) 0x26, (byte) 0x76, (byte) 0xE4, (byte) 0x50, (byte) 0xC8, (byte) 0x3A, (byte) 0xB2, (byte) 0x7E, (byte) 0xC2, (byte) 0xE0, (byte) 0x40,
                (byte) 0xFA, (byte) 0x0C, (byte) 0x04, (byte) 0x06, (byte) 0xEC, (byte) 0x5A, (byte) 0xB3, (byte) 0x9D, (byte) 0x0B, (byte) 0xA0, (byte) 0x6C, (byte) 0x4E, (byte) 0xAE, (byte) 0xAE, (byte) 0xD4, (byte) 0x76,
                (byte) 0x58, (byte) 0xEA, (byte) 0x1C, (byte) 0xDC, (byte) 0xD2, (byte) 0xD2, (byte) 0x38, (byte) 0x34, (byte) 0x0A, (byte) 0x86, (byte) 0x02, (byte) 0x06, (byte) 0x0E, (byte) 0x5A, (byte) 0xDC, (byte) 0x86,
                (byte) 0xFE, (byte) 0x7C, (byte) 0x0A, (byte) 0xE6, (byte) 0x96, (byte) 0xE0, (byte) 0xDA, (byte) 0x44, (byte) 0x9C, (byte) 0x66, (byte) 0xF4, (byte) 0x4A, (byte) 0x8C, (byte) 0x94, (byte) 0xBC, (byte) 0x14,
                (byte) 0xA4, (byte) 0x6E, (byte) 0xA2, (byte) 0xA6, (byte) 0x82, (byte) 0x66, (byte) 0xB2, (byte) 0x76, (byte) 0x3C, (byte) 0xA4, (byte) 0xEC, (byte) 0x0E, (byte) 0x66, (byte) 0x5A, (byte) 0x46, (byte) 0x10,
                (byte) 0x3C, (byte) 0x8E, (byte) 0xD2, (byte) 0x62, (byte) 0xAC, (byte) 0xCE, (byte) 0xC8, (byte) 0x6C, (byte) 0xD4, (byte) 0xB6, (byte) 0x3C, (byte) 0x16, (byte) 0x78, (byte) 0xBA, (byte) 0xC8, (byte) 0x12,
                (byte) 0xBE, (byte) 0x0A, (byte) 0x6A, (byte) 0x86, (byte) 0xB2, (byte) 0x98, (byte) 0xB8, (byte) 0x7E, (byte) 0xF0, (byte) 0x68, (byte) 0x3C, (byte) 0x64, (byte) 0x7A, (byte) 0x6E, (byte) 0x06, (byte) 0xD4,
                (byte) 0x1E, (byte) 0x24, (byte) 0x40, (byte) 0xD4, (byte) 0xCC, (byte) 0xEA, (byte) 0x88, (byte) 0x78, (byte) 0xE4, (byte) 0x24, (byte) 0xC6, (byte) 0x00, (byte) 0xA2, (byte) 0x26, (byte) 0x38, (byte) 0x00,
                (byte) 0x60, (byte) 0x0C, (byte) 0xFA, (byte) 0x00, (byte) 0x80, (byte) 0xAE, (byte) 0x2E, (byte) 0xC4, (byte) 0x7A, (byte) 0x62, (byte) 0xA2, (byte) 0xB4, (byte) 0xD8, (byte) 0x1E, (byte) 0xA4, (byte) 0x34,
                (byte) 0x9C, (byte) 0xE4, (byte) 0x5A, (byte) 0xA0, (byte) 0x62, (byte) 0x58, (byte) 0x6E, (byte) 0xA8, (byte) 0x8E, (byte) 0x9E, (byte) 0x88, (byte) 0xC6, (byte) 0x32, (byte) 0xBA, (byte) 0x5E, (byte) 0xC2,
                (byte) 0xCC, (byte) 0xA2, (byte) 0x38, (byte) 0x06, (byte) 0x04, (byte) 0xF2, (byte) 0xCE, (byte) 0x76, (byte) 0x4E, (byte) 0x70, (byte) 0xB6, (byte) 0x2C, (byte) 0x5C, (byte) 0xA8, (byte) 0xDA, (byte) 0xBE,
                (byte) 0x40, (byte) 0x38, (byte) 0xEE, (byte) 0x3A, (byte) 0x20, (byte) 0x9C, (byte) 0xA0, (byte) 0xDA, (byte) 0x1C, (byte) 0x14, (byte) 0xD2, (byte) 0x78, (byte) 0x70, (byte) 0xB8, (byte) 0x54, (byte) 0x3C,
                (byte) 0xD2, (byte) 0x94, (byte) 0xB0, (byte) 0xEE, (byte) 0xC8, (byte) 0x5E, (byte) 0xA4, (byte) 0xEC, (byte) 0x2A, (byte) 0xD2, (byte) 0x4A, (byte) 0x54, (byte) 0x4E, (byte) 0x2A, (byte) 0xA8, (byte) 0x64,
                (byte) 0x8A, (byte) 0xA6, (byte) 0x9A, (byte) 0x66, (byte) 0xF0, (byte) 0xC4, (byte) 0x2C, (byte) 0x4E, (byte) 0x0E, (byte) 0x6A, (byte) 0xDA, (byte) 0x78, (byte) 0x10, (byte) 0x46, (byte) 0x34, (byte) 0xE6,
                (byte) 0xEC, (byte) 0x50, (byte) 0x42, (byte) 0xDC, (byte) 0x4A, (byte) 0xFC, (byte) 0xB0, (byte) 0xBA, (byte) 0x4A, (byte) 0x9C, (byte) 0x38, (byte) 0x0C, (byte) 0x1E, (byte) 0xE4, (byte) 0xFA, (byte) 0x2A,
                (byte) 0x74, (byte) 0xE0, (byte) 0x22, (byte) 0x06, (byte) 0x32, (byte) 0x70, (byte) 0x56, (byte) 0x06, (byte) 0x04, (byte) 0xBE, (byte) 0xB0, (byte) 0xB8, (byte) 0x78, (byte) 0x48, (byte) 0x9E, (byte) 0x36,
                (byte) 0x5C, (byte) 0x8E, (byte) 0x96, (byte) 0xC6, (byte) 0xF8, (byte) 0xEC, (byte) 0x68, (byte) 0x9E, (byte) 0x5A, (byte) 0xDA, (byte) 0x34, (byte) 0x0A, (byte) 0xA2, (byte) 0x02, (byte) 0xD0, (byte) 0xA0,
                (byte) 0x76, (byte) 0x88, (byte) 0xB8, (byte) 0x2A, (byte) 0xBE, (byte) 0x0A, (byte) 0x9E, (byte) 0xF8, (byte) 0xA4, (byte) 0x7A, (byte) 0x92, (byte) 0xD0, (byte) 0xA6, (byte) 0x94, (byte) 0x98, (byte) 0x70,
                (byte) 0x0A, (byte) 0x6A, (byte) 0xA4, (byte) 0xA8, (byte) 0x4C, (byte) 0x92, (byte) 0x32, (byte) 0x6E, (byte) 0xAA, (byte) 0x2E, (byte) 0x6A, (byte) 0x84, (byte) 0xBE, (byte) 0x40, (byte) 0xB2, (byte) 0xB4,
                (byte) 0xFA, (byte) 0x3C, (byte) 0xB4, (byte) 0x18, (byte) 0x66, (byte) 0x8E, (byte) 0xC2, (byte) 0xD4, (byte) 0x2E, (byte) 0xC8, (byte) 0xC0, (byte) 0x62, (byte) 0xAC, (byte) 0x6C, (byte) 0x7E, (byte) 0x90,
                (byte) 0x94, (byte) 0xD6, (byte) 0xCA, (byte) 0x6E, (byte) 0x44, (byte) 0xCA, (byte) 0xD8, (byte) 0x0C, (byte) 0x98, (byte) 0xAC, (byte) 0xE0, (byte) 0x9A, (byte) 0x50, (byte) 0x1A, (byte) 0xC6, (byte) 0x0E,
                (byte) 0x26, (byte) 0x12, (byte) 0xD0, (byte) 0x0C, (byte) 0xD4, (byte) 0xB6, (byte) 0xBA, (byte) 0xA8, (byte) 0xD4, (byte) 0x54, (byte) 0xCA, (byte) 0x38, (byte) 0xD8, (byte) 0xC8, (byte) 0x76, (byte) 0x1E,
                (byte) 0x42, (byte) 0xA0, (byte) 0xF6, (byte) 0x58, (byte) 0xB4, (byte) 0x52, (byte) 0xB6, (byte) 0xD4, (byte) 0xA6, (byte) 0x18, (byte) 0x18, (byte) 0x18, (byte) 0x96, (byte) 0xA6, (byte) 0xC2, (byte) 0xFE,
                (byte) 0x00, (byte) 0xB4, (byte) 0x72, (byte) 0xFE, (byte) 0x82, (byte) 0xEE, (byte) 0xB8, (byte) 0x4A, (byte) 0xC8, (byte) 0x9E, (byte) 0x64, (byte) 0xCC, (byte) 0xBA, (byte) 0xA6, (byte) 0x3C, (byte) 0xE4,
                (byte) 0x64, (byte) 0xB8, (byte) 0xA6, (byte) 0xE0, (byte) 0x12, (byte) 0x68, (byte) 0x86, (byte) 0x6C, (byte) 0x4C, (byte) 0x16, (byte) 0x86, (byte) 0xB2, (byte) 0xFA, (byte) 0xA6, (byte) 0xFA, (byte) 0xE8
        };

        dataList.add(NeiraPrinterFunction.PrintRasterBitImageData(0x28,0x50, image));

        //Get Receipt Size
        int size= 0;
        for (byte[] data:
             dataList ) {
            size = size + data.length;
        }

        //Prepare Receipt
        byte[] dataToSend = new byte[size];
        size = 0;
        for (byte[] data:
                dataList ) {
            System.arraycopy(data,0,dataToSend,size,data.length);
            size = size + data.length;
        }

        sendToBluetooth(dataToSend);
    }

    public void sendReceipt2(){
        String tmp = "\n  PARKIR PRASI-MALL\n";
        List<byte[]> dataList = new ArrayList<>();

        //Set Up Merchant Name [Header]
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetBeepTimes(1,1));
        dataList.add(NeiraPrinterFunction.SetFontSize(3));
        dataList.add(NeiraPrinterFunction.SetBold(true));
        dataList.add(tmp.getBytes());

        //Set Up Merchant Address [Header]
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(2));
        dataList.add(NeiraPrinterFunction.SetLineSpacing(10));
        tmp = "JL. Margonda 494D, Depok\n\n\n";
        dataList.add(tmp.getBytes());

        //Set Up Horizontal Line
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(1));
        dataList.add(NeiraPrinterFunction.SetBold(true));
        tmp = "------------------------------------------------";
        dataList.add(tmp.getBytes());

        //Set Up Receipt Details
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(1));
        dataList.add(NeiraPrinterFunction.SetLineSpacing(20));
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.LONG);
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
        Date dt = new Date(System.currentTimeMillis());
        Random rand = new Random();

        tmp = "\n" + df.format(dt) + "|" +  tf.format(dt) + "|No." + Integer.toString(rand.nextInt(999999998)+1) + "\n";
        dataList.add(tmp.getBytes());

        //Set Up Horizontal Line
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(1));
        dataList.add(NeiraPrinterFunction.SetBold(true));
        dataList.add(NeiraPrinterFunction.SetLineSpacing(10));
        tmp = "------------------------------------------------";
        dataList.add(tmp.getBytes());

        //Set Up Receipt Contents
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(2));
        dataList.add(NeiraPrinterFunction.SetLineSpacing(10));
        tmp =   "   Silahkan menuju parkir LOT nomor :\n ";
        dataList.add(tmp.getBytes());

        tmp =   "B" + Integer.toString(rand.nextInt(4)+1) + "/" + Integer.toString(rand.nextInt(400)+1);

        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetBarcodeHRIPosition(1));
        dataList.add(NeiraPrinterFunction.SetBarcodeHeight(10));
        dataList.add(NeiraPrinterFunction.PrintBarcode(4, tmp.getBytes()));

        //Set Up Footer
        dataList.add(NeiraPrinterFunction.PrinterInit());
        dataList.add(NeiraPrinterFunction.SetFontSize(1));
        dataList.add(NeiraPrinterFunction.SetLineSpacing(40));
        dataList.add(NeiraPrinterFunction.SetFontType(49)); //Set to Italic
        tmp =   "\n\n    JANGAN MENINGGALKAN BARANG BERHARGA DIKENDARAAN ANDA\n" +
                "    KEHILANGAN BUKAN MERUPAKAN TANGGUNG JAWAB KAMI\n\n\n\n";
        dataList.add(tmp.getBytes());

        dataList.add(NeiraPrinterFunction.SetBeepTimes(3,1));

        //Get Receipt Size
        int size= 0;
        for (byte[] data:
                dataList ) {
            size = size + data.length;
        }

        //Prepare Receipt
        byte[] dataToSend = new byte[size];
        size = 0;
        for (byte[] data:
                dataList ) {
            System.arraycopy(data,0,dataToSend,size,data.length);
            size = size + data.length;
        }

        sendToBluetooth(dataToSend);
    }

    public synchronized void sendButton(View v){
        EditText et = (EditText) findViewById(R.id.editText);
        String messageToSend =  et.getText().toString();

        displayInfoMessage(messageToSend);

        /* dibuang sayang */

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            btCtrl.addByteToWrite(messageToSend.getBytes(StandardCharsets.UTF_8));
        }else{
            btCtrl.addByteToWrite(messageToSend.getBytes());
        }

        Message messageT = new Message();
        messageT.what = BluetoothController.BT_DATAWRITE;
        messageT.arg1 = 77;
        messageT.arg2 = 77;
        messageHandler.sendMessage(messageT);

    }

    private void prepareTerminalMessage(){
        BaseMessage message = new BaseMessage();
        message.terminal = neiraTerminal;
        message.createdAt = System.currentTimeMillis();
        message.message = "Hello Welcome to Neira Application Sample ..";
        messageList.add(message);

        message = new BaseMessage();
        message.terminal = neiraTerminal;
        message.createdAt = System.currentTimeMillis();
        message.message = "Go Ahead to try some feature in this Application";
        messageList.add(message);

        mMessageAdapter.notifyDataSetChanged();

//        byte[] data = new byte[256];
//
//        for (int i = 0; i<256; i++){
//            data[i] = (byte) i;
//        }
//
//        enableHexConversion = true;
//        displayInfoMessage(new String(data), data);
    }

    @Override
    public void onAcceptedDevice(BluetoothDevice device) {
        DialogFragment dialog = (DialogFragment) getSupportFragmentManager().findFragmentByTag("BluetoothDialogFragment");
        dialog.dismiss();

        Message message = new Message();
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothController.INFO_KEY, "Trying to establish connection with "+device.getName()+".");
        message.setData(bundle);
        messageHandler.handleMessage(message);
        mDevice = device;

        btCtrl.startService(device);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.dismiss();
    }

    public void showBluetoothDialog(){
        BluetoothDeviceListDialog bl = (BluetoothDeviceListDialog) bluetoothDialog;
        bl.newListDevice(NeiraBluetooth.getPairedDevice());
        bluetoothDialog.show(getSupportFragmentManager(), "BluetoothDialogFragment");
    }

    private void openGroupMenuItem(NavigationView nav, int menuItem){
        closeAllMenuDrawer();
        switch (menuItem){
            case R.id.nav_printer_function:
                nav.getMenu().setGroupVisible(R.id.nav_group_printer_functions,true);
                break;
            case R.id.nav_sample_receipt:
                nav.getMenu().setGroupVisible(R.id.nav_group_sample_receipt,true);
                break;
        }
    }

    private void closeAllMenuDrawer(){
        NavigationView nav = findViewById(R.id.nav_view);
        nav.getMenu().setGroupVisible(R.id.nav_group_sample_receipt, false);
        nav.getMenu().setGroupVisible(R.id.nav_group_printer_functions, false);
    }

    private boolean sendToBluetooth(byte[] data){
        boolean result = false;
        if(!btCtrl.isThreadActive()) return result;

        if(!btCtrl.addByteToWrite(data)) return result;

        Message messageT = new Message();
        messageT.what = BluetoothController.BT_DATAWRITE;
        messageT.arg1 = 77;
        messageT.arg2 = 77;
        result = messageHandler.sendMessage(messageT);

        displayInfoMessage(new String(data), data);

        return result;
    }

    @Override
    public void onDialogExit(DialogFragment dialog, boolean isAccepted) {
        if(!isAccepted) return;

        InputFunctionDialog fragment = (InputFunctionDialog) dialog;

        Bundle bundle = fragment.getBundle();
        byte[] dataToSend={};

        switch (bundle.getInt(fragment.SENDER_ID_KEY)){

            case R.id.nav_pf_PrintAndFeedPaper:
                dataToSend = NeiraPrinterFunction.PrintAndFeedPaper(fragment.number1);
                break;
            case R.id.nav_pf_SetBeepTimes:
                dataToSend = NeiraPrinterFunction.SetBeepTimes(fragment.number1, fragment.number2);
                break;
            case R.id.nav_pf_SetLeftMargin:
                dataToSend = NeiraPrinterFunction.SetLeftMargin(fragment.number1);
                break;
            case R.id.nav_pf_SetPrintWidth:
                dataToSend = NeiraPrinterFunction.SetPrintWidth(fragment.number1);
                break;
            case R.id.nav_pf_SetLineSpacing:
                dataToSend = NeiraPrinterFunction.SetLineSpacing(fragment.number1);
                break;
            case R.id.nav_pf_SetBold:
                dataToSend = NeiraPrinterFunction.SetBold(fragment.number1>0?true:false);
                break;
//            case R.id.nav_pf_SetFontSize:
//                dataToSend = NeiraPrinterFunction.SetFontSize(fragment.number1);
//                break;
            case R.id.nav_pf_SetPrinterSpeed:
                dataToSend = NeiraPrinterFunction.ChangePrintSpeed(fragment.number1);
                break;
            case R.id.nav_custombyte:
                dataToSend = hexStringToByteArray(fragment.hexData);
                break;
            case R.id.nav_pf_SetQRCode:
                byte[] data = NeiraPrinterFunction.StoreQRCodeData(fragment.hexData);
                byte[] cmd = NeiraPrinterFunction.PrintQRCode();
                dataToSend = new byte[data.length+cmd.length];
                System.arraycopy(cmd,0,dataToSend,0, cmd.length);
                System.arraycopy(data,0,dataToSend,cmd.length,data.length);
                break;
                default:
                    //Unreachable Code
                    return;
        }
        displayInfoMessage(new String(dataToSend), dataToSend);
        sendToBluetooth(dataToSend);
    }

    boolean enableHexConversion = false;
    public void displayInfoMessage(String data){
        BaseMessage message = new BaseMessage();
        message.terminal = MessageListAdapter.defaultSender;
        message.createdAt = System.currentTimeMillis();
        message.message = data;
        messageList.add(message);
        mMessageAdapter.notifyDataSetChanged();
        mMessageRecycler.smoothScrollToPosition(mMessageAdapter.getItemCount()-1);
    }

    public void displayInfoMessage(String data, byte[] dataHex){
        displayInfoMessage(data);
        if(enableHexConversion) {
            displayInfoMessage("In HEX:\r\n" +bytesToHex(dataHex));
        }
    }

    public void displayTerminalMessage(String terminalName, String data){
        BaseMessage message = new BaseMessage();
        message.terminal = terminalName;
        message.createdAt = System.currentTimeMillis();
        message.message = data;
        messageList.add(message);

        mMessageAdapter.notifyDataSetChanged();
        mMessageRecycler.smoothScrollToPosition(mMessageAdapter.getItemCount()-1);
    }

    public void displayTerminalMessage(String terminalName, String data, byte[] dataHex){
        displayTerminalMessage(terminalName,data);

        if(enableHexConversion && !terminalName.equals(neiraTerminal)) {
            displayTerminalMessage(terminalName + " In Hex:", "In HEX:\r\n" +bytesToHex(dataHex));
        }
    }

    //https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    // https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
