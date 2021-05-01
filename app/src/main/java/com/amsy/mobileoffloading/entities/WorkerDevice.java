package com.amsy.mobileoffloading.entities;

import java.io.Serializable;

public class WorkerDevice implements Serializable {
    private String endpointId;
    private String endpointName;
    private WorkerDeviceStatistics deviceStats;
    private String requestStatus;


    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public WorkerDeviceStatistics getDeviceStats() {
        return deviceStats;
    }

    public void setDeviceStats(WorkerDeviceStatistics deviceStats) {
        this.deviceStats = deviceStats;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }
}
