package io.cloudonix.simplecallexample;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class SimpleButtonActivity extends AppCompatActivity {
    private static final String TAG = "SimpleButtonActivity";
    private int mStreamID;
    private volatile boolean isActiveCall = false;
    private SoundPool mSoundPool;
    private int ringingId;
    private ImageButton knepl; //Call button.
    private VoipImplClient klyent; //VoIP client instance.
    private TextView label; //"CONNECTED" label, becomes visible only when the call is connected.
    private int delay=10000;// Statistics will start after 10 seconds.
    private int period = 3000; //Statistics will show every 3 seconds.

    private Timer timer = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_button);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.title_activity_simple_button);
        setSupportActionBar(toolbar);
        label = (TextView) findViewById(R.id.label_connected);
        label.setVisibility(View.INVISIBLE);
        //Button call/hangup
        knepl = (ImageButton) findViewById(R.id.call_button);
        knepl.setBackgroundColor(getResources().getColor(android.R.color.transparent, this.getTheme()));
        knepl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Call button taped, the call is active: " + isActiveCall);
                if (isActiveCall) {
                    klyent.hangup();
                    knepl.setImageResource(R.drawable.btn_call_selector);
                    stopRingingTone();
                    makeLableInvisible();
                } else {
                    //Dial specific number or application
                    klyent.dial("call-the-number-or-application");
                    knepl.setImageResource(R.drawable.btn_hangup_selector);
                    timer = new Timer();
                    if(timer!=null) {
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                klyent.statistic();
                            }
                        }, delay, period);
                    }
                }
            }
        });
        knepl.setClickable(false);
        klyent = new VoipImplClient(this);
        klyent.addCallbacksListener(mCallbacksListener);
        klyent.askRecordAudioPermissions(this, true);
        //Ringing tone
        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        ringingId = mSoundPool.load(getApplicationContext(), R.raw.ringing, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        klyent.addCallbacksListener(mCallbacksListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        klyent.removeCallbacksListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        klyent.shutdown();
    }

    private void startRingingTone() {
        if (mStreamID > 0) {
            return;
        }
        mStreamID = mSoundPool.play(ringingId, 1.0f, 1.0f, 1, -1, 1.0f);
        Log.i(TAG, "startRingingTone, streamID: " + mStreamID);
    }

    private void stopRingingTone() {
        if (mSoundPool == null) {
            return;
        }
        if (mStreamID != 0) {
            mSoundPool.stop(mStreamID);
            mStreamID = 0;
        }
        Log.i(TAG, "stopRingingTone");
    }

    private void makeLableVisible(){
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                label.setVisibility(View.VISIBLE);
            }
        });
    }
    private void makeLableInvisible(){
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                label.setVisibility(View.INVISIBLE);
            }
        });
    }
    private SimpleActivityEvents mCallbacksListener = new SimpleActivityEvents() {
        @Override
        public void onVoIPStart() {
            knepl.setClickable(true);
        }

        @Override
        public void onVoIPError() {
            knepl.setClickable(false);
        }

        @Override
        public void onConnectState(boolean connected) {
            knepl.setClickable(connected);
        }

        @Override
        public void onCallRinging(){
            isActiveCall = true;
            startRingingTone();
        }
        @Override
        public void onCallEarlyMedia(){
            isActiveCall = true;
            stopRingingTone();
        }
        @Override
        public void onCallConnected(){
            stopRingingTone();
            isActiveCall = true;
            makeLableVisible();
        }
        @Override
        public void onCallDisconnected(){
            if(timer != null) {
            timer.cancel();
            timer=null; }
            stopRingingTone();
            knepl.setImageResource(R.drawable.btn_call_selector);
            isActiveCall = false;
            makeLableInvisible();
        }
    };
}

