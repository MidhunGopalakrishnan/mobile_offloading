package com.amsy.mobileoffloading;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.amsy.mobileoffloading.helper.Constants;
import com.amsy.mobileoffloading.services.DeviceStatisticsPublisher;

public class WorkerComputation extends AppCompatActivity {
    private String masterId;
    private DeviceStatisticsPublisher deviceStatsPublisher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_computation);
        Bundle bundle = getIntent().getExtras();
        this.masterId = bundle.getString(Constants.MASTER_ENDPOINT_ID);
        startDeviceStatsPublisher();
    }

    private void startDeviceStatsPublisher() {
        deviceStatsPublisher = new DeviceStatisticsPublisher(getApplicationContext(), masterId);
        deviceStatsPublisher.start();
    }
}