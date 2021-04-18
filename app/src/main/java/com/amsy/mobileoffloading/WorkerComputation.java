package com.amsy.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.amsy.mobileoffloading.helper.Constants;
import com.amsy.mobileoffloading.services.DeviceStatisticsPublisher;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

public class WorkerComputation extends AppCompatActivity {
    private String masterId;
    private DeviceStatisticsPublisher deviceStatsPublisher;
    private ConnectionLifecycleCallback connectionListener;
    private PayloadCallback payloadCallback;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_computation);



        connectionListener = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String id, ConnectionInfo connectionInfo) {

            }

            @Override
            public void onConnectionResult(String id, ConnectionResolution connectionResolution) {

            }

            @Override
            public void onDisconnected(String id) {

            }
        };

        extractBundle();
        startDeviceStatsPublisher();
        connectToMaster();

    }
    private void extractBundle() {
        Bundle bundle = getIntent().getExtras();
        this.masterId = bundle.getString(Constants.MASTER_ENDPOINT_ID);
    }

    private void startDeviceStatsPublisher() {
        deviceStatsPublisher = new DeviceStatisticsPublisher(getApplicationContext(), masterId);
        deviceStatsPublisher.start();
    }
    private void connectToMaster() {
        payloadCallback = new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {

            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

            }
        };
        Nearby.getConnectionsClient(this.getApplicationContext()).acceptConnection(masterId, payloadCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }
}