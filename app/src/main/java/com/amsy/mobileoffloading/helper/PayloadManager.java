package com.amsy.mobileoffloading.helper;

import android.content.Context;

import com.amsy.mobileoffloading.entities.ClientPayload;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;

import java.io.IOException;

public class PayloadManager {
    public static void sendPayload(Context context, String endpointId, ClientPayload tPayload) {
        try {
            Payload payload = PayloadInterface.toPayload(tPayload);
            Nearby.getConnectionsClient(context).sendPayload(endpointId, payload);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

