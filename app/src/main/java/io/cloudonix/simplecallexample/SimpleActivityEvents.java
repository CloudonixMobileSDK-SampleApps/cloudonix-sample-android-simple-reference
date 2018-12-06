package io.cloudonix.simplecallexample;

interface SimpleActivityEvents {
    void onVoIPStart();

    void onVoIPError();

    void onConnectState(boolean connected);

    void onCallRinging();

    void onCallEarlyMedia();

    void onCallConnected();

    void onCallDisconnected();
}
