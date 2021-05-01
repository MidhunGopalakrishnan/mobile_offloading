package com.amsy.mobileoffloading.services;

import android.content.Context;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.Task;

public class WorkerAdvertiserService {

    private Context context;
    private AdvertisingOptions advtOptions;

    public WorkerAdvertiserService(Context context) {
        this.context = context;
        this.advtOptions = new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
    }

    public Task<Void> start(String workerId) {
       return WorkerConnectionManagerService.getInstance(context).advertise(workerId, advtOptions);
    }

    public void stop() {
        Nearby.getConnectionsClient(context).stopAdvertising();
    }

}
