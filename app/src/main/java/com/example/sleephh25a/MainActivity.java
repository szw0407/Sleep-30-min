package com.example.sleephh25a;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {



    static long timeout = 1801;  // half an hour
    static long timeleft= 1801; // left
    static long time0  = 0;   // start to sleep
    static long timeP = 0;   // 上次修改睡眠时间长短的时间，3s后才能改动
    static int pass = 0;    // counter of sensor callback
    static int hit = 0;    // counter of hit(if>12)



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });




        // 传感器事件处理函数
        SensorEventListener el = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent e) {
                if (e.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                    pass++;
                    float xyz = Math.abs(e.values[0])+Math.abs(e.values[1])+Math.abs(e.values[2]);
                    if (xyz>12)  // one of them is 9.8
                    {
                        time0 = System.currentTimeMillis()/1000;
                        String tips=pass+" 动作: "+xyz+" @ "+time0;
                        //Toast.makeText(MainActivity.this, tips, Toast.LENGTH_SHORT).show();
                        Log.v("xyz", tips);
                        //if (xyz>30) 改为TYPE_GYROSCOPE
                        timeleft = timeout;
                    }


                }
                // 一个失败的尝试，算了。还是回到上面if(xyz>30)条件吧
                else if (e.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    //Log.v("gyr", e.values[0]+":"+e.values[1]+":"+e.values[2]);
                    float xyz = Math.abs(e.values[0])+Math.abs(e.values[1])+Math.abs(e.values[2]);
                    if ((xyz > 3) && (time0-timeP > 2)) {  // 翻转幅度较大，且间隔2s以上
                        timeout = (timeout/300*300 + 300) % 1801 + 5;
                        //timeout = (timeout + 3) % 15 + 3;  // for debug easy
                        Toast.makeText(MainActivity.this,
                                "timeout rewind to " + timeout / 60 + "m:" + timeout % 60 + "s",
                                Toast.LENGTH_LONG).show();
                        timeP = time0;
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
                Toast.makeText(MainActivity.this, "精度变化事件 hit", Toast.LENGTH_SHORT).show();
            }
        };

        SensorManager sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor s1 = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor s2 = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sm.registerListener(el,s1,SensorManager.SENSOR_DELAY_UI);
        sm.registerListener(el,s2,SensorManager.SENSOR_DELAY_UI);
        final boolean[] status = {false};  // has not yet achieved the timeout
        // 消息处理函数
        Handler flush = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                if (msg.what != 5107)
                    return true;
                if (status[0]) return true;
                if (timeleft<=1) {
                    MediaPlayer m = MediaPlayer.create(MainActivity.this, R.raw.alife);
                    //mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    m.setLooping(true);
                    m.start();
                    Toast.makeText(MainActivity.this, "time out", Toast.LENGTH_LONG).show();
                    status[0] = true;
                    // 任意触碰屏幕停止播放
                    // 先显示这行字在倒计时的地方
                    ((TextView)findViewById(R.id.id_helloworld)).setText("time out, touch screen to restart");
                    findViewById(R.id.main).setOnClickListener(v -> {
                        m.stop();
                        m.release();
                        timeleft = timeout;
                        status[0] = false;
                        ((TextView)findViewById(R.id.id_helloworld)).setText("time left " + timeleft/60 + "m:" + timeleft%60+"s");
                        findViewById(R.id.main).setOnClickListener(null);
                    });

                }
                else{
                    timeleft = timeleft-1;

                ((TextView)findViewById(R.id.id_helloworld)).setText("time left " + timeleft/60 + "m:" + timeleft%60+"s");
                }
                return true;//false;
            }
        });

        // 计时器线程任务，就是发一个消息
        class Every1m extends TimerTask {
            @Override
            public void run() {
                flush.sendEmptyMessage(5107);
            }
        }

        Timer timer1m = new Timer();
        timer1m.schedule(new Every1m(), 1000, 1000);

    }




}