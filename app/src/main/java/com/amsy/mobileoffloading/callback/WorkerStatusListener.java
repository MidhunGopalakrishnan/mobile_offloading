package com.amsy.mobileoffloading.callback;

import com.amsy.mobileoffloading.entities.WorkerDeviceStatistics;
import com.amsy.mobileoffloading.entities.WorkDataStatus;

public interface WorkerStatusListener {

    void onWorkStatusReceived(String endpointId, WorkDataStatus workDataStatus);

     void onDeviceStatsReceived(String endpointId, WorkerDeviceStatistics deviceStats);

}
