package com.amsy.mobileoffloading;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.amsy.mobileoffloading.adapters.WorkersAdapter;
import com.amsy.mobileoffloading.callback.ClientConnectionListener;
import com.amsy.mobileoffloading.callback.FusedLocationListener;
import com.amsy.mobileoffloading.callback.PayloadListener;
import com.amsy.mobileoffloading.callback.WorkerStatusListener;
import com.amsy.mobileoffloading.entities.ClientPayLoad;
import com.amsy.mobileoffloading.entities.ConnectedDevice;
import com.amsy.mobileoffloading.entities.DeviceStatistics;
import com.amsy.mobileoffloading.entities.WorkInfo;
import com.amsy.mobileoffloading.entities.Worker;
import com.amsy.mobileoffloading.helper.Constants;
import com.amsy.mobileoffloading.helper.Device;
import com.amsy.mobileoffloading.helper.FlushToFile;
import com.amsy.mobileoffloading.helper.MatrixDS;
import com.amsy.mobileoffloading.helper.PayloadConverter;
import com.amsy.mobileoffloading.services.LocationMonitor;
import com.amsy.mobileoffloading.services.LocationService;
import com.amsy.mobileoffloading.services.NearbyConnectionsManager;
import com.amsy.mobileoffloading.services.WorkAllocator;
import com.amsy.mobileoffloading.services.WorkerStatusSubscriber;
import com.app.progresviews.ProgressWheel;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MasterActivity extends AppCompatActivity {

    private RecyclerView rvWorkers;

    private TextView tvWorkFinished, tvWorkTotal;

    private HashMap<String, WorkerStatusSubscriber> workerStatusSubscriberMap = new HashMap<>();

    private ArrayList<Worker> workers = new ArrayList<>();
    private WorkersAdapter workersAdapter;


    /* [row1 x cols1] * [row2 * cols2] */
    private int rows1 = Constants.matrixSize;
    private int cols1 = Constants.matrixSize;
    private int rows2 = Constants.matrixSize;
    private int cols2 = Constants.matrixSize;

    private int[][] matrix1;
    private int[][] matrix2;

    private WorkAllocator workAllocator;

    private LocationService locationService;
    private Location lastAvailableLocation;

    private PayloadListener payloadListener;
    private ClientConnectionListener clientConnectionListener;

    private int workAmount;
    private int totalPartitions;
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        locationService = new LocationService(getApplicationContext());
        Log.d("MasterDiscovery", "Starting computing matrix multiplication on only master");
        computeMatrixMultiplicationOnMaster();
        Log.d("MasterDiscovery", "Completed computing matrix multiplication on only master");

        unpackBundle();
        bindViews();
        setAdapters();
        init();
        setupDeviceBatteryStatsCollector();

    }

    @Override
    public void onBackPressed() {
        for (Worker w : workers) {
            updateWorkerConnectionStatus(w.getEndpointId(), Constants.WorkStatus.DISCONNECTED);
            workAllocator.removeWorker(w.getEndpointId());
            NearbyConnectionsManager.getInstance(getApplicationContext()).disconnectFromEndpoint(w.getEndpointId());
        }
        super.onBackPressed();
    }

    private void init() {
        totalPartitions = rows1 * cols2;
        updateProgress(0);
        matrix1 = MatrixDS.createMatrix(rows1, cols1);
        matrix2 = MatrixDS.createMatrix(rows2, cols2);

        workAllocator = new WorkAllocator(getApplicationContext(), workers, matrix1, matrix2);
        workAllocator.beginDistributedComputation();

    }

    private void updateProgress(int done) {
        ProgressWheel wheel = findViewById(R.id.wheelprogress);
        wheel.setPercentage(360 * (int) (done / totalPartitions));
        wheel.setStepCountText(done + "/" + totalPartitions);
        TextView totalPart = findViewById(R.id.totalPartitions);
        totalPart.setText("Total Partitions: " + totalPartitions);
    }

    private void bindViews() {
        rvWorkers = findViewById(R.id.rv_workers);
    }


    private void setAdapters() {
        workersAdapter = new WorkersAdapter(this, workers);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvWorkers.setLayoutManager(linearLayoutManager);

        rvWorkers.setAdapter(workersAdapter);
        workersAdapter.notifyDataSetChanged();
    }

    private void setEventListeners() {
        locationService.requestLocationUpdates(new FusedLocationListener() {
            @Override
            public void onLocationAvailable(Location location) {
                lastAvailableLocation = location;
            }
        });

        clientConnectionListener = new ClientConnectionListener() {
            @Override
            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {

            }

            @Override
            public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {

            }

            @Override
            public void onDisconnected(String endpointId) {
                updateWorkerConnectionStatus(endpointId, Constants.WorkStatus.DISCONNECTED);
                workAllocator.removeWorker(endpointId);
                NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(endpointId);
            }
        };
//
        payloadListener = new PayloadListener() {
            @Override
            public void onPayloadReceived(String endpointId, Payload payload) {
                try {
                    ClientPayLoad tPayload = PayloadConverter.fromPayload(payload);
                    if (tPayload.getTag().equals(Constants.PayloadTags.DISCONNECTED)) {
                        Log.d("OFLOD", "DISCONN");

                        updateWorkerConnectionStatus(endpointId, Constants.WorkStatus.DISCONNECTED);
                        workAllocator.removeWorker(endpointId);
                        NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(endpointId);
                    }

                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {
                //TODO : here or up there : this is little dicey. can be fixed
            }
        };

    }


    private void unpackBundle() {
        try {
            Bundle bundle = getIntent().getExtras();

            ArrayList<ConnectedDevice> connectedDevices = (ArrayList<ConnectedDevice>) bundle.getSerializable(Constants.CONNECTED_DEVICES);
            addToWorkers(connectedDevices);
            Log.d("CHECK", "Added a connected Device as worker");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void addToWorkers(ArrayList<ConnectedDevice> connectedDevices) {
        for (ConnectedDevice connectedDevice : connectedDevices) {
            Worker worker = new Worker();
            worker.setEndpointId(connectedDevice.getEndpointId());
            worker.setEndpointName(connectedDevice.getEndpointName());

            WorkInfo workStatus = new WorkInfo();
            workStatus.setStatusInfo(Constants.WorkStatus.WORKING);

            worker.setWorkStatus(workStatus);
            worker.setDeviceStats(new DeviceStatistics());

            workers.add(worker);
        }
    }

    private void computeMatrixMultiplicationOnMaster() {
        matrix1 = MatrixDS.createMatrix(rows1, cols1);
        matrix2 = MatrixDS.createMatrix(rows2, cols2);

        long startTime = System.currentTimeMillis();

        int[][] mul = new int[rows1][cols2];
        for (int i = 0; i < rows1; i++) {
            for (int j = 0; j < cols2; j++) {
                mul[i][j] = 0;
                for (int k = 0; k < cols1; k++) {
                    mul[i][j] += matrix1[i][k] * matrix2[k][j];
                }
            }
        }

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        //Toast.makeText(this, "Execution on master alone: " + (totalTime), Toast.LENGTH_SHORT).show();
        FlushToFile.writeTextToFile(getApplicationContext(), "exec_time_master_alone.txt", false, totalTime + "ms");
    }

    private void setupDeviceBatteryStatsCollector() {
        handler = new Handler();
        runnable = () -> {
            DeviceStatistics deviceStats = Device.getStats(getApplicationContext());
            Location location = LocationService.getInstance(getApplicationContext()).getLastAvailableLocation();

            if (location != null) {
                deviceStats.setLatitude(location.getLatitude());
                deviceStats.setLongitude(location.getLongitude());
                deviceStats.setLocationValid(true);
            }

            String deviceStatsStr = deviceStats.getBatteryLevel() + "%"
                    + "\t" + (deviceStats.isCharging() ? "CHARGING" : "NOT CHARGING");
            FlushToFile.writeTextToFile(getApplicationContext(), "master_battery.txt", true, deviceStatsStr);

            handler.postDelayed(runnable, 5 * 1000);
        };
    }

    @Override
    protected void onResume() {
        super.onResume();

        NearbyConnectionsManager.getInstance(getApplicationContext()).registerPayloadListener(payloadListener);
        NearbyConnectionsManager.getInstance(getApplicationContext()).registerClientConnectionListener(clientConnectionListener);
        startWorkerStatusSubscribers();

        handler.postDelayed(runnable, 5 * 1000);
        LocationService.getInstance(getApplicationContext()).start();
    }

    private void updateWorkerConnectionStatus(String endpointId, String status) {
        Log.d("DISCONNECTED----", endpointId);
        for (int i = 0; i < workers.size(); i++) {

            Log.d("DISCONNECTED--", workers.get(i).getEndpointId());
            if (workers.get(i).getEndpointId().equals(endpointId)) {
                workers.get(i).getWorkStatus().setStatusInfo(status);
                workersAdapter.notifyDataSetChanged();
                break;
            }
        }
    }


    private void startWorkerStatusSubscribers() {
        for (Worker worker : workers) {
            if (workerStatusSubscriberMap.containsKey(worker.getEndpointId())) {
                continue;
            }

            WorkerStatusSubscriber workerStatusSubscriber = new WorkerStatusSubscriber(getApplicationContext(), worker.getEndpointId(), new WorkerStatusListener() {
                @Override
                public void onWorkStatusReceived(String endpointId, WorkInfo workStatus) {

                    if (workStatus.getStatusInfo().equals(Constants.WorkStatus.DISCONNECTED)) {

                        //Log.d("OFLOD", "DISCONN");

                        updateWorkerConnectionStatus(endpointId, Constants.WorkStatus.DISCONNECTED);
                        workAllocator.removeWorker(endpointId);
                        NearbyConnectionsManager.getInstance(getApplicationContext()).rejectConnection(endpointId);
                    } else {
                        updateWorkerStatus(endpointId, workStatus);
                    }

                    workAllocator.checkWorkCompletion(getWorkAmount());
                }

                @Override
                public void onDeviceStatsReceived(String endpointId, DeviceStatistics deviceStats) {
                    updateWorkerStatus(endpointId, deviceStats);

                    String deviceStatsStr = deviceStats.getBatteryLevel() + "%"
                            + "\t" + (deviceStats.isCharging() ? "CHARGING" : "NOT CHARGING")
                            + "\t\t" + deviceStats.getLatitude()
                            + "\t" + deviceStats.getLongitude();
                    FlushToFile.writeTextToFile(getApplicationContext(), endpointId + ".txt", true, deviceStatsStr);

                    Log.d("OFLOD", "WORK AMOUNT: " + getWorkAmount());
                    workAllocator.checkWorkCompletion(getWorkAmount());
                }
            });

            workerStatusSubscriber.start();
            workerStatusSubscriberMap.put(worker.getEndpointId(), workerStatusSubscriber);
        }
    }


    private int getWorkAmount() {
        int sum = 0;
        for (Worker worker : workers) {
            sum += worker.getWorkAmount();

        }
        return sum;
    }

    private void updateWorkerStatus(String endpointId, WorkInfo workStatus) {
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);

            if (worker.getEndpointId().equals(endpointId)) {
                worker.setWorkStatus(workStatus);

                if (workStatus.getStatusInfo().equals(Constants.WorkStatus.WORKING) && workAllocator.isItNewWork(workStatus.getPartitionIndexInfo())) {
                    workers.get(i).setWorkAmount(workers.get(i).getWorkAmount() + 1);
                    workAmount += 1;
                }

                workAllocator.updateWorkStatus(worker, workStatus);

                workersAdapter.notifyItemChanged(i);
                break;
            }
        }
        updateProgress(workAmount);
    }

    private void updateWorkerStatus(String endpointId, DeviceStatistics deviceStats) {
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);

            if (worker.getEndpointId().equals(endpointId)) {
                worker.setDeviceStats(deviceStats);

                if (deviceStats.isLocationValid() && lastAvailableLocation != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(lastAvailableLocation.getLatitude(), lastAvailableLocation.getLongitude(), deviceStats.getLatitude(), deviceStats.getLongitude(), results);

                    worker.setDistanceFromMaster(results[0]);
                }

                workersAdapter.notifyItemChanged(i);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterPayloadListener(payloadListener);
        NearbyConnectionsManager.getInstance(getApplicationContext()).unregisterClientConnectionListener(clientConnectionListener);
        stopWorkerStatusSubscribers();

        handler.removeCallbacks(runnable);
        LocationMonitor.getInstance(getApplicationContext()).stop();
    }

    private void stopWorkerStatusSubscribers() {
        for (Worker worker : workers) {
            WorkerStatusSubscriber workerStatusSubscriber = workerStatusSubscriberMap.get(worker.getEndpointId());
            if (workerStatusSubscriber != null) {
                workerStatusSubscriber.stop();
                workerStatusSubscriberMap.remove(worker.getEndpointId());
            }
        }
    }

}