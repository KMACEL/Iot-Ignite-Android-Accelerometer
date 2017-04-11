package com.ardic.android.iotignite.accelerometerprocessor;


import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ardic.android.iotignite.callbacks.ConnectionCallback;
import com.ardic.android.iotignite.enumerations.NodeType;
import com.ardic.android.iotignite.enumerations.ThingCategory;
import com.ardic.android.iotignite.enumerations.ThingDataType;
import com.ardic.android.iotignite.exceptions.UnsupportedVersionException;
import com.ardic.android.iotignite.listeners.NodeListener;
import com.ardic.android.iotignite.listeners.ThingListener;
import com.ardic.android.iotignite.nodes.IotIgniteManager;
import com.ardic.android.iotignite.nodes.Node;
import com.ardic.android.iotignite.things.Thing;
import com.ardic.android.iotignite.things.ThingActionData;
import com.ardic.android.iotignite.things.ThingData;
import com.ardic.android.iotignite.things.ThingType;


//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//+++++++++++++++++++++++++++++++++ IoT - Ignite Handler Class +++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
public class IotIgniteHandler implements ConnectionCallback,NodeListener,ThingListener {

    //**********************************************************************************************
    //*********************************** Start ****************************************************
    //**********************************************************************************************



    //**********************************************************************************************
    //************************************ Definitions *********************************************

    private static final String TAG = "Tst";                        // IoT - Ignite Global TAG
    private static final String TAGSend = "SendData";               // IoT - Ignite Data Send TAG
    private static final String TAGRec = "RecData";                 // IoT - Ignite Data Receive TAG

    private static IotIgniteHandler INSTANCE = null;                // IoT - Ignite Handler
    private static final long IGNITE_RECONNECT_INTERVAL = 10000L;   // IoT - Ignite Handler Interval


    private IotIgniteManager mIotIgniteManager;                     // IoT - Ignite Connection Manager
    private boolean igniteConnected = false;                        // IoT - Ignite Connection Statue
    private Context appContext;                                     // Context Create
    private Handler igniteWatchdog = new Handler();                 // IoT - Ignite Connection Handler

    //*********************************** Definitions End ******************************************
    //**********************************************************************************************



    //**********************************************************************************************
    //************************************* Context ************************************************

    //----------------------------------- Context Definitions --------------------------------------
    private IotIgniteHandler(Context context) {

        this.appContext = context;
    }
    //----------------------------------------------------------------------------------------------


    //------------------------------------ Instance Control ----------------------------------------
    public static synchronized IotIgniteHandler getInstance(Context appContext) {

        if (INSTANCE == null) {
            INSTANCE = new IotIgniteHandler(appContext);
        }
        return INSTANCE;
    }
    //---------------------------------------------------------------------------------------------

    //************************************* XXXXXXX ************************************************
    //**********************************************************************************************


    //**********************************************************************************************
    //******************************** Start End ***************************************************
    //**********************************************************************************************






    //**********************************************************************************************
    //**************************** IoT - Ignite Connection Begin ***********************************
    //**********************************************************************************************

    //------------------------------- Start WatchDog Function --------------------------------------
    public void start() {

        startIgniteWatchdog();
    }
    //----------------------------------------------------------------------------------------------


    //----------------------------------------------------------------------------------------------
    private void startIgniteWatchdog() {    //????
        igniteWatchdog.removeCallbacks(igniteWatchdogRunnable);
        igniteWatchdog.postDelayed(igniteWatchdogRunnable, IGNITE_RECONNECT_INTERVAL);

    }
    //----------------------------------------------------------------------------------------------


    //------------------------------- Connection Check ---------------------------------------------
    private Runnable igniteWatchdogRunnable = new Runnable() {
        // This section controls the connection with "IoT-Ignite" at the specified time.
        @Override
        public void run() {

            if (!igniteConnected) {
                rebuildIgnite();        // Connection Function
                igniteWatchdog.postDelayed(this, IGNITE_RECONNECT_INTERVAL);    // WatchDog Delay
                Log.e(TAG, "Ignite is not connected. Trying to reconnect...");
            } else {
                Log.e(TAG, "Ignite is already connected.");
            }
        }
    };
    //----------------------------------------------------------------------------------------------


    //---------------------------- IoT - Ignite Connection -----------------------------------------
    private void rebuildIgnite() {
        //This section carries out the "IoT-Ignite" connection
        try {
            mIotIgniteManager = new IotIgniteManager.Builder()
                    .setConnectionListener(this)
                    .setContext(appContext)
                    .build();
        } catch (UnsupportedVersionException e) {
            Log.e(TAG, "UnsupportedVersionException :" + e);
        }
    }
    //----------------------------------------------------------------------------------------------


    //------------------------ IoT - Ignite Closing the Connection ---------------------------------
    public void shutdown() {
        // This section ends the connection "IoT-Ignite"
        if (accelerometerNode != null) {
            if (accelerometerSensX != null) {
                accelerometerSensX.setConnected(false, "Application Destroyed");
            }

            if (accelerometerSensY != null) {
                accelerometerSensY.setConnected(false, "Application Destroyed");
            }

            if (accelerometerSensZ != null) {
                accelerometerSensZ.setConnected(false, "Application Destroyed");
            }

            accelerometerNode.setConnected(false, "Application Destroyed");
        }
    }
    //---------------------------------------------------------------------------------------------


    //**********************************************************************************************
    //********************** IoT - Ignite Connection End *******************************************
    //**********************************************************************************************






    //**********************************************************************************************
    //************************* IoT - Ignite Callback **********************************************
    //**********************************************************************************************

    //--------------------------- Connect Call Back ------------------------------------------------
    @Override
    public void onConnected() {
        // This section contains the first operations to be performed when the "IoT-Ignite" connection is provided
        Log.i(TAG, "Ignite Connected");

        igniteWatchdog.removeCallbacks(igniteWatchdogRunnable);
        igniteConnected = true;

        createNod();
        createSensors();

        sendTimeIntent();
        speedConf();
    }
    //----------------------------------------------------------------------------------------------


    //------------------------ Get Configuration Callback ------------------------------------------
    @Override
    public void onConfigurationReceived(Thing thing) {
        // This section contains the operations to be performed when configured by "IoT-Ignite" **************
        Log.e(TAGRec, "conf Recived");
        Log.e(TAGRec, "Reading Freq : " + thing.getThingConfiguration().getDataReadingFrequency());
        Log.e(TAGRec, "Ofline Time out : " + thing.getThingConfiguration().getOfflineDataTimeout());

        speedConf();
    }
    //----------------------------------------------------------------------------------------------



    //---------------------------- Get Action Callback ---------------------------------------------
    @Override
    public void onActionReceived(String s, String s1, ThingActionData thingActionData) {
        //This section contains the operations to be performed when action by "IoT-Ignite" ************
        Log.e(TAGRec, "Node =" + s + "\nSensor = " + s1 + "\nData = " + thingActionData.getMessage());
    }
    //----------------------------------------------------------------------------------------------


    //-------------------------- Thing Unregistered Callback ---------------------------------------
    @Override
    public void onThingUnregistered(String s, String s1) {
        // This section contains operations to be performed when the sensor connection is interrupted

    }
    //---------------------------------------------------------------------------------------------


    //-------------------------- Node Unregistered Call Back ---------------------------------------
    @Override
    public void onNodeUnregistered(String s) {
        // This section contains operations to be performed when the node connection is interrupted

    }
    //----------------------------------------------------------------------------------------------


    //---------------------------- Disconnect Callback ---------------------------------------------
    @Override
    public void onDisconnected() {
        // This section contains the first operations to be performed when the "IoT-Ignite" dissconnected is provided
        Log.i(TAG, "Ignite Disconnected");
        // start watchdog again here.
        igniteConnected = false;
        startIgniteWatchdog();
    }
    //---------------------------------------------------------------------------------------------

    //**********************************************************************************************
    //************************** IoT - Ignite Callback End *****************************************
    //**********************************************************************************************






    //**********************************************************************************************
    //*************************** Node - Sensor Transactions ***************************************
    //**********************************************************************************************



    //**********************************************************************************************
    //******************************** Definitions *************************************************

    private Node accelerometerNode;         // Create Node

    private Thing accelerometerSensX;       // Create Sensor
    private Thing accelerometerSensY;       // Create Sensor
    private Thing accelerometerSensZ;       // Create Sensor

    private long speedX;                    // accelerometerSensX delay time
    private long speedY;                    // accelerometerSensY delay time
    private long speedZ;                    // accelerometerSensz delay time

    private ThingData dataX;                // accelerometerSensX Send Area
    private ThingData dataY;                // accelerometerSensY Send Area
    private ThingData dataZ;                // accelerometerSensZ Send Area

    private static final String accelerometerNodeStr = "accelerometerNode";     // Node Name
    private static final String accelerometerSensXStr = "accelerometerSensX";   // Sensor Name
    private static final String accelerometerSensYStr = "accelerometerSensY";   // Sensor Name
    private static final String accelerometerSensZStr = "accelerometerSensZ";   // Sensor Name

    //******************************* Definitions End **********************************************
    //**********************************************************************************************



    //**********************************************************************************************
    //***************************** Definition Sensors *********************************************

    //--------------------------- Definition "X" Sensor --------------------------------------------
    private ThingType accelerometerSensXType = new ThingType(
            "Accelerometer Sensor X Type",      // Sensor Type
            "Accelerometer Sensor X Vendor",    // Sensor Vendor
            ThingDataType.FLOAT                 // Sensor Varible Type
    );
    //----------------------------------------------------------------------------------------------


    //--------------------------- Definition "Y" Sensor --------------------------------------------
    private ThingType accelerometerSensYType = new ThingType(
            "Accelerometer Sensor Y Type",      // Sensor Type
            "Accelerometer Sensor Y Vendor",    // Sensor Vendor
            ThingDataType.FLOAT                 // Sensor Varible Type
    );
    //---------------------------------------------------------------------------------------------


    //--------------------------- Definition "Z" Sensor --------------------------------------------
    private ThingType accelerometerSensZType = new ThingType(
            "Accelerometer Sensor Z Type",      // Sensor Type
            "Accelerometer Sensor Z Vendor",    // Sensor Vendor
            ThingDataType.FLOAT                 // Sensor Varible Type
    );
    //----------------------------------------------------------------------------------------------

    //************************ Definition Sensors End **********************************************
    //**********************************************************************************************



    //**********************************************************************************************
    //************************** Create Transactions ***********************************************

    //------------------------------ Create Nod ----------------------------------------------------
    private void  createNod(){
        Log.i(TAG, "Creating Node: " + accelerometerNodeStr);

        accelerometerNode = IotIgniteManager.NodeFactory.createNode(
                accelerometerNodeStr,
                accelerometerNodeStr,
                NodeType.GENERIC,
                null,
                this
        );


        if (accelerometerNode != null) {
            Log.i(TAG, accelerometerNode.getNodeID() + " created.");

            if (!accelerometerNode.isRegistered()) {
                Log.i(TAG, accelerometerNode.getNodeID() + " is registering...");

                if (accelerometerNode.register()) {
                    Log.i(TAG, accelerometerNode.getNodeID() + " is registered successfully. Setting connection true");
                    accelerometerNode.setConnected(true, "");
                }
            } else {
                Log.i(TAG, accelerometerNode.getNodeID() + " has already registered. Setting connection true");
                accelerometerNode.setConnected(true, "");
            }
        }
    }
    //----------------------------------------------------------------------------------------------


    //------------------------------ Create Sonrsor ------------------------------------------------
    private void createSensors(){
        if (accelerometerNode != null && accelerometerNode.isRegistered()) {

            accelerometerSensX = accelerometerNode.createThing(
                    accelerometerSensXStr,
                    accelerometerSensXType,
                    ThingCategory.EXTERNAL,
                    true,
                    this,
                    null
            );

            accelerometerSensY = accelerometerNode.createThing(
                    accelerometerSensYStr,
                    accelerometerSensYType,
                    ThingCategory.EXTERNAL,
                    true,
                    this,
                    null
            );

            accelerometerSensZ = accelerometerNode.createThing(
                    accelerometerSensZStr,
                    accelerometerSensZType,
                    ThingCategory.EXTERNAL,
                    true,
                    this,
                    null
            );
        }
    }
    //---------------------------------------------------------------------------------------------

    //************************* Create Transactions End ********************************************
    //**********************************************************************************************



    //**********************************************************************************************
    //************************* Send Data Transactions *********************************************

    //------------------------- Send "X" Sensor Data -----------------------------------------------
    public void sendAccelerometerDataX(float x) {
        speedConf();
        dataX = new ThingData();
        if (accelerometerSensX != null) {

            if (accelerometerSensX.isRegistered() || accelerometerSensX.register()) {
                Log.i(TAG, "Thing[" + accelerometerSensX.getThingID() + "]  is registered.");
                accelerometerSensX.setConnected(true, "");
                dataX.addData(x);

                if (accelerometerSensX.sendData(dataX)) {
                    Log.i(TAGSend, "Data transmission completed. Submitted Data X : "+x);
                } else {
                    Log.e(TAGSend, "Error! Data transmission could not be completed..."+x);
                }
            }
        }
    }
    //----------------------------------------------------------------------------------------------


    //---------------------------- Send "Y" Sensor Data --------------------------------------------
    public void sendAccelerometerDataY(float y) {
        speedConf();
        dataY = new ThingData();

        if (accelerometerSensY != null) {

            if (accelerometerSensY.isRegistered() || accelerometerSensY.register()) {
                Log.i(TAG, "Thing[" + accelerometerSensY.getThingID() + "]  is registered.");
                accelerometerSensY.setConnected(true, "");
                dataY.addData(y);

                if (accelerometerSensY.sendData(dataY)) {
                    Log.i(TAGSend, "Data transmission completed. Submitted Data Y : "+y);
                } else {
                    Log.e(TAGSend, "Error! Data transmission could not be completed..."+ y);
                }
            }
        }
    }
    //----------------------------------------------------------------------------------------------


    //---------------------------- Send "Z" Sensor Data --------------------------------------------
    public void sendAccelerometerDataZ(float z) {
        speedConf();
        dataZ = new ThingData();
        if (accelerometerSensZ != null) {

            if (accelerometerSensZ.isRegistered() || accelerometerSensZ.register()) {
                Log.i(TAG, "Thing[" + accelerometerSensZ.getThingID() + "]  is registered.");
                accelerometerSensZ.setConnected(true, "");
                dataZ.addData(z);

                if (accelerometerSensZ.sendData(dataZ)) {
                    Log.i(TAGSend, "Data transmission completed. Submitted Data Y : "+z);
                } else {
                    Log.e(TAGSend, "Error! Data transmission could not be completed..."+z);
                }
            }
        }
    }
    //---------------------------------------------------------------------------------------------

    //************************* Send Data Transactions *********************************************
    //**********************************************************************************************



    //**********************************************************************************************
    //*************************** Other Transactions ***********************************************

    //-------------------------- Sensor Data Frequency ---------------------------------------------
    private void speedConf(){
        sendTimeIntent();
        speedX = accelerometerSensX.getThingConfiguration().getDataReadingFrequency();
        speedY = accelerometerSensY.getThingConfiguration().getDataReadingFrequency();
        speedZ = accelerometerSensZ.getThingConfiguration().getDataReadingFrequency();
    }
    //---------------------------------------------------------------------------------------------


    //----------------------- Sensor Data Frequency BroadCast --------------------------------------
    private void sendTimeIntent(){
        //*********************
        Intent intents = new Intent("onConnect");
        intents.putExtra("speedX", speedX);
        intents.putExtra("speedY", speedY);
        intents.putExtra("speedZ", speedZ);
        LocalBroadcastManager.getInstance(this.appContext).sendBroadcast(intents);
    }
    //----------------------------------------------------------------------------------------------

    //**************************** Other Transactions End ******************************************
    //**********************************************************************************************

    //**********************************************************************************************
    //************************** Node - Sensor Transactions End ************************************
    //**********************************************************************************************
}

//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++ Class End  ++++++++++++++++++++++++++++++++++++++++++++++
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
