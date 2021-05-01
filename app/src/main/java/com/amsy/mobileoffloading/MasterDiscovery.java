package com.amsy.mobileoffloading;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.amsy.mobileoffloading.adapters.DeviceConnectionAdapter;
import com.amsy.mobileoffloading.callback.WorkerConnectionListener;
import com.amsy.mobileoffloading.callback.PayloadListener;
import com.amsy.mobileoffloading.entities.ClientPayload;
import com.amsy.mobileoffloading.entities.WorkerDevice;
import com.amsy.mobileoffloading.entities.WorkerDeviceStatistics;
import com.amsy.mobileoffloading.helper.GobalConstants;
//import com.amsy.mobileoffloading.helper.FlushToFile;
//import com.amsy.mobileoffloading.helper.MatrixDS;
import com.amsy.mobileoffloading.helper.PayloadInterface;
import com.amsy.mobileoffloading.services.WorkerConnectorService;
import com.amsy.mobileoffloading.services.MasterDiscoveryService;
import com.amsy.mobileoffloading.services.WorkerConnectionManagerService;
import com.amsy.mobileoffloading.services.WorkAllocatorService;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MasterDiscovery extends AppCompatActivity {

    private RecyclerView rvConnectedDevices;
    private DeviceConnectionAdapter deviceConnectionAdapter;
    private List<WorkerDevice> workerDevices = new ArrayList<>();
    private MasterDiscoveryService masterDiscoveryService;
    private WorkerConnectionListener workerConnectionListener;
    private PayloadListener payloadListener;

    @Override
    protected void onPause() {
        super.onPause();
        setStatus("Stopped", false);
        masterDiscoveryService.stop();
        WorkerConnectionManagerService.getInstance(getApplicationContext()).unregisterPayloadListener(payloadListener);
        WorkerConnectionManagerService.getInstance(getApplicationContext()).unregisterClientConnectionListener(workerConnectionListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startMasterDiscovery();
        WorkerConnectionManagerService.getInstance(getApplicationContext()).registerPayloadListener(payloadListener);
        WorkerConnectionManagerService.getInstance(getApplicationContext()).registerClientConnectionListener(workerConnectionListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_discovery);

        rvConnectedDevices = findViewById(R.id.rv_connected_devices);
        deviceConnectionAdapter = new DeviceConnectionAdapter(this, workerDevices);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvConnectedDevices.setLayoutManager(linearLayoutManager);

        rvConnectedDevices.setAdapter(deviceConnectionAdapter);
        deviceConnectionAdapter.notifyDataSetChanged();

        payloadListener = new PayloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                Log.d("MASTER_DISCOVERY", "PayloadListener -  onPayloadReceived");
                try {
                    ClientPayload tPayload = PayloadInterface.fromPayload(payload);
                    if (tPayload.getTag().equals(GobalConstants.PayloadTags.DEVICE_STATS)) {
                        updateDeviceStats(endpointId, (WorkerDeviceStatistics) tPayload.getData());
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }


            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {
                Log.d("MASTER_DISCOVERY", "PayloadListener -  onPayloadTransferUpdate");
            }
        };


        workerConnectionListener = new WorkerConnectionListener() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionInitiated");
                WorkerConnectionManagerService.getInstance(getApplicationContext()).acceptConnection(endpointId);
            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {

                Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult" + endpointId);

                int statusCode = connectionResolution.getStatus().getStatusCode();
                if (statusCode == ConnectionsStatusCodes.STATUS_OK) {
                    Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult - ACCEPTED");
                    updateConnectedDeviceRequestStatus(endpointId, GobalConstants.RequestStatus.ACCEPTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED) {
                    Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult - REJECTED");
                    updateConnectedDeviceRequestStatus(endpointId, GobalConstants.RequestStatus.REJECTED);
                } else if (statusCode == ConnectionsStatusCodes.STATUS_ERROR) {
                    Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onConnectionResult - ERROR");
                    removeConnectedDevice(endpointId, true);
                }
            }

            @Override
            public void onDisconnected(String endpointId) {
                Log.d("MASTER_DISCOVERY", "clientConnectionListener -  onDisconnected " + endpointId);
                removeConnectedDevice(endpointId, true);
            }
        };


    }


    public void assignTasks(View view) {
        ArrayList<WorkerDevice> readyDevices = getDevicesInReadyState();
        if (readyDevices.size() == 0) {
            Toast.makeText(getApplicationContext(), "No worker Available at the moment", Toast.LENGTH_LONG).show();
            onBackPressed();
        } else {
            masterDiscoveryService.stop();
            startMasterActivity(readyDevices);
            finish();
        }
    }

    private ArrayList<WorkerDevice> getDevicesInReadyState() {
        ArrayList<WorkerDevice> res = new ArrayList<>();
        for (int i = 0; i < workerDevices.size(); i++) {
            if (workerDevices.get(i).getRequestStatus().equals(GobalConstants.RequestStatus.ACCEPTED)) {
                if (workerDevices.get(i).getDeviceStats().getBatteryLevel() > WorkAllocatorService.ThresholdsHolder.MINIMUM_BATTERY_LEVEL) {
                    res.add(workerDevices.get(i));
                } else {
                    ClientPayload tPayload = new ClientPayload();
                    tPayload.setTag(GobalConstants.PayloadTags.DISCONNECTED);

                    WorkerConnectorService.sendToDevice(getApplicationContext(), workerDevices.get(i).getEndpointId(), tPayload);
                }
            } else {
                Log.d("MASTER_DISCOVERY", "LOOPING");
                ClientPayload tPayload = new ClientPayload();
                tPayload.setTag(GobalConstants.PayloadTags.DISCONNECTED);

                WorkerConnectorService.sendToDevice(getApplicationContext(), workerDevices.get(i).getEndpointId(), tPayload);
            }

        }
        return res;
    }

    private void updateConnectedDeviceRequestStatus(String endpointId, String status) {
        for (int i = 0; i < workerDevices.size(); i++) {
            if (workerDevices.get(i).getEndpointId().equals(endpointId)) {
                workerDevices.get(i).setRequestStatus(status);
                Log.d("MASTER_DISCOVERY", "Status of end point set to "+status);
                deviceConnectionAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void startMasterDiscovery() {
                Log.d("MASTER_DISCOVERY", "Starting Master Discovery");
        EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
            @Override
            public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                Log.d("MASTER_DISCOVERY", "ENDPOINT FOUND " +endpointId);
                Log.d("MASTER_DISCOVERY", endpointId);
                Log.d("MASTER_DISCOVERY", discoveredEndpointInfo.getServiceId() + " " + discoveredEndpointInfo.getEndpointName());

                WorkerDevice workerDevice = new WorkerDevice();
                workerDevice.setEndpointId(endpointId);
                workerDevice.setEndpointName(discoveredEndpointInfo.getEndpointName());
                workerDevice.setRequestStatus(GobalConstants.RequestStatus.PENDING);
                workerDevice.setDeviceStats(new WorkerDeviceStatistics());

                workerDevices.add(workerDevice);
                deviceConnectionAdapter.notifyItemChanged(workerDevices.size() - 1);

                Log.d("MASTER_DISCOVERY", "Added end point to connected devices : " +endpointId);

                WorkerConnectionManagerService.getInstance(getApplicationContext()).requestConnection(endpointId, "MASTER");
                Log.d("MASTER_DISCOVERY", "Requested connection for : " +endpointId);

            }

            @Override
            public void onEndpointLost(@NonNull String endpointId) {
                Log.d("MASTER_DISCOVERY", "ENDPOINT LOST");
                Log.d("MASTER_DISCOVERY", endpointId);
                removeConnectedDevice(endpointId, false);
            }
        };

        masterDiscoveryService = new MasterDiscoveryService(this);
        masterDiscoveryService.start(endpointDiscoveryCallback)
                .addOnSuccessListener((unused) -> {
                    setStatus("Searching...", true);
                })
                .addOnFailureListener(command -> {
                    if (((ApiException) command).getStatusCode() == 8002) {
                        setStatus("Still Searching...", true);
                    } else {
                        setStatus("Discovering Failed", false);
                        finish();
                    }
                    command.printStackTrace();
                });
        ;
    }

    private void removeConnectedDevice(String endpointId, boolean forceRemove) {

        for (int i = 0; i < workerDevices.size(); i++) {
            boolean checkStatus = forceRemove ? true :  !workerDevices.get(i).getRequestStatus().equals(GobalConstants.RequestStatus.ACCEPTED);
            if (workerDevices.get(i).getEndpointId().equals(endpointId) && checkStatus) {
                Log.d("MASTER_DISCOVERY", "Removed end point from connected devices " + endpointId );
                workerDevices.remove(i);
                deviceConnectionAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void updateDeviceStats(String endpointId, WorkerDeviceStatistics deviceStats) {
        canAssign(deviceStats);
        for (int i = 0; i < workerDevices.size(); i++) {
            if (workerDevices.get(i).getEndpointId().equals(endpointId)) {
                workerDevices.get(i).setDeviceStats(deviceStats);

//                Toast.makeText(getApplicationContext(), "Success: updated battery level: can proceed", Toast.LENGTH_SHORT).show();
                workerDevices.get(i).setRequestStatus(GobalConstants.RequestStatus.ACCEPTED);
                deviceConnectionAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    void canAssign(WorkerDeviceStatistics deviceStats) {
        Button assignButton = findViewById(R.id.assignTask);
        assignButton.setVisibility(deviceStats.getBatteryLevel() > WorkAllocatorService.ThresholdsHolder.MINIMUM_BATTERY_LEVEL ? View.VISIBLE : View.INVISIBLE);
    }

    void setStatus(String text, boolean search) {
        TextView disc = findViewById(R.id.discovery);
        disc.setText(text);
        ProgressBar pb = findViewById(R.id.progressBar);
        pb.setIndeterminate(search);
    }

    @Override
    public void finish() {
        super.finish();
        masterDiscoveryService.stop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void startMasterActivity(ArrayList<WorkerDevice> workerDevices) {
        Intent intent = new Intent(getApplicationContext(), MasterActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(GobalConstants.CONNECTED_DEVICES, workerDevices);
        intent.putExtras(bundle);
        startActivity(intent);
        finish();
    }

}