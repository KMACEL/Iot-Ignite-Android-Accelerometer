package com.ardic.android.iotignite.accelerometerprocessor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
//Use Sensor for "implements SensorEventListener"

    private static final String TAGMain="MainTag";
    private static final String TAGTime="MTTime";
    //For Sensor Data
    private TextView accelerometerTextVievX;
    private ProgressBar accelerometerProccesX;

    private TextView accelerometerTextVievY;
    private ProgressBar accelerometerProccesY;

    private TextView accelerometerTextVievZ;
    private ProgressBar accelerometerProccesZ;


    // Sensor Manager
    private SensorManager sManager;

    private IotIgniteHandler mIotIgniteHandler;

    private int flag=0;
    private  long timerValueX;
    private  long timerValueY ;
    private  long timerValueZ ;

    private Handler handlerMainX = new Handler();
    private Handler handlerMainY = new Handler();
    private Handler handlerMainZ = new Handler();

    private float dataValueX;
    private float dataValueY;
    private float dataValueZ;

    BroadcastReceiver message = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            flag = 1;

            Toast.makeText(context,"Connected ", Toast.LENGTH_LONG).show();
            timerValueX= intent.getLongExtra("speedX",1000);
            Log.i(TAGTime,"X Delay Time : "+timerValueX);

            timerValueY= intent.getLongExtra("speedY",1000);
            Log.i(TAGTime,"Y Delay Time : "+timerValueY);

            timerValueZ= intent.getLongExtra("speedZ",1000);
            Log.i(TAGTime,"Z Delay Time : "+timerValueZ);

            handlerMainX.removeCallbacks(handlerMainRunnableX);
            handlerMainY.removeCallbacks(handlerMainRunnableY);
            handlerMainZ.removeCallbacks(handlerMainRunnableZ);

            handlerMainX.postDelayed(handlerMainRunnableX, timerValueX);
            handlerMainY.postDelayed(handlerMainRunnableY, timerValueY);
            handlerMainZ.postDelayed(handlerMainRunnableZ, timerValueZ);

        }
    };

    Runnable handlerMainRunnableX =new Runnable(){
        @Override
        public void run() {
            handlerMainX.postDelayed(this, timerValueX);
            if (flag==1) {
                mIotIgniteHandler.sendAccelerometerDataX(dataValueX);
                Log.i(TAGMain,"Send To Data X");
            }
        }
    };

    Runnable handlerMainRunnableY =new Runnable(){
        @Override
        public void run() {
            handlerMainY.postDelayed(this, timerValueY);
            if (flag==1) {
                mIotIgniteHandler.sendAccelerometerDataY(dataValueY);
                Log.i(TAGMain,"Send To Data Y ");
            }
        }
    };

    Runnable handlerMainRunnableZ =new Runnable(){
        @Override
        public void run() {
            handlerMainZ.postDelayed(this, timerValueZ);
            if (flag==1) {
                mIotIgniteHandler.sendAccelerometerDataZ(dataValueZ);
                Log.i(TAGMain,"Send To Data Z ");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelerometerTextVievX=(TextView) findViewById(R.id.accelerometerTextX);
        accelerometerProccesX=(ProgressBar) findViewById(R.id.accelerometerProccesBarX);

        accelerometerTextVievY=(TextView) findViewById(R.id.accelerometerTextY);
        accelerometerProccesY=(ProgressBar) findViewById(R.id.accelerometerProccesBarY);

        accelerometerTextVievZ=(TextView) findViewById(R.id.accelerometerTextZ);
        accelerometerProccesZ=(ProgressBar) findViewById(R.id.accelerometerProccesBarZ);


        sManager=(SensorManager)getSystemService(SENSOR_SERVICE);
        sManager.registerListener(this,sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),sManager.SENSOR_DELAY_NORMAL);

        mIotIgniteHandler = IotIgniteHandler.getInstance(getApplicationContext());
        mIotIgniteHandler.start();

        LocalBroadcastManager.getInstance(this).registerReceiver(message,
                new IntentFilter("onConnect"));



    }


    //Implement SensorEventListener



    @Override
    protected void onDestroy(){
        super.onDestroy();
        sManager.unregisterListener(this);

        if(mIotIgniteHandler != null){
            mIotIgniteHandler.shutdown();
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){
            float[] accelerometerValue=event.values;

            accelerometerTextVievX.setText("X Value : " + String.valueOf(accelerometerValue[0]));
            accelerometerProccesX.setProgress(-((int)accelerometerValue[0]*10)+100);

            accelerometerTextVievY.setText("Y Value : " + String.valueOf(accelerometerValue[1]));
            accelerometerProccesY.setProgress(((int)accelerometerValue[1]*10)+100);

            accelerometerTextVievZ.setText("Z Value : " + String.valueOf(accelerometerValue[2]));
            accelerometerProccesZ.setProgress(((int)accelerometerValue[2]*10)+100);

            //mIotIgniteHandler.sendAccelerometerData(accelerometerValue[0],accelerometerValue[1],accelerometerValue[2]);
            dataValueX=accelerometerValue[0] ;
            dataValueY=accelerometerValue[1];
            dataValueZ=accelerometerValue[2];
        }



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
