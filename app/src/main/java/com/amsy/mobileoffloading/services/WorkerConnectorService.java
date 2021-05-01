package com.amsy.mobileoffloading.services;

import android.content.Context;

import com.amsy.mobileoffloading.entities.ClientPayload;
import com.amsy.mobileoffloading.helper.PayloadInterface;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;

import java.io.IOException;

public class WorkerConnectorService {
    public static void sendToDevice(Context context, String endpointId, ClientPayload tPayload) {
        try {
            Payload payload = PayloadInterface.toPayload(tPayload);
            WorkerConnectorService.sendToDevice(context, endpointId, payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendToDevice(Context context, String endpointId, byte[] data) {
        Payload payload = Payload.fromBytes(data);
        WorkerConnectorService.sendToDevice(context, endpointId, payload);
    }

    public static void sendToDevice(Context context, String endpointId, Payload payload) {
        Nearby.getConnectionsClient(context).sendPayload(endpointId, payload);
    }
}
