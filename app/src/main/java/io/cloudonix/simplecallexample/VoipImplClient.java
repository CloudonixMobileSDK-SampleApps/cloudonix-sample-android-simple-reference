package io.cloudonix.simplecallexample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import net.greenfieldtech.cloudonixsdk.api.interfaces.IVoIPObserver;
import net.greenfieldtech.cloudonixsdk.api.models.RegistrationData;
import net.greenfieldtech.cloudonixsdk.appinterface.CloudonixSDKClient;
import net.greenfieldtech.cloudonixsdk.utils.SDKLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.stream.Collectors;

import static net.greenfieldtech.cloudonixsdk.api.models.RegistrationData.TransportType.TRANSPORT_TYPE_TLS;
import static net.greenfieldtech.cloudonixsdk.api.models.RegistrationData.TransportType.TRANSPORT_TYPE_UDP;

public class VoipImplClient implements IVoIPObserver {
    private static final String TAG = "VoipImplClient";
    private String callKey = null;
    private SimpleActivityEvents activityEvents;
    private CloudonixSDKClient cxClient;

    public VoipImplClient(Context ctx) {
        InputStream lic = ctx.getResources().openRawResource(R.raw.cloudonix_license_key);
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(lic))) {
            cxClient = CloudonixSDKClient.getInstance(buffer.lines().collect(Collectors.joining("\n")));
            cxClient.addEventsListener(this);
            cxClient.setLogLevel(SDKLogger.LOG_LEVEL_ALL);
            if (!cxClient.checkBinder()) {
                cxClient.bind(ctx); // will cause onSipStarted to be call
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    @Override
    public void onLicense(LicensingState licensingState, String description) {
        if (licensingState != LicensingState.LICENSING_SUCCESS) {
            Log.e(TAG, "License error: " + description);
            return;
        }
        cxClient.setConfig(ConfigurationKey.USER_AGENT, "MyApp/1.0");
        cxClient.setConfiguration(new RegistrationData() {{
            setServerUrl("my-dns-or-ip.server.io");
            setPort(5060);
            setTransportType(TRANSPORT_TYPE_UDP);

            //In case of tls transport use two rows below instead of two row above,
            //and make sure TRANSPORT_TYPE_TLS is imported.
            //setPort(443);
            //setTransportType(TRANSPORT_TYPE_TLS);

            setDomain("my-domain");
            setUsername("my-username");
            setPassword("my-password");
            setDisplayName("my-display-name");
        }});
    }

    public void shutdown() {
        cxClient.shutdown();
    }

    public void addCallbacksListener(SimpleActivityEvents callbacksListener) {
        activityEvents = callbacksListener;
    }

    public void removeCallbacksListener() {
        activityEvents = null;
    }

    @Override
    public void onSipStarted() {
        Log.d(TAG, "Sip is Started");
        cxClient.registerAccount();
        if (Objects.nonNull(activityEvents)) {
            activityEvents.onVoIPStart();
        }
    }

    @Override
    public void onSipStartFailed() {
        Log.d(TAG, "Sip is Failed after Start");
        activityEvents.onVoIPError();
    }

    @Override
    public void onRegisterState(RegisterState result, int expiry) {
        switch (result) {
            case REGISTRATION_SUCCESS:
                Log.d(TAG, "registered with expire: " + expiry);
                if (Objects.nonNull(activityEvents)) {
                    activityEvents.onConnectState(true);
                }
                break;
            case REGISTRATION_ERROR_CREDENTIALS:
                Log.d(TAG, "auth error, expire: " + expiry);
                if (Objects.nonNull(activityEvents)) {
                    activityEvents.onConnectState(false);
                }
                break;
            case REGISTRATION_UNREGISTERED:
                Log.d(TAG, "No longer registered, expire: " + expiry);
                if (Objects.nonNull(activityEvents)) {
                    activityEvents.onConnectState(false);
                }
                break;
        }
    }

    @Override
    public void onCallState(String key, CallState callState, String contactUrl) {
        callKey = key;
        switch (callState) {
            case CALL_STATE_STARTING:
                Log.d(TAG, "onStarting: " + key + " to number: " + contactUrl);
                break;
            case CALL_STATE_CONNECTING:
                Log.d(TAG, "onConnecting: " + key + " to number: " + contactUrl);
                break;
            case CALL_STATE_CALLING:
                Log.d(TAG, "onCalling: " + key + " to number: " + contactUrl);
                break;
            case CALL_STATE_RINGING:
                Log.d(TAG, "onRinging: " + key + " Number: " + contactUrl);
                if (Objects.nonNull(activityEvents)) {
                    activityEvents.onCallRinging();
                }
                break;
            case CALL_STATE_EARLY:
                Log.d(TAG, "onEarlyMedia: " + key + " Number: " + contactUrl);
                if (Objects.nonNull(activityEvents)) {
                    activityEvents.onCallEarlyMedia();
                }
                break;
            case CALL_STATE_MEDIAACTIVE:
                Log.d(TAG, "onMediaActive: " + key + " Number: " + contactUrl);
                break;
            case CALL_STATE_CONFIRMED:
                Log.d(TAG, "onConfirmed: " + key + " Number: " + contactUrl);
                if (Objects.nonNull(activityEvents)) {
                    activityEvents.onCallConnected();
                }
                break;
            case CALL_STATE_DISCONNECTEDDUETOBUSY:
            case CALL_STATE_DISCONNECTEDMEDIACHANGED:
            case CALL_STATE_DISCONNECTEDDUETONOMEDIA:
            case CALL_STATE_DISCONNECTEDDUETOTIMEOUT:
            case CALL_STATE_DISCONNECTED:
                Log.d(TAG, "onHangup: " + callState + ", " + key + " Number: " + contactUrl);
                callKey = null;
                if (Objects.nonNull(activityEvents)) {
                    activityEvents.onCallDisconnected();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onLog(int i, String s) {

    }

    @Override
    public void onSipStopped() {
        Log.d(TAG, "Sip is Failed after Start");
    }

    @Override
    public void onBluetoothState(BluetoothState bluetoothState) {

    }

    @Override
    public void onAudioRouteChange(AudioRoute audioRoute) {

    }

    @Override
    public void onBluetoothBtnState(BluetoothButtonsState bluetoothButtonsState, String s, String s1, Integer integer, Integer integer1) {

    }

    @Override
    public void onNetworkLost() {

    }

    @Override
    public void onNetworkChanged() {

    }

    @Override
    public void onNetworkRegained() {

    }

    @Override
    public void onNATTypeDetected(NATType natType) {
        Log.d(TAG, "onNATTypeDetected " + natType);
    }

    public boolean dial(String number) {
        Log.d(TAG, "dial: " + number);
        if (!cxClient.isRegistered()) {
            return false;
        }
        cxClient.dial(number);
        return false;
    }

    public void hangup() {
        cxClient.hangup(callKey);
    }

    public void askRecordAudioPermissions(Activity activity, boolean isFirstTime) {
        if (activity.checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "Permission " + Manifest.permission.RECORD_AUDIO
                            + " is not granted. Please accept it for the app to work correctly.",
                    Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
            return;
        }
    }
}

