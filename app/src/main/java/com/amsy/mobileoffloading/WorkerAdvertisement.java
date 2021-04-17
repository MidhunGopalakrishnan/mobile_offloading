package com.amsy.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amsy.mobileoffloading.helper.Constants;
import com.amsy.mobileoffloading.services.Advertiser;
import com.amsy.mobileoffloading.services.DeviceStatisticsPublisher;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.tasks.OnSuccessListener;

import eo.view.batterymeter.BatteryMeterView;
import pl.droidsonroids.gif.GifImageView;

public class WorkerAdvertisement extends AppCompatActivity {
    private Advertiser advertiser;
    private String workerId;
    private String masterId;
    private ConnectionLifecycleCallback connectionListener;
    private PayloadCallback payloadCallback;
    private Dialog confirmationDialog;
    private DeviceStatisticsPublisher deviceStatsPublisher;
    private Handler handler;
    private Runnable runnable;
    public boolean isOnline = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_advertisement);
        initialiseDialog();
        advertiser = new Advertiser(this.getApplicationContext());
        workerId =  Build.MANUFACTURER + " " + Build.MODEL;
        deviceStatsPublisher = new DeviceStatisticsPublisher(getApplicationContext(), null);
        setDeviceId("Device ID: " + workerId);

        connectionListener = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(String id, ConnectionInfo connectionInfo) {
                Log.d("WORKER", "Connection Init");
                Log.d("WORKER", id);
                Log.d("WORKER", connectionInfo.getEndpointName() + " " + connectionInfo.getAuthenticationToken());
                masterId = id;
                showDialog(connectionInfo.getEndpointName());
            }

            @Override
            public void onConnectionResult(String id, ConnectionResolution connectionResolution) {
                Log.d("WORKER", "Connection Accepted by master: " + id);
                Log.d("WORKER", connectionResolution.getStatus().toString() + "");
                Toast.makeText(WorkerAdvertisement.this, "Connected", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDisconnected(String id) {
                Log.d("WORKER", "Disconnected");
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

        handler = new Handler(Looper.getMainLooper());
        runnable = () -> {
            refreshCardData();
            handler.postDelayed(runnable, Constants.UPDATE_INTERVAL);
        };
    }

    void setStatusText(String text) {
        TextView st = findViewById(R.id.statusText);
        st.setText(text);
    }
    void setDeviceId(String text) {
        TextView st = findViewById(R.id.deviceId);
        st.setText(text);
    }
    void refreshCardData() {
        TextView st = findViewById(R.id.percentage);
        BatteryMeterView bv = findViewById(R.id.batteryMeter);
        bv.setChargeLevel(DeviceStatisticsPublisher.getBatteryLevel(this));
        bv.setCharging( DeviceStatisticsPublisher.isPluggedIn(this));
        st.setText("Percentage: " + DeviceStatisticsPublisher.getBatteryLevel(this) +"%");
        TextView st2 = findViewById(R.id.plugged);
        st2.setText(String.format("Charging Status: %s", DeviceStatisticsPublisher.isPluggedIn(this) ? "Plugged In" : "Not Charging"));
        if(DeviceStatisticsPublisher.getLocation(this) != null) {
            TextView la = findViewById(R.id.latitude);
            la.setText(String.format("Latitude: %s", DeviceStatisticsPublisher.getLocation(this).getLatitude()));
            TextView lo = findViewById(R.id.longitude);
            lo.setText("Longitude: " + DeviceStatisticsPublisher.getLocation(this).getLongitude());
        } else {
            TextView la = findViewById(R.id.latitude);
            la.setText("Latitude: Not Available");
            TextView lo = findViewById(R.id.longitude);
            lo.setText("Longitude: Not Available" );
        }
        TextView sta = findViewById(R.id.statusText);

        boolean isDisc = sta.getText().toString().contains("Discoverable");
        ImageView online = findViewById(R.id.online);
        online.setVisibility(isDisc ? View.VISIBLE: View.INVISIBLE);
        GifImageView loading = findViewById(R.id.loading);
        loading.setVisibility(isDisc ? View.INVISIBLE: View.VISIBLE);
    }



    void initialiseDialog() {
        confirmationDialog = new Dialog(this);
        confirmationDialog.setContentView(R.layout.confirmation_dialog);
        confirmationDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        confirmationDialog.findViewById(R.id.accept).setOnClickListener(v -> acceptConnection());
        confirmationDialog.findViewById(R.id.reject).setOnClickListener(v -> rejectConnection());;
    }
    void showDialog(String masterInfo) {
       TextView title =  confirmationDialog.findViewById(R.id.dialogText);
        title.setText(String.format("Master(%s) is trying to connect. Do you accept the connection ?", masterInfo));
        confirmationDialog.show();
    }

    void acceptConnection() {
        Nearby.getConnectionsClient(this.getApplicationContext()).acceptConnection(masterId, payloadCallback);
        advertiser.stop();
        confirmationDialog.dismiss();
        startWorkerComputation();
    }
    void rejectConnection() {
        Nearby.getConnectionsClient(this.getApplicationContext()).rejectConnection(masterId);
        confirmationDialog.dismiss();
    }

    @Override
    protected void onResume() {
        setStatusText("Initializing...");
        super.onResume();
        advertiser.start(workerId, connectionListener,findViewById(R.id.statusText) );
        deviceStatsPublisher.start();
        handler.postDelayed(runnable,  Constants.UPDATE_INTERVAL);

    }

    @Override
    protected void onPause() {
        super.onPause();
        advertiser.stop();
        setStatusText("Device is not discoverable");
        deviceStatsPublisher.stop();
        handler.removeCallbacks(runnable);

    }

    private void startWorkerComputation() {
        Intent intent = new Intent(getApplicationContext(), WorkerComputation.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.MASTER_ENDPOINT_ID, masterId);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

    public void simulate(View view) {
        startWorkerComputation();
    }
}