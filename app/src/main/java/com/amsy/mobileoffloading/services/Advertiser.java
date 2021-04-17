package com.amsy.mobileoffloading.services;

import android.content.Context;
import android.util.Log;

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

    public void start(String workerId, ConnectionLifecycleCallback connCallback) {
        Nearby.getConnectionsClient(context)
                .startAdvertising(workerId, context.getPackageName(), connCallback, advtOptions)
                .addOnSuccessListener((unused) -> {
                    Log.d("WORKER", "Advertising to other devices");
                })
                .addOnFailureListener((Exception e) -> {
                    Log.d("WORKER", "Advertising Failed");
                    e.printStackTrace();
                });
    }

    public void stop() {
        Nearby.getConnectionsClient(context).stopAdvertising();
    }
}
