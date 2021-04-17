package com.amsy.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import com.amsy.mobileoffloading.adapters.ConnectedDevicesAdapter;
import com.amsy.mobileoffloading.entities.ConnectedDevice;
import com.amsy.mobileoffloading.entities.DeviceStatistics;
import com.amsy.mobileoffloading.helper.Constants;
import com.amsy.mobileoffloading.services.MasterDiscoveryService;
import com.amsy.mobileoffloading.services.NearbyConnectionsManager;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;

import java.util.ArrayList;
import java.util.List;

public class MasterDiscovery extends AppCompatActivity {

    private Button bDiscoveryComplete;
    private RecyclerView rvConnectedDevices;

    private ConnectedDevicesAdapter connectedDevicesAdapter;
    private List<ConnectedDevice> connectedDevices = new ArrayList<>();

    private MasterDiscoveryService masterDiscoveryService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_discovery);

        bDiscoveryComplete = findViewById(R.id.b_discovery_done);
        rvConnectedDevices = findViewById(R.id.rv_connected_devices);

        connectedDevicesAdapter = new ConnectedDevicesAdapter(this, connectedDevices);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvConnectedDevices.setLayoutManager(linearLayoutManager);

        rvConnectedDevices.setAdapter(connectedDevicesAdapter);
        connectedDevicesAdapter.notifyDataSetChanged();

        //TODO: listeners are not initialized
        startMasterDiscovery();

    }

    private void startMasterDiscovery() {
        EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                Log.d("OFLOD", "ENDPOINT FOUND");
                Log.d("OFLOD", endpointId);
                Log.d("OFLOD", discoveredEndpointInfo.getServiceId() + " " + discoveredEndpointInfo.getEndpointName());

                ConnectedDevice connectedDevice = new ConnectedDevice();
                connectedDevice.setEndpointId(endpointId);
                connectedDevice.setEndpointName(discoveredEndpointInfo.getEndpointName());
                connectedDevice.setRequestStatus(Constants.RequestStatus.PENDING);
                connectedDevice.setDeviceStats(new DeviceStatistics());

                connectedDevices.add(connectedDevice);
                connectedDevicesAdapter.notifyItemChanged(connectedDevices.size() - 1);


                NearbyConnectionsManager.getInstance(getApplicationContext()).requestConnection(endpointId, "MASTER");
            }

            @Override
            public void onEndpointLost(@NonNull String endpointId) {
                Log.d("OFLOD", "ENDPOINT LOST");
                Log.d("OFLOD", endpointId);

                removeConnectedDevice(endpointId);
            }
        };

        masterDiscoveryService = new MasterDiscoveryService(this);
        masterDiscoveryService.start(endpointDiscoveryCallback);
    }

    private void removeConnectedDevice(String endpointId) {
        for (int i = 0; i < connectedDevices.size(); i++) {
            if (connectedDevices.get(i).getEndpointId().equals(endpointId)) {
                connectedDevices.remove(i);
                connectedDevicesAdapter.notifyItemChanged(i);
                i--;
            }
        }
    }
}