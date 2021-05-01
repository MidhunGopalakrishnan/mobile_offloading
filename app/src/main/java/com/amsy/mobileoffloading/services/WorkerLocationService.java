package com.amsy.mobileoffloading.services;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class WorkerLocationService {
    private Context context;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static WorkerLocationService workerLocationService;
    private Location lastAvailableLocation;
    private LocationCallback locationCallback;


    public WorkerLocationService(Context context) {
        this.context = context;
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                lastAvailableLocation = locationResult.getLastLocation();
            }
        };
    }

    public static WorkerLocationService getInstance(Context context) {
        if (workerLocationService == null) {
            workerLocationService = new WorkerLocationService(context);
        }
        return workerLocationService;
    }

    public Location getLastAvailableLocation() {
        return this.lastAvailableLocation;
    }

    @SuppressLint("MissingPermission")
    public void start(int interval) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(interval);
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, context.getMainLooper());
    }

    public void stop() {
        if (this.locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(this.locationCallback);
        }
    }


}