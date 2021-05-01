package com.amsy.mobileoffloading.entities;

import java.io.Serializable;

public class Worker implements Serializable {

    private String endpointId, endpointName;
    private WorkerDeviceStatistics workerDeviceStatistics;
    private WorkDataStatus workDataStatus;

    private int workQuantity;
    private float distanceFromMaster;

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

    public WorkDataStatus getWorkStatus() {
        return workDataStatus;
    }

    public void setWorkStatus(WorkDataStatus workStatus) {
        this.workDataStatus = workStatus;
    }

    public WorkerDeviceStatistics getDeviceStats() {
        return workerDeviceStatistics;
    }

    public void setDeviceStats(WorkerDeviceStatistics deviceStats) {
        this.workerDeviceStatistics = deviceStats;
    }

    public int getWorkAmount() {
        return workQuantity;
    }

    public void setWorkAmount(int workAmount) {
        this.workQuantity = workAmount;
    }

    public float getDistanceFromMaster() {
        return distanceFromMaster;
    }

    public void setDistanceFromMaster(float distanceFromMaster) {
        this.distanceFromMaster = distanceFromMaster;
    }

}
