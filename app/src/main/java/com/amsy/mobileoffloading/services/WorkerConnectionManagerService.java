package com.amsy.mobileoffloading.services;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.amsy.mobileoffloading.callback.WorkerConnectionListener;
import com.amsy.mobileoffloading.callback.PayloadListener;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.tasks.Task;

import java.util.HashSet;

public class WorkerConnectionManagerService {

    private static WorkerConnectionManagerService workerConnectionManagerService;
    private Context context;

    private ConnectionLifecycleCallback connectionLifecycleCallback;
    private HashSet<WorkerConnectionListener> workerConnectionListenerSet = new HashSet<>();

    private HashSet<PayloadListener> payloadListenersSet = new HashSet<>();

    public WorkerConnectionManagerService(Context context) {
        this.context = context;
        this.connectionLifecycleCallback = new ConnectionLifecycleCallback() {
            @Override
            public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
                for (WorkerConnectionListener workerConnectionListener : workerConnectionListenerSet) {
                    try {
                        workerConnectionListener.onConnectionInitiated(endpointId, connectionInfo);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
                for (WorkerConnectionListener workerConnectionListener : workerConnectionListenerSet) {
                    try {
                        workerConnectionListener.onConnectionResult(endpointId, connectionResolution);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onDisconnected(@NonNull String endpointId) {
                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show();
                for (WorkerConnectionListener workerConnectionListener : workerConnectionListenerSet) {
                    try {
                        workerConnectionListener.onDisconnected(endpointId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public static WorkerConnectionManagerService getInstance(Context context) {
        if (workerConnectionManagerService == null) {
            workerConnectionManagerService = new WorkerConnectionManagerService(context);
        }

        return workerConnectionManagerService;
    }

    public void requestConnection(String endpointId, String clientId) {
        Nearby.getConnectionsClient(context)
                .requestConnection(clientId, endpointId, connectionLifecycleCallback)
                .addOnSuccessListener(unused -> {
                    Log.d("NEARBYCONNCTNMGR", "CONNECTION REQUESTED");
                })
                .addOnFailureListener((Exception e) -> {
                    Log.d("NEARBYCONNCTNMGR", "CONNECTION FAILED");
                    e.printStackTrace();
                });
    }

    public void acceptConnection(String endpointId) {
        Nearby.getConnectionsClient(context).acceptConnection(endpointId, new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                for (PayloadListener payloadListener : payloadListenersSet) {
                    try {
                        payloadListener.onPayloadReceived(endpointId, payload);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

            }
        });
    }

    public void rejectConnection(String endpointId) {
        Nearby.getConnectionsClient(context).rejectConnection(endpointId);
    }

    public void disconnectFromEndpoint(String endpointId) {
        Nearby.getConnectionsClient(context).disconnectFromEndpoint(endpointId);
    }

    public Task<Void> advertise(String clientId, AdvertisingOptions advertisingOptions) {
        return Nearby.getConnectionsClient(context)
                .startAdvertising(clientId, context.getPackageName(), connectionLifecycleCallback, advertisingOptions)
                .addOnFailureListener((Exception e) -> {
                    e.printStackTrace();
                });
    }

    public boolean registerPayloadListener(PayloadListener payloadListener) {
        if (payloadListener != null) {
            return payloadListenersSet.add(payloadListener);
        }
        return false;
    }

    public boolean registerClientConnectionListener(WorkerConnectionListener workerConnectionListener) {
        if (workerConnectionListener != null) {
            return workerConnectionListenerSet.add(workerConnectionListener);
        }
        return false;
    }

    public boolean unregisterPayloadListener(PayloadListener payloadListener) {
        if (payloadListener != null) {
            return payloadListenersSet.remove(payloadListener);
        }
        return false;
    }


    public boolean unregisterClientConnectionListener(WorkerConnectionListener workerConnectionListener) {
        if (workerConnectionListener != null) {
            return workerConnectionListenerSet.remove(workerConnectionListener);
        }
        return false;
    }


}
