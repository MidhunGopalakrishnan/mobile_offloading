package com.amsy.mobileoffloading;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.widget.Button;

public class MasterDiscovery extends AppCompatActivity {

    private Button bDiscoveryComplete;
    private RecyclerView rvConnectedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_discovery);

        bDiscoveryComplete = findViewById(R.id.b_discovery_done);
        rvConnectedDevices = findViewById(R.id.rv_connected_devices);


    }
}