package com.amsy.mobileoffloading.services;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.Strategy;

public class Advertiser {

    private Context context;
    private AdvertisingOptions advtOptions;

    public Advertiser(Context context) {
        this.context = context;
        this.advtOptions = new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
    }

    public void start(String workerId, TextView text) {
        NearbyConnectionsManager.getInstance(context).advertise(workerId, advtOptions, text);
    }

    public void stop() {
        Nearby.getConnectionsClient(context).stopAdvertising();
    }
}
