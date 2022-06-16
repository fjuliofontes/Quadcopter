package com.example.juliofontes.quadcopterremotecontrol;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.content.res.Configuration;
import android.widget.Toast;

import static com.example.juliofontes.quadcopterremotecontrol.JoystickView.scale;


public class RemoteControl extends AppCompatActivity implements JoystickView.JoystickListener {

    /* Global variables */
    private static final int ENABLE_BT_REQUEST_CODE = 1;
    private static final int INIT_OF_FRAME = 0xAABB; // 2 bytes
    private static final int END_OF_FRAME = 0xCCDD; // 2 bytes
    private static final int KEEP_ALIVE_TIMEOUT = 1000; // 1000 ms
    private static final int BTL_SEND_PERIODICITY = 100; // 100 ms
    private static final int BTL_READ_PERIODICITY = 1000; // 1000 ms
    private static final int SUB_ITEM_BASEID = 0xAB00;
    private static final IntentFilter filter = new IntentFilter();
            //IntentFilter(BluetoothDevice.ACTION_FOUND);
    private static boolean bluetoothActivationStatus = true;
    private static long blt_last_send = 0;

    /* Bluetooth related variables */
    List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    BluetoothAdapter bltAdapter;
    BluetoothSocket mmSocket = null;
    BluetoothDevice mmDevice = null;
    OutputStream mmOutputStream = null;
    InputStream mmInputStream = null;

    /*Threads related*/
    Thread t1,t2,t3;

    /* Menu Related Variables */
    Menu optionsMenu = null;
    SubMenu sMenu = null;

    /* Connection Status flag
        false - connection not established
        true - connection established
     */
    boolean connectionStatus = false;


    boolean isSmooth = false;

    boolean isKeepAlive = false;

    int ch1val = 0, ch2val = 0, ch3val = 0, ch4val = 0; // All the values of the channel

    /* initBt
     *
     * Brief: This function, will perform the following operations:
     *     1- check if bluetooth module exits
     *     2- check if bluetooth module is turned on
     *     3- ask to turn it on in case off turned off
     *     4- start discovering devices
     *
     */
    public void initBt(BluetoothAdapter bluetoothAdapter) {
        // 1 - check the bluetoothAdapter code, to analyse if the bluetooth module exists in the current device
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2 - if the module exits, proceed and check if the module is enabled, otherwise ask for permission to enable
        else {
            // ask to enable bluetooth
            if (!bluetoothAdapter.isEnabled()) {
                try {
                    Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBluetoothIntent, ENABLE_BT_REQUEST_CODE);
                } catch (Exception e) {
                }

            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth enabled.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /* discoverDevices
     *
     * Brief: This function, will start/stop the discovery process
     *    1 - check if is already discovering
     *    2 - act accordingly
     *    3 - update bluetooth icon
     */
    public void discoverDevices(BluetoothAdapter bluetoothAdapter) {
        // If the app is discovering stop , otherwise start
        if (bluetoothAdapter.isDiscovering()) {
            // cancel discovery
            bluetoothAdapter.cancelDiscovery();
            // notify
            Toast.makeText(getApplicationContext(), "Bluetooth: Canceling discovery.", Toast.LENGTH_SHORT).show();
        } else {
            // start discovery
            if (bluetoothAdapter.startDiscovery()) {
                // register callback
                try {
                    this.registerReceiver(broadcastReceiver, filter);
                } catch (Exception ME) {
                }
                // notify
                Toast.makeText(getApplicationContext(), "Bluetooth: Searching unpaired devices.", Toast.LENGTH_SHORT).show();
            } else {
                // notify
                Toast.makeText(getApplicationContext(), "Bluetooth: Discovery failed to start.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /*
     * updateBluetoothIcon
     *
     * Brief: Updates the bluetooth connection status icon
    */
    public void updateBluetoothIcon(BluetoothAdapter bluetoothAdapter) {
        // Change bluetooth icon
        if (optionsMenu != null) {
            // Check if bluetooth exists
            if (bluetoothAdapter != null) {
                // check if is enabled or disabled
                if (bluetoothAdapter.isEnabled()) {
                    // check if is in discovering
                    if (bluetoothAdapter.isDiscovering()) {
                        optionsMenu.findItem(R.id.bluetooth_status).setIcon(getResources()
                                .getDrawable(R.drawable.ic_bluetooth_searching));
                    }
                    // check if is connected to a device or disconnected
                    else {
                        if (connectionStatus) {
                            optionsMenu.findItem(R.id.bluetooth_status).setIcon(getResources()
                                    .getDrawable(R.drawable.ic_bluetooth_connected));
                        } else {
                            optionsMenu.findItem(R.id.bluetooth_status).setIcon(getResources()
                                    .getDrawable(R.drawable.ic_bluetooth));
                        }
                    }

                }
                // bluetooth disabled
                else {
                    optionsMenu.findItem(R.id.bluetooth_status).setIcon(getResources()
                            .getDrawable(R.drawable.ic_bluetooth_disabled));
                }

            }
        }
    }

    /*
     * connectDevice()
     *
     * Brief: try to connect to a bluetooth device and returns a socket to the connection
     *
    */
    public BluetoothSocket connectDevice (BluetoothAdapter bluetoothAdapter, BluetoothDevice btDevice) {
        BluetoothSocket btSocket = null;

        ParcelUuid UUIDlist[] = btDevice.getUuids();
        UUID deviceUUID  = UUID.fromString(UUIDlist[0].toString());


        // create a socket for this bluetooth device
        if(btDevice != null) {
            try{
                btSocket = btDevice.createRfcommSocketToServiceRecord(deviceUUID);
            }catch (IOException e){}
        }
        // try to connect but first cancel discovery because it otherwise slows down the connection.
        if(bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        try {
            btSocket.connect();
        }catch (IOException e){}

        return btSocket;
    }

    /* closeBt
      *
      * Brief: This function, will close the bluetooth adaptor
     */
    public void closeBt(BluetoothAdapter bluetoothAdapter){
        if(bluetoothAdapter!= null) {
            if(bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.disable();
            }
        }
        Toast.makeText(getApplicationContext(), "Bluetooth: Shutdown.", Toast.LENGTH_SHORT).show();
    }

    /*
     * disconnectDevice
     *
     * Brief: This function will close an active connection
    */
    public void disconnectDevice(BluetoothSocket bltSocket){
        if(bltSocket != null){
            try {
                bltSocket.close();
            }catch (IOException e) { }
        }
        Toast.makeText(getApplicationContext(), "Bluetooth: Disconnecting from device.", Toast.LENGTH_SHORT).show();
    }

    /*
     * sendBtMsg
     *
     * Brief: This function will send the msg2send message throw bluetooth
     */
    public void sendBtMsg(OutputStream bltOutStream, int[] msg2send){
        //Log.d("sendBtMsg", "CH1: " + msg2send[1] + " CH2: " + msg2send[2] + " CH3: " + msg2send[3] + " CH4: " + msg2send[4]);
        try {
            if(bltOutStream != null) {
                for(int i = 0; i < msg2send.length; i++) {
                    // send only two bytes
                    bltOutStream.write(msg2send[i]>>8);   // more significant byte
                    bltOutStream.write(msg2send[i]&0xFF); // less significant byte
                    //Log.d("sendBtMsg",  );
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // save last transmit time
        blt_last_send = System.currentTimeMillis();
    }


    final class btnSendThread implements Runnable {
        private boolean exit = false;
        private int[] toSend = new int[6];
        public void run() {
            while(!exit) {
                if(connectionStatus){
                    if(ch1val != toSend[1] || ch2val != toSend[2] || ch3val != toSend[3] || ch4val != toSend[4]) {
                        toSend[0] = INIT_OF_FRAME;
                        toSend[1] = ch1val;
                        toSend[2] = ch2val;
                        toSend[3] = ch3val;
                        toSend[4] = ch4val;
                        toSend[5] = END_OF_FRAME;
                        // send by bluetooth
                        sendBtMsg(mmOutputStream, toSend);
                        // send peridiocity
                        try { // sleep 100ms in order to complete the msg sending
                            Thread.sleep(BTL_SEND_PERIODICITY);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    try { // sleep 1 second
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
        public void stop(){
            exit = true;
        }
    }

    final class KeepAliveThread implements Runnable {
        private int[] toSend = {INIT_OF_FRAME,END_OF_FRAME};
        private boolean exit = false;
        public void run() {
            while (!exit) {
                if (isKeepAlive && connectionStatus) {
                    //
                    if((System.currentTimeMillis() - blt_last_send) > KEEP_ALIVE_TIMEOUT){
                        sendBtMsg(mmOutputStream,toSend);
                    }
                    try { // sleep 1s to get periodicity of 1 second
                        Thread.sleep(KEEP_ALIVE_TIMEOUT);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else if(!isKeepAlive){
                    break;
                }else{
                    // blt not active
                    try { // sleep 1s to get periodicity of 1 second
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d("KeepAliveThread","Bluetooth not connected");
                }
            }
            Log.d("KeepAliveThread","Goodby...");
        }
        public void stop(){
            exit = true;
        }
    }

    final class btnRecevThread implements Runnable {
        private boolean exit = false;
        public void run() {
            while (!exit) {
                if (connectionStatus && mmInputStream != null){
                    try {
                        while (mmInputStream.available() > 0){
                            Log.d("btnRecevThread","" + (char)mmInputStream.read());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try { // sleep 1s to get periodicity of 1 second
                        Thread.sleep(BTL_READ_PERIODICITY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else {
                    try { // sleep 1s to get periodicity of 1 second
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        public void stop(){
            exit = true;
        }
    }

    public void portrait(){
    }
    public void landscape(){
    }


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            //int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Log.d("broadcastReceiver", "connected: " + device + " " + device.getName());
                // filter to our device
                if(mmDevice != null) {
                    if(mmDevice.getName().equals(device.getName())) {
                        // connection ok :)
                        connectionStatus = true;

                        // check connection box
                        optionsMenu.findItem(R.id.connect).setChecked(true);
                    }
                }
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Log.d("broadcastReceiver", "disconnected: " + device);
                // filter to our device
                if(mmDevice != null) {
                    if(mmDevice.getName().equals(device.getName())) {
                        // if connectionStatus = true, means that the disconnect was from the device side
                        if (connectionStatus) {
                            // close the active socket
                            disconnectDevice(mmSocket);
                        }
                        // disconnected
                        connectionStatus = false;

                        // close input and output buffers
                        try {
                            mmInputStream.close();
                            mmOutputStream.close();
                        }catch(IOException e) {}

                        // clear variables
                        mmSocket = null;
                        mmInputStream = null;
                        mmOutputStream = null;
                        mmDevice = null;

                        // un-check connection box
                        optionsMenu.findItem(R.id.connect).setChecked(false);
                    }
                }
            }
            else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //Log.d("broadcastReceiver", "found: " + device + "");
            }
            else if (bltAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //Log.d("broadcastReceiver", "Discovery started");
            }
            else if (bltAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //Log.d("broadcastReceiver", "Discovery finished");
            }

            // update bluetooth icon
            updateBluetoothIcon(bltAdapter);
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // enable bluetooth request
        if (requestCode == ENABLE_BT_REQUEST_CODE) {

            // Bluetooth successfully enabled!
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Ha! Bluetooth is now enabled." +
                                "\n" + "Scanning for remote Bluetooth devices...",
                        Toast.LENGTH_SHORT).show();

            } else { // RESULT_CANCELED as user refused or failed to enable Bluetooth
                Toast.makeText(getApplicationContext(), "Bluetooth is not enabled.",
                        Toast.LENGTH_SHORT).show();
            }
        }
        // other options
        else {}
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setContentView(R.layout.activity_remote_control);
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
            //landscape();
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            setContentView(R.layout.activity_remote_control);
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
            //portrait();
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*Android Init*/
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control);
        // init bluetooth adapter
        bltAdapter = BluetoothAdapter.getDefaultAdapter();
        // init bluetooth actions
        //filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        //filter.addAction(bltAdapter.ACTION_DISCOVERY_STARTED);
        //filter.addAction(bltAdapter.ACTION_DISCOVERY_FINISHED);
        // Register the BroadcastReceiver for ACTION_FOUND
        try {
            this.registerReceiver(broadcastReceiver, filter);
        }catch (Exception ME){}
        // start send thread
        t1 = new Thread(new btnSendThread());
        t1.setPriority(Thread.NORM_PRIORITY); // Norm priority to this thread
        t1.start();
        // start read thread
        t2 = new Thread(new btnRecevThread());
        t2.setPriority(Thread.NORM_PRIORITY); // Norm priority to this thread
        t2.start();
        // start portrait function
        portrait();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        // save menu options for later modification
        optionsMenu = menu;
        // save sub menu options for later modification
        sMenu = optionsMenu.findItem(R.id.connect).getSubMenu();
        // clear sub-menu
        sMenu.clear();
        // check if bluetooth is enabled and change the icon
        updateBluetoothIcon(bltAdapter);
        return true;
    }
    @Override
    public void onJoystickMoved(int xPercent, int yPercent, int id){
        switch (id){
            case R.id.joystickLeft:
                ch1val = xPercent;
                if(isSmooth){
                    if((scale-yPercent) <= (scale/2)){
                        ch2val = (int)((scale-yPercent)*0.5);
                    }else{
                        ch2val = (int)(ch2val*0.95+(scale-yPercent)*0.05);
                    }
                }else{
                    ch2val = scale-yPercent;
                }
                //Log.d("joystickLeft","CH1: " + ch1val + " CH2: " + ch2val);
                break;
            case R.id.joystickRight:
                ch3val = xPercent;
                ch4val = scale-yPercent;
                //Log.d("joystickRight","CH3: " + ch3val + " CH4: " + ch4val);
                break;
        }

        //Log.d("teste",""+ch2val);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // make an assertion for options menu configurations
        assert(optionsMenu != null);

        // if is not a sub item id
        if((item.getItemId() & SUB_ITEM_BASEID) != SUB_ITEM_BASEID) {
            // go throw menu options
            switch (item.getItemId()) {
                case R.id.lock:
                    if (item.isChecked()) {
                        // If item already checked then unchecked it
                        item.setChecked(false);
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);

                    } else {
                        // If item is unchecked then checked it
                        item.setChecked(true);
                        try {
                            if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= 8) {
                                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                                    // setting the orientation as per the device orientation for API level 8+
                                    if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_0) {
                                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                                    } else if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_180) {
                                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                                    } else if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_90) {
                                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                                    } else if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_270) {
                                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                                    }
                                } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                    if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_0) {
                                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                    } else if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_180) {
                                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                                    } else if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_90) {
                                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                    } else if (getWindowManager().getDefaultDisplay().getRotation() == Surface.ROTATION_270) {
                                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                                    }
                                }
                            } else if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) < 8) {
                                // setting the orientation as per the device orientation for API level below 8
                                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                                } else {
                                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                                }

                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                    return true;

                case R.id.smooth:
                    if (item.isChecked()) {
                        // If item already checked then unchecked it
                        item.setChecked(false);
                        isSmooth = false;
                    } else {
                        item.setChecked(true);
                        isSmooth = true;
                    }
                    return true;

                case R.id.bluetooth_status:
                    // if bluetooth is disabled, try to enable it
                    if (!bltAdapter.isEnabled()) {
                        // Init bluetooth module
                        initBt(bltAdapter);
                        // Save previous status of the bluetooth module
                        bluetoothActivationStatus = false;
                    }
                    // if bluetooth is enabled but is not in discovery mode or connected, enter in discovery
                    /*
                    else if ((bltAdapter.isEnabled()) && (!bltAdapter.isDiscovering()) && (!connectionStatus)) {
                        // enter in discovery
                        discoverDevices(bltAdapter);
                    }*/
                    // if bluetooth is enabled and in discovery
                    /*
                    else if ((bltAdapter.isEnabled()) && (bltAdapter.isDiscovering())) {
                        // leave discovery
                        discoverDevices(bltAdapter);
                        // un-register receiver
                        try {
                            this.unregisterReceiver(broadcastReceiver);
                        }catch (Exception ME){}
                        // if the bluetooth module was already active leave it that way
                        if (!bluetoothActivationStatus) {
                            // shutdown bluetooth module
                            closeBt(bltAdapter);
                        }
                    }*/
                    else if(bltAdapter.isEnabled() && !bluetoothActivationStatus && !connectionStatus){
                        if (!bluetoothActivationStatus) {
                            // shutdown bluetooth module
                            closeBt(bltAdapter);
                        }
                    }
                    // bluetooth connected
                    else if(bltAdapter.isEnabled() && connectionStatus){
                        // disconnect from device
                        disconnectDevice(mmSocket);
                    }

                    // change bluetooth icon
                    updateBluetoothIcon(bltAdapter);
                    return true;

                case R.id.connect:
                    // if is not connected yet , fill list of bluetooth devices
                    if (!item.isChecked()) {
                        // update list of bluetooth devices
                        Set<BluetoothDevice> pairedDevices = bltAdapter.getBondedDevices();
                        // check if everything is ok
                        if (sMenu != null) {
                            // fill only new bluetooth devices
                            if (pairedDevices.size() > 0) {
                                // There are paired devices. Get the name and address of each paired device.
                                String deviceName;
                                boolean deviceExists = false;
                                for (BluetoothDevice device : pairedDevices) {
                                    deviceName = device.getName();
                                    for (int i = 0; i < bluetoothDevices.size(); i++) {
                                        if (deviceName.equals(bluetoothDevices.get(i).getName())) {
                                            deviceExists = true;
                                            break;
                                        }
                                    }
                                    if (!deviceExists) {
                                        // add newer device to the list
                                        bluetoothDevices.add(device);
                                        sMenu.add(0, SUB_ITEM_BASEID | bluetoothDevices.size(), Menu.NONE, deviceName);
                                    }
                                }
                            }
                        }
                    }
                    else{
                        // disconnect from device
                        disconnectDevice(mmSocket);
                    }
                    return true;


                case R.id.keepalive:
                    if (item.isChecked()) {
                        // If item already checked then unchecked it
                        item.setChecked(false);
                        isKeepAlive = false;
                    } else {
                        item.setChecked(true);
                        isKeepAlive = true;
                        t3 = new Thread(new KeepAliveThread());
                        t3.setPriority(Thread.MIN_PRIORITY); // Min priority to this thread
                        t3.start();
                    }
                    return true;

                default:
                    return super.onOptionsItemSelected(item);
            }

        }else{
            // get the bluetooth device configurations
            Toast.makeText(this, "Bluetooth: Connecting to: " + item.getTitle() + ".", Toast.LENGTH_SHORT).show();
            // go throw the list of bluetooth devices and search for the selected one
            for (int i = 0; i < bluetoothDevices.size(); i ++) {
                String deviceName = bluetoothDevices.get(i).getName();
                if(deviceName.equals(item.getTitle())){
                    mmDevice = bluetoothDevices.get(i);
                }
            }
            // try to connect to the device
            mmSocket = connectDevice(bltAdapter, mmDevice);

            // check the connection status
            if(mmSocket != null) {
                if(mmSocket.isConnected()){
                    Toast.makeText(this, "Bluetooth: Connected to: " + item.getTitle()  + ".", Toast.LENGTH_SHORT).show();
                    // create input and output buffers
                    try {
                        mmOutputStream = mmSocket.getOutputStream();
                        mmInputStream = mmSocket.getInputStream();
                    }catch(IOException e) {}
                }else{
                    Toast.makeText(this, "Bluetooth: Error while connecting to: " + item.getTitle()  + ".", Toast.LENGTH_SHORT).show();
                }
            }


            //run = false;
            //closeBt(bluetoothAdapter);
            // Toast.makeText(this, "quit()", Toast.LENGTH_SHORT).show();


                /*
                if (connectionStatus){

                    Toast.makeText(this, "Connected to JulioPi", Toast.LENGTH_SHORT).show();
                    run = true;
                    Thread t1 = new Thread(new workerThread());
                    t1.setPriority(Thread.NORM_PRIORITY); // Norm priority to this thread
                    t1.start();
                }
                else
                    Toast.makeText(this, "No JulioPi Detected", Toast.LENGTH_SHORT).show();

                 */
            return true;
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Register the BroadcastReceiver for ACTION_FOUND
        //try {
        //    this.registerReceiver(broadcastReceiver, filter);
        //}catch (Exception ME){}
    }
    @Override
    protected void onPause() {
        super.onPause();
        //try {
        //    this.unregisterReceiver(broadcastReceiver);
        //}catch (Exception ME){}
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // unregister receiver
            this.unregisterReceiver(broadcastReceiver);
            // disconnect from socket
            if(mmSocket!=null){
                disconnectDevice(mmSocket);
            }
            // kill threads
            t1.stop();
            t2.stop();
            t3.stop();
        }catch (Exception ME){}
    }
}

