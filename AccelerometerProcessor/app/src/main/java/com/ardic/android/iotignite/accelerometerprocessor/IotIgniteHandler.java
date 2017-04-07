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


//**********************************************************************************************
//**********************************************************************************************
//**********************************************************************************************
public class IotIgniteHandler implements ConnectionCallback,NodeListener,ThingListener {

    //private static final String TAG = IotIgniteHandler.class.getSimpleName();
    private static final String TAG = "Tst";
    private static final String TAGSend = "SendData";
    private static final String TAGRec = "RecData";

    private static final Intent CUSTOM_LOCAL_ACTION = new Intent();

    // Static singleton instance
    private static IotIgniteHandler INSTANCE = null;
    private static final long IGNITE_RECONNECT_INTERVAL = 10000L;

    private static final String accelerometerNodeStr = "accelerometerNode";
    private static final String accelerometerSensXStr = "accelerometerSensX";
    private static final String accelerometerSensYStr = "accelerometerSensY";
    private static final String accelerometerSensZStr = "accelerometerSensZ";


    private IotIgniteManager mIotIgniteManager;
    private boolean igniteConnected = false;
    private Context appContext;
    private Handler igniteWatchdog = new Handler();

    private Node accelerometerNode;

    private Thing accelerometerSensX;
    private Thing accelerometerSensY;
    private Thing accelerometerSensZ;

    private long speedX;//*******
    private long speedY;
    private long speedZ;

    public String cloudMsg;//*****

    private ThingData dataX;
    private ThingData dataY;
    private ThingData dataZ;

    private ThingType accelerometerSensXType = new ThingType(
            "Accelerometer Sensor X Type",
            "Accelerometer Sensor X Vendor",
            ThingDataType.FLOAT
    );

    private ThingType accelerometerSensYType = new ThingType(
            "Accelerometer Sensor Y Type",
            "Accelerometer Sensor Y Vendor",
            ThingDataType.FLOAT
    );

    private ThingType accelerometerSensZType = new ThingType(
            "Accelerometer Sensor Z Type",
            "Accelerometer Sensor Z Vendor",
            ThingDataType.FLOAT
    );


    private Runnable igniteWatchdogRunnable = new Runnable() {
        @Override
        public void run() {

            if (!igniteConnected) {
                rebuildIgnite();
                igniteWatchdog.postDelayed(this, IGNITE_RECONNECT_INTERVAL);
                Log.e(TAG, "Ignite is not connected. Trying to reconnect...");
            } else {
                Log.e(TAG, "Ignite is already connected.");
            }
        }
    };

    //**********************************************************************************************
    private IotIgniteHandler(Context context) {

        this.appContext = context;
    }

    //**********************************************************************************************
    public static synchronized IotIgniteHandler getInstance(Context appContext) {

        if (INSTANCE == null) {
            INSTANCE = new IotIgniteHandler(appContext);
        }
        return INSTANCE;
    }


    public void start() {

        startIgniteWatchdog();
    }

    //**********************************************************************************************
    private void sendTimeIntent(){
        //*********************
        Intent intents = new Intent("onConnect");
        intents.putExtra("speedX", speedX);
        intents.putExtra("speedY", speedY);
        intents.putExtra("speedZ", speedZ);
        LocalBroadcastManager.getInstance(this.appContext).sendBroadcast(intents);
        //*********************
    }
    @Override
    public void onConnected() {



        Log.i(TAG, "Ignite Connected");

        // cancel watchdog //
        igniteWatchdog.removeCallbacks(igniteWatchdogRunnable);
        igniteConnected = true;

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

        //*****************
        //thnint = ben_Thing.getThingConfiguration().getDataReadingFrequency();
        //sendAccelerometerData(3,3,3);
        sendTimeIntent();
        speedConf();
    }

    //**********************************************************************************************
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

    //**********************************************************************************************
    @Override
    public void onDisconnected() {
        Log.i(TAG, "Ignite Disconnected");
        // start watchdog again here.
        igniteConnected = false;
        startIgniteWatchdog();
    }


    /**
     * Connect to iot ignite
     */
    //**********************************************************************************************
    private void rebuildIgnite() {
        try {
            mIotIgniteManager = new IotIgniteManager.Builder()
                    .setConnectionListener(this)
                    .setContext(appContext)
                    .build();
        } catch (UnsupportedVersionException e) {
            Log.e(TAG, "UnsupportedVersionException :" + e);
        }
    }

    /**
     * remove previous callback and setup new watchdog
     */
    //**********************************************************************************************
    private void startIgniteWatchdog() {
        igniteWatchdog.removeCallbacks(igniteWatchdogRunnable);
        igniteWatchdog.postDelayed(igniteWatchdogRunnable, IGNITE_RECONNECT_INTERVAL);

    }


    /**
     * Set all things and nodes connection to offline.
     * When the application close or destroyed.
     */

    //**********************************************************************************************
    public void shutdown() {

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

    //**********************************************************************************************
    @Override
    public void onConfigurationReceived(Thing thing) {

        /**
         * Thing configuration messages will be handled here.
         * For example data sending frequency or custom configuration may be in the incoming thing object.
         */
        Log.e(TAGRec, "conf Recived");
        Log.e(TAGRec, "Reading Freq : " + thing.getThingConfiguration().getDataReadingFrequency());
        Log.e(TAGRec, "Ofline Time out : " + thing.getThingConfiguration().getOfflineDataTimeout());

        speedConf();
    }

    private void speedConf(){
        sendTimeIntent();
        speedX = accelerometerSensX.getThingConfiguration().getDataReadingFrequency();
        speedY = accelerometerSensY.getThingConfiguration().getDataReadingFrequency();
        speedZ = accelerometerSensZ.getThingConfiguration().getDataReadingFrequency();
    }

    //**********************************************************************************************
    @Override
    public void onActionReceived(String s, String s1, ThingActionData thingActionData) {

        /**
         * Thing action message will be handled here. Call thingActionData.getMessage()
         */

        Log.e(TAGRec, "Node =" + s + "\nSensor = " + s1 + "\nData = " + thingActionData.getMessage());
        //cloudMsg = thingActionData.getMessage();**********

    }

    //**********************************************************************************************
    @Override
    public void onThingUnregistered(String s, String s1) {

        /**
         * If your thing object is unregistered from outside world, you will receive this
         * information callback.
         */
    }

    //**********************************************************************************************
    @Override
    public void onNodeUnregistered(String s) {

    }
}
