package com.amsy.mobileoffloading.services;

import android.content.Context;

import com.amsy.mobileoffloading.callback.PayloadListener;
import com.amsy.mobileoffloading.callback.WorkerStatusListener;
import com.amsy.mobileoffloading.entities.ClientPayload;
import com.amsy.mobileoffloading.entities.WorkerDeviceStatistics;
import com.amsy.mobileoffloading.entities.WorkDataStatus;
import com.amsy.mobileoffloading.helper.GobalConstants;
import com.amsy.mobileoffloading.helper.PayloadInterface;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;

public class WorkerStatusManager {

    private Context context;
    private String endpointId;
    private PayloadListener payloadListener;
    private WorkerStatusListener workerStatusListener;

    public WorkerStatusManager(Context context, String endpointId, WorkerStatusListener workerStatusListener) {
        this.context = context;
        this.endpointId = endpointId;
        this.workerStatusListener = workerStatusListener;
    }

    public void start() {
        payloadListener = new PayloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                try {
                    ClientPayload tPayload = (ClientPayload) PayloadInterface.fromPayload(payload);
                    String payloadTag = tPayload.getTag();

                    if (payloadTag.equals(GobalConstants.PayloadTags.WORK_STATUS)) {
                        if (workerStatusListener != null) {
                            workerStatusListener.onWorkStatusReceived(endpointId, (WorkDataStatus) tPayload.getData());
                        }
                    } else if (payloadTag.equals(GobalConstants.PayloadTags.DEVICE_STATS)) {
                        if (workerStatusListener != null) {
                            workerStatusListener.onDeviceStatsReceived(endpointId, (WorkerDeviceStatistics) tPayload.getData());
                        }
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {

            }
        };

        WorkerConnectionManagerService.getInstance(context).registerPayloadListener(payloadListener);
        WorkerConnectionManagerService.getInstance(context).acceptConnection(endpointId);
    }

    public void stop() {
        WorkerConnectionManagerService.getInstance(context).unregisterPayloadListener(payloadListener);
    }

}
