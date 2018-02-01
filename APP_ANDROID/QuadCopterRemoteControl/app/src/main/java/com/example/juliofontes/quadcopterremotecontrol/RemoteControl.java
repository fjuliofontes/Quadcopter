package com.example.juliofontes.quadcopterremotecontrol;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
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
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.content.res.Configuration;


public class RemoteControl extends AppCompatActivity implements JoystickView.JoystickListener {

    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice = null;

    boolean run = false;
    float ch1val=0, ch2val=0, ch3val=0, ch4val=0; // All the values of the channel
    public void initBt(){
        UUID uuid = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"); //Standard SerialPortService ID
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            if (!mmSocket.isConnected()){
                mmSocket.connect();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    public void closeBt(){
        try{
            sendBtMsg("done()");
            mmSocket.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    public void sendBtMsg(String msg2send){
        try {
            OutputStream mmOutputStream = mmSocket.getOutputStream();
            mmOutputStream.write(msg2send.getBytes());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*Android Init*/
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote_control);
        /*TextView and bluetooth stuff*/
        final TextView logger = (TextView) findViewById(R.id.textView2);
        final Switch on_off = (Switch) findViewById(R.id.switch1);
        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        /*End of inicializations */

        final class workerThread implements Runnable {
            float old_ch1 = 0,old_ch2 = 0,old_ch3 = 0,old_ch4 = 0;
            String msg;
            public void run() {
                while(run) {
                    if(ch1val != old_ch1 || ch2val != old_ch2 || ch3val != old_ch3 || ch4val != old_ch4) {
                        old_ch1 = ch1val;
                        old_ch2 = ch2val;
                        old_ch3 = ch3val;
                        old_ch4 = ch4val;
                        msg = "CH1:" + (int) ch1val + "_CH2:" + (int) ch2val + "_CH3:" + (int) ch3val + "_CH4:" + (int) ch4val + "\n";
                        sendBtMsg(msg);
                        try { // sleep 1ms in order to complete the mensage sending
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }


        on_off.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                logger.setText("Switch Changed!");
                if(isChecked) {
                    if(!mBluetoothAdapter.isEnabled())
                    {
                        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBluetooth, 0);
                    }
                    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                    if(pairedDevices.size() > 0)
                    {
                        boolean status = false;
                        for(BluetoothDevice device : pairedDevices)
                        {
                            if(device.getName().equals("JulioPi")) //Note, you will need to change this to match the name of your device
                            {
                                //Log.e("QuadPiServer",device.getName());
                                mmDevice = device;
                                initBt();
                                status = true;
                                break;
                            }
                        }
                        if(status) {
                            logger.setText("Connected to JulioPi");
                            run = true;
                            Thread t1 = new Thread(new workerThread());
                            t1.start();
                        }
                        else {
                            logger.setText("No JulioPi Detected");
                        }
                    }
                    else{
                        logger.setText("No Paired Devices");
                    }
                }
                else {
                    if(run) {
                        run = false;
                        closeBt();
                        logger.setText("quit()");
                    }
                    else {
                        logger.setText("Switch Changed!");
                    }
                }
            }
        });

        /*
        home.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            }
        });*/

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }
    @Override
    public void onJoystickMoved(float xPercent, float yPercent, int id){
        switch (id){
            case R.id.joystickLeft:
                //Log.d("Main Method","X percent" + xPercent + "Y percent" + yPercent);
                ch1val = xPercent;
                ch2val = yPercent;
                break;
            case R.id.joystickRight:
                //Log.d("Main Method","X percent" + xPercent + "Y percent" + yPercent);
                ch3val = xPercent;
                ch4val = yPercent;
                break;
        }
    }
}
