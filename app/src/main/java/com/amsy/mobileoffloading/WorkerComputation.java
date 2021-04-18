package com.amsy.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import com.amsy.mobileoffloading.callback.ClientConnectionListener;
import com.amsy.mobileoffloading.callback.PayloadListener;
import com.amsy.mobileoffloading.helper.Constants;
import com.amsy.mobileoffloading.services.Advertiser;
import com.amsy.mobileoffloading.services.DeviceStatisticsPublisher;
import com.amsy.mobileoffloading.services.NearbyConnectionsManager;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

public class WorkerComputation extends AppCompatActivity {
    private String masterId;
    private DeviceStatisticsPublisher deviceStatsPublisher;
    private ClientConnectionListener connectionListener;
    private PayloadListener payloadCallback;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_computation);
        extractBundle();
        startDeviceStatsPublisher();
        setConnectionCallback();
        connectToMaster();
    }
    private void extractBundle() {
        Bundle bundle = getIntent().getExtras();
        this.masterId = bundle.getString(Constants.MASTER_ENDPOINT_ID);
    }

    private void startDeviceStatsPublisher() {
        deviceStatsPublisher = new DeviceStatisticsPublisher(getApplicationContext(), masterId);
    }

    private void connectToMaster() {
        payloadCallback = new PayloadListener() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {

            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

            }
        };
        NearbyConnectionsManager.getInstance(getApplicationContext()).acceptConnection(masterId);
    }

    private void setConnectionCallback() {
        connectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String id, ConnectionInfo connectionInfo) {
            }

            @Override
            public void onConnectionResult(String id, ConnectionResolution connectionResolution) {
            }

            @Override
            public void onDisconnected(String id) {
                navBack();
            }
        };
    }

    private void navBack() {
        Intent intent = new Intent(getApplicationContext(), WorkerAdvertisement.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NearbyConnectionsManager.getInstance(getApplicationContext()).registerPayloadListener(payloadCallback);
        NearbyConnectionsManager.getInstance(getApplicationContext()).registerClientConnectionListener(connectionListener);
        deviceStatsPublisher.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterPayloadListener(payloadCallback);
        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(connectionListener);
        deviceStatsPublisher.stop();
    }

    @Override
    public void finish() {
        super.finish();
        Advertiser.stopAdvertising(this.getApplicationContext());
    }
}