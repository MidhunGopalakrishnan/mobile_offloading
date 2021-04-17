package com.amsy.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.amsy.mobileoffloading.adapters.ConnectedDevicesAdapter;
import com.amsy.mobileoffloading.callback.ClientConnectionListener;
import com.amsy.mobileoffloading.callback.PayloadListener;
import com.amsy.mobileoffloading.entities.ClientPayLoad;
import com.amsy.mobileoffloading.entities.ConnectedDevice;
import com.amsy.mobileoffloading.entities.DeviceStatistics;
import com.amsy.mobileoffloading.helper.Constants;
import com.amsy.mobileoffloading.helper.PayloadConverter;
import com.amsy.mobileoffloading.services.MasterDiscoveryService;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MasterDiscovery extends AppCompatActivity {

    private Button bDiscoveryComplete;
    private RecyclerView rvConnectedDevices;

    private ConnectedDevicesAdapter connectedDevicesAdapter;
    private List<ConnectedDevice> connectedDevices = new ArrayList<>();

    private MasterDiscoveryService masterDiscoveryService;
    private ClientConnectionListener clientConnectionListener;
    private ConnectionLifecycleCallback connectionLifecycleCallback;

    private PayloadListener payloadListener;
    private PayloadCallback payloadCallback;


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

        payloadListener = new PayloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {

                try {
                    ClientPayLoad tPayload = PayloadConverter.fromPayload(payload);
                    if (tPayload.getTag().equals(Constants.PayloadTags.DEVICE_STATS)) {
                        updateDeviceStats(endpointId, (DeviceStatistics) tPayload.getData());
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        };
        payloadCallback = new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                try {
                    payloadListener.onPayloadReceived(endpointId, payload);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

            }
        };

        clientConnectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, payloadCallback);
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {
                int statusCode = connectionResolution.getStatus().getStatusCode();

                if (statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    updateConnectedDeviceRequestStatus(endpointId, Constants.RequestStatus.ACCEPTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED) {
                    updateConnectedDeviceRequestStatus(endpointId, Constants.RequestStatus.REJECTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_ERROR) {
                    removeConnectedDevice(endpointId);
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                removeConnectedDevice(endpointId);
            }
        };

        connectionLifecycleCallback = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
                    try {
                        clientConnectionListener.onConnectionInitiated(endpointId, connectionInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

            }

            @Override
            public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
                    try {
                        clientConnectionListener.onConnectionResult(endpointId, connectionResolution);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }

            @Override
            public void onDisconnected(@NonNull String endpointId) {
                Toast.makeText(getApplicationContext(), "DISCONNECTED", Toast.LENGTH_SHORT).show();
                    try {
                        clientConnectionListener.onDisconnected(endpointId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
            }
        };

        startMasterDiscovery();

    }

    private void updateConnectedDeviceRequestStatus(String endpointId, String status) {
        for (int i = 0; i < connectedDevices.size(); i++) {
            if (connectedDevices.get(i).getEndpointId().equals(endpointId)) {
                connectedDevices.get(i).setRequestStatus(status);
                connectedDevicesAdapter.notifyItemChanged(i);
            }
        }
    }

    private void startMasterDiscovery() {
        EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                Log.d("MASTER", "ENDPOINT FOUND");
                Log.d("MASTER", endpointId);
                Log.d("MASTER", discoveredEndpointInfo.getServiceId() + " " + discoveredEndpointInfo.getEndpointName());

                ConnectedDevice connectedDevice = new ConnectedDevice();
                connectedDevice.setEndpointId(endpointId);
                connectedDevice.setEndpointName(discoveredEndpointInfo.getEndpointName());
                connectedDevice.setRequestStatus(Constants.RequestStatus.PENDING);
                connectedDevice.setDeviceStats(new DeviceStatistics());

                connectedDevices.add(connectedDevice);
                connectedDevicesAdapter.notifyItemChanged(connectedDevices.size() - 1);

                Nearby.getConnectionsClient(getApplicationContext())
                        .requestConnection("MASTER", endpointId, connectionLifecycleCallback)
                        .addOnSuccessListener(unused -> {
                            Log.d("OFLOD", "CONNECTION REQUESTED");
                        })
                        .addOnFailureListener((Exception e) -> {
                            Log.d("OFLOD", "CONNECTION FAILED");
                            e.printStackTrace();
                        });
            }

            @Override
            public void onEndpointLost(@NonNull String endpointId) {
                Log.d("MASTER", "ENDPOINT LOST");
                Log.d("MASTER", endpointId);

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

    private void updateDeviceStats(String endpointId, DeviceStatistics deviceStats) {
        for (int i = 0; i < connectedDevices.size(); i++) {
            if (connectedDevices.get(i).getEndpointId().equals(endpointId)) {
                connectedDevices.get(i).setDeviceStats(deviceStats);
                connectedDevices.get(i).setRequestStatus(Constants.RequestStatus.ACCEPTED);
                connectedDevicesAdapter.notifyItemChanged(i);
            }
        }
    }
}