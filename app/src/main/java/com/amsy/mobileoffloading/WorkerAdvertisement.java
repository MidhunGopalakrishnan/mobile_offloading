package com.amsy.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

public class WorkerAdvertisement extends AppCompatActivity {
    private  Advertiser advertiser;
    private String workerId;
    private String masterId;
    private ConnectionLifecycleCallback connectionListener;
    private PayloadCallback payloadCallback;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_advertisement);
        advertiser = new Advertiser(this.getApplicationContext());
        workerId =  Build.MANUFACTURER + " " + Build.MODEL;
        setCallback();
    }

    void setCallback() {
        connectionListener = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String id, ConnectionInfo connectionInfo) {
                Log.d("WORKER", "Connection Init");
                Log.d("WORKER", id);
                Log.d("WORKER", connectionInfo.getEndpointName() + " " + connectionInfo.getAuthenticationToken());
                masterId = id;
            }

            @Override
            public void onConnectionResult(String id, ConnectionResolution connectionResolution) {
                Log.d("WORKER", "Connection Accepted by master: " + id);
                Log.d("WORKER", connectionResolution.getStatus().toString() + "");
                Toast.makeText(WorkerAdvertisement.this, "Connected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisconnected(String id) {
                Log.d("WORKER", "DISCONNECTED");
                Log.d("WORKER", id);
                Toast.makeText(WorkerAdvertisement.this, "Disconnected", Toast.LENGTH_SHORT).show();
            }
        };
        payloadCallback = new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                Log.d("WORKER", "Payload Received");
            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                Log.d("WORKER", "Payload Transferring...");
            }
        };
    }

    void acceptConnection() {
        Nearby.getConnectionsClient(this.getApplicationContext()).acceptConnection(masterId, payloadCallback);
        advertiser.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        advertiser.start(workerId, connectionListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        advertiser.stop();
    }
}