package io.cloudonix.simplecallexample;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import net.greenfieldtech.cloudonixsdk.api.interfaces.IVoIPObserver;
import net.greenfieldtech.cloudonixsdk.api.models.RegistrationData;
import net.greenfieldtech.cloudonixsdk.appinterface.CloudonixSDKClient;
import net.greenfieldtech.cloudonixsdk.utils.CryptUtils;
import net.greenfieldtech.cloudonixsdk.utils.SDKLogger;

import java.io.IOException;
import java.io.InputStream;

import static net.greenfieldtech.cloudonixsdk.api.models.RegistrationData.TransportType.TRANSPORT_TYPE_UDP;

public class VoipImplClient implements IVoIPObserver {

    private static final String TAG = VoipImplClient.class.getSimpleName();

    private String callKey = null;
    private SimpleActivityEvents activityEvents;
    private CloudonixSDKClient cloudonixClient;

    public VoipImplClient(Context ctx) {
        String license = licenseToString(ctx);
        cloudonixClient = CloudonixSDKClient.getInstance(license);
        cloudonixClient.addEventsListener(this);
        cloudonixClient.setLogLevel(SDKLogger.LOG_LEVEL_ALL);
        if (!cloudonixClient.checkBinder()) {
            cloudonixClient.bind(ctx); // will cause onSipStarted to be call

//            Show notification during active call
//            Intent will be processed when user clicks on notification
//            Intent intent = new Intent(ctx, SimpleButtonActivity.class);
//            cloudonixClient.setNotificationResources(intent, R.mipmap.ic_launcher, "Title",
//                    "SDK is running and has an active call");
        }
    }

    // converts license file to String
    private static String licenseToString(Context context) {
        InputStream input = context.getResources().openRawResource(R.raw.cloudonix_license_key);
        try {
            byte[] inputArray = CryptUtils.convertStreamToByteArray(input);
            return new String(inputArray);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public void onLicense(LicensingState licensingState, String description) {
        if (licensingState != LicensingState.LICENSING_SUCCESS) {
            Log.e(TAG, "License error: " + description);
            return;
        }

        cloudonixClient.setConfig(ConfigurationKey.USER_AGENT, "MyApp/1.0");
        cloudonixClient.setConfiguration(new RegistrationData() {{
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
        cloudonixClient.shutdown();
    }

    public void setCallbacksListener(SimpleActivityEvents callbacksListener) {
        activityEvents = callbacksListener;
    }

    public void removeCallbacksListener() {
        activityEvents = null;
    }

    @Override
    public void onSipStarted() {
        Log.d(TAG, "Sip is Started");
        cloudonixClient.registerAccount();
        if (activityEvents != null) {
			activityEvents.onConnectState(true);
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
                if (activityEvents != null) {
                    activityEvents.onConnectState(true);
                }
                break;
            case REGISTRATION_ERROR_CREDENTIALS:
                Log.d(TAG, "auth error, expire: " + expiry);
                if (activityEvents != null) {
                    activityEvents.onConnectState(false);
                }
                break;
            case REGISTRATION_UNREGISTERED:
                Log.d(TAG, "No longer registered, expire: " + expiry);
                if (activityEvents != null) {
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
                if (activityEvents != null) {
                    activityEvents.onCallRinging();
                }
                break;
            case CALL_STATE_EARLY:
                Log.d(TAG, "onEarlyMedia: " + key + " Number: " + contactUrl);
                if (activityEvents != null) {
                    activityEvents.onCallEarlyMedia();
                }
                break;
            case CALL_STATE_MEDIAACTIVE:
                Log.d(TAG, "onMediaActive: " + key + " Number: " + contactUrl);
                break;
            case CALL_STATE_CONFIRMED:
                Log.d(TAG, "onConfirmed: " + key + " Number: " + contactUrl);
                if (activityEvents != null) {
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
                if (activityEvents != null) {
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
        Log.d(TAG, "onSipStopped");
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
        cloudonixClient.dial(number);
        return false;
    }

    public void hangup() {
        cloudonixClient.hangup(callKey);
    }

    public void askRecordAudioPermissions(Activity activity) {
        if (activity.checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "Permission " + Manifest.permission.RECORD_AUDIO
                            + " is not granted. Please accept it for the app to work correctly.",
                    Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, 100);
        }
    }

}