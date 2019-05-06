package io.cloudonix.simplecallexample;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

public class SimpleButtonActivity extends AppCompatActivity {

    private static final String TAG = SimpleButtonActivity.class.getSimpleName();

    private ImageButton btnCall;       // Call button.
    private TextView label;            // "CONNECTED" label, becomes visible only when the call is connected.

    private VoipImplClient voipClient; // VoIP client instance.

    private int mStreamID;
    private boolean isActiveCall;
    private SoundPool mSoundPool;
    private int ringingId;

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
        btnCall = (ImageButton) findViewById(R.id.call_button);
        btnCall.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        btnCall.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.i(TAG, "Call button taped, the call is active: " + isActiveCall);
                if (isActiveCall) {
                    voipClient.hangup();
                    btnCall.setImageResource(R.drawable.btn_call_selector);
                    stopRingingTone();
                    setLabelVisibility(false);
                } else {
                    //Dial specific number or application
                    voipClient.dial("call-the-number-or-application");
                    btnCall.setImageResource(R.drawable.btn_hangup_selector);
                }
            }

        });
        btnCall.setClickable(false);

        voipClient = new VoipImplClient(this);
        voipClient.askRecordAudioPermissions(this);

        //Ringing tone
        mSoundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        ringingId = mSoundPool.load(getApplicationContext(), R.raw.ringing, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        voipClient.setCallbacksListener(mCallbacksListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        voipClient.removeCallbacksListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        voipClient.shutdown();
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

    private void setLabelVisibility(boolean isVisible) {
        label.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }

    private SimpleActivityEvents mCallbacksListener = new SimpleActivityEvents() {

        @Override
        public void onVoIPStart() {
            btnCall.setClickable(false);
        }

        @Override
        public void onVoIPError() {
            btnCall.setClickable(false);
        }

        @Override
        public void onConnectState(boolean isConnected) {
            btnCall.setClickable(isConnected);
        }

        @Override
        public void onCallRinging() {
            isActiveCall = true;
            startRingingTone();
        }

        @Override
        public void onCallEarlyMedia() {
            isActiveCall = true;
            stopRingingTone();
        }

        @Override
        public void onCallConnected() {
            stopRingingTone();
            isActiveCall = true;
            setLabelVisibility(true);
        }

        @Override
        public void onCallDisconnected() {
            stopRingingTone();
            btnCall.setImageResource(R.drawable.btn_call_selector);
            isActiveCall = false;
            setLabelVisibility(false);
        }

    };

}