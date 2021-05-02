package com.amsy.mobileoffloading;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.content.Context;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.amsy.mobileoffloading.adapters.WorkerDeviceAdapter;
import com.amsy.mobileoffloading.callback.WorkerStatusListener;
import com.amsy.mobileoffloading.entities.WorkerDevice;
import com.amsy.mobileoffloading.entities.WorkerDeviceStatistics;
import com.amsy.mobileoffloading.entities.WorkDataStatus;
import com.amsy.mobileoffloading.entities.Worker;
import com.amsy.mobileoffloading.helper.GobalConstants;
import com.amsy.mobileoffloading.helper.WriteToFile;
import com.amsy.mobileoffloading.helper.MatrixManager;
import com.amsy.mobileoffloading.services.DeviceStatsManagerService;
import com.amsy.mobileoffloading.services.WorkerConnectionManagerService;
import com.amsy.mobileoffloading.services.WorkAllocatorService;
import com.amsy.mobileoffloading.services.WorkerStatusManager;
import com.app.progresviews.ProgressWheel;

import java.util.ArrayList;
import java.util.HashMap;

import needle.Needle;

public class MasterActivity extends AppCompatActivity {

    private RecyclerView rvWorkers;

    private HashMap<String, WorkerStatusManager> workerStatusSubscriberMap = new HashMap<>();

    private ArrayList<Worker> workers = new ArrayList<>();
    private WorkerDeviceAdapter workerDeviceAdapter;


    /* [row1 x cols1] * [row2 * cols2] */
    private int rows1 = GobalConstants.matrix_rows;
    private int cols1 = GobalConstants.matrix_columns;
    private int rows2 = GobalConstants.matrix_columns;
    private int cols2 = GobalConstants.matrix_rows;

    private int[][] matrix1;
    private int[][] matrix2;

    private WorkAllocatorService workAllocatorService;

    private int workAmount;
    private int totalPartitions;
    private Handler handler;
    private Runnable runnable;
    private DeviceStatsManagerService deviceStatsPublisher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master);

        Log.d("MasterDiscovery", "Starting computing matrix multiplication on only master");
        TextView masterPower = findViewById(R.id.masterPower);
        masterPower.setText("Stats not available");
        BatteryManager mBatteryManager =
                (BatteryManager)getSystemService(Context.BATTERY_SERVICE);
        Long initialEnergyMaster =
                mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        computeMatrixMultiplicationOnMaster();
        Long finalEnergyMaster =
                mBatteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        Long energyConsumedMaster = Math.abs(initialEnergyMaster-finalEnergyMaster);
        masterPower.setText("Power Consumption (Master): " +Long.toString(energyConsumedMaster)+ " nWh");
        Log.d("MasterDiscovery", "Completed computing matrix multiplication on only master");

        unpackBundle();
        bindViews();
        setAdapters();
        init();
        setupDeviceBatteryStatsCollector();

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopWorkerStatusSubscribers();
        deviceStatsPublisher.stop();
        handler.removeCallbacks(runnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startWorkerStatusSubscribers();
        deviceStatsPublisher.start();
        handler.postDelayed(runnable, GobalConstants.UPDATE_INTERVAL_UI);
    }

    @Override
    public void onBackPressed() {
        for (Worker w : workers) {
            updateWorkerConnectionStatus(w.getEndpointId(), GobalConstants.WorkStatus.DISCONNECTED);
            workAllocatorService.removeWorker(w.getEndpointId());
            WorkerConnectionManagerService.getInstance(getApplicationContext()).disconnectFromEndpoint(w.getEndpointId());
        }
        super.onBackPressed();
        finish();
    }

    private void init() {
        totalPartitions = rows1 * cols2;
        updateProgress(0);
        matrix1 = MatrixManager.createMatrix(rows1, cols1);
        matrix2 = MatrixManager.createMatrix(rows2, cols2);

        workAllocatorService = new WorkAllocatorService(getApplicationContext(), workers, matrix1, matrix2, slaveTime -> {
            TextView slave = findViewById(R.id.slaveTime);
            slave.setText("Execution time (Slave): " + slaveTime + "ms");
        });
        workAllocatorService.beginDistributedComputation();
    }

    private void updateProgress(int done) {
        ProgressWheel wheel = findViewById(R.id.wheelprogress);
        int per = 360 * done / totalPartitions;
        wheel.setPercentage(per);
        wheel.setStepCountText(done + "");
        TextView totalPart = findViewById(R.id.totalPartitions);
        totalPart.setText("Total Partitions: " + totalPartitions);
        if(per == 360) {
            deviceStatsPublisher.stop();
        }
    }

    private void bindViews() {
        rvWorkers = findViewById(R.id.rv_workers);
        SimpleItemAnimator itemAnimator = (SimpleItemAnimator) rvWorkers.getItemAnimator();
        itemAnimator.setSupportsChangeAnimations(false);
    }


    private void setAdapters() {
        workerDeviceAdapter = new WorkerDeviceAdapter(this, workers);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        rvWorkers.setLayoutManager(linearLayoutManager);

        rvWorkers.setAdapter(workerDeviceAdapter);
        workerDeviceAdapter.notifyDataSetChanged();
    }


    private void unpackBundle() {
        try {
            Bundle bundle = getIntent().getExtras();

            ArrayList<WorkerDevice> workerDevices = (ArrayList<WorkerDevice>) bundle.getSerializable(GobalConstants.CONNECTED_DEVICES);
            addToWorkers(workerDevices);
            Log.d("CHECK", "Added a connected Device as worker");
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private void addToWorkers(ArrayList<WorkerDevice> workerDevices) {
        for (WorkerDevice workerDevice : workerDevices) {
            Worker worker = new Worker();
            worker.setEndpointId(workerDevice.getEndpointId());
            worker.setEndpointName(workerDevice.getEndpointName());

            WorkDataStatus workStatus = new WorkDataStatus();
            workStatus.setStatusInfo(GobalConstants.WorkStatus.WORKING);

            worker.setWorkStatus(workStatus);
            worker.setDeviceStats(new WorkerDeviceStatistics());

            workers.add(worker);
        }
    }

    private void computeMatrixMultiplicationOnMaster() {
        matrix1 = MatrixManager.createMatrix(rows1, cols1);
        matrix2 = MatrixManager.createMatrix(rows2, cols2);
        Needle.onBackgroundThread().execute(() -> {
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
            WriteToFile.writeTextToFile(getApplicationContext(), "master_execution_time.txt", false, totalTime + "ms");
            TextView master = findViewById(R.id.masterTime);
            master.setText("Execution time (Master): " + totalTime + "ms");
        });
    }



    private void setupDeviceBatteryStatsCollector() {
        deviceStatsPublisher = new DeviceStatsManagerService(getApplicationContext(), null, GobalConstants.UPDATE_INTERVAL_UI);
        handler = new Handler();
        runnable = () -> {
            String deviceStatsStr = DeviceStatsManagerService.getBatteryLevel(this) + "%"
                    + "\t" + (DeviceStatsManagerService.isPluggedIn(this) ? "CHARGING" : "NOT CHARGING");
            WriteToFile.writeTextToFile(getApplicationContext(), "master_battery.txt", true, deviceStatsStr);
            handler.postDelayed(runnable, GobalConstants.UPDATE_INTERVAL_UI);
        };
    }


    private void updateWorkerConnectionStatus(String endpointId, String status) {
        Log.d("DISCONNECTED----", endpointId);
        for (int i = 0; i < workers.size(); i++) {

            Log.d("DISCONNECTED--", workers.get(i).getEndpointId());
            if (workers.get(i).getEndpointId().equals(endpointId)) {
                workers.get(i).getWorkStatus().setStatusInfo(status);
                workerDeviceAdapter.notifyDataSetChanged();
                break;
            }
        }
    }


    private void startWorkerStatusSubscribers() {
        for (Worker worker : workers) {
            if (workerStatusSubscriberMap.containsKey(worker.getEndpointId())) {
                continue;
            }

            WorkerStatusManager workerStatusManager = new WorkerStatusManager(getApplicationContext(), worker.getEndpointId(), new WorkerStatusListener() {
                @Override
                public void onWorkStatusReceived(String endpointId, WorkDataStatus workStatus) {

                    if (workStatus.getStatusInfo().equals(GobalConstants.WorkStatus.DISCONNECTED)) {
                        updateWorkerConnectionStatus(endpointId, GobalConstants.WorkStatus.DISCONNECTED);
                        workAllocatorService.removeWorker(endpointId);
                        WorkerConnectionManagerService.getInstance(getApplicationContext()).rejectConnection(endpointId);
                    } else {
                        updateWorkerStatus(endpointId, workStatus);
                    }
                    workAllocatorService.checkWorkCompletion(getWorkAmount());
                }

                @Override
                public void onDeviceStatsReceived(String endpointId, WorkerDeviceStatistics deviceStats) {
                    updateWorkerStatus(endpointId, deviceStats);

                    String deviceStatsStr = deviceStats.getBatteryLevel() + "%"
                            + "\t" + (deviceStats.isCharging() ? "CHARGING" : "NOT CHARGING")
                            + "\t\t" + deviceStats.getLatitude()
                            + "\t" + deviceStats.getLongitude();
                    WriteToFile.writeTextToFile(getApplicationContext(), endpointId + ".txt", true, deviceStatsStr);
                    Log.d("MASTER_ACTIVITY", "WORK AMOUNT: " + getWorkAmount());
                    workAllocatorService.checkWorkCompletion(getWorkAmount());
                }
            });

            workerStatusManager.start();
            workerStatusSubscriberMap.put(worker.getEndpointId(), workerStatusManager);
        }
    }


    private int getWorkAmount() {
        int sum = 0;
        for (Worker worker : workers) {
            sum += worker.getWorkAmount();

        }
        return sum;
    }

    private void updateWorkerStatus(String endpointId, WorkDataStatus workStatus) {
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);

            if (worker.getEndpointId().equals(endpointId)) {
                worker.setWorkStatus(workStatus);

                if (workStatus.getStatusInfo().equals(GobalConstants.WorkStatus.WORKING) && workAllocatorService.isItNewWork(workStatus.getPartitionIndexInfo())) {
                    workers.get(i).setWorkAmount(workers.get(i).getWorkAmount() + 1);
                    workAmount += 1;
                }

                workAllocatorService.updateWorkStatus(worker, workStatus);

                workerDeviceAdapter.notifyItemChanged(i);
                break;
            }
        }
        updateProgress(workAmount);
    }

    private void updateWorkerStatus(String endpointId, WorkerDeviceStatistics deviceStats) {
        for (int i = 0; i < workers.size(); i++) {
            Worker worker = workers.get(i);

            if (worker.getEndpointId().equals(endpointId)) {
                worker.setDeviceStats(deviceStats);
                Location masterLocation = DeviceStatsManagerService.getLocation(this);
                if (deviceStats.isLocationValid() && masterLocation != null) {
                    float[] results = new float[1];
                    Location.distanceBetween(masterLocation.getLatitude(), masterLocation.getLongitude(),
                            deviceStats.getLatitude(), deviceStats.getLongitude(), results);
                    Log.d("MASTER_ACTIVITY", "Master Location: " + masterLocation.getLatitude() + ", " + masterLocation.getLongitude());
                    Log.d("MASTER_ACTIVITY", "Master Distance: " + results[0]);
                    worker.setDistanceFromMaster(results[0]);
                }

                workerDeviceAdapter.notifyItemChanged(i);
            }
        }
    }

    private void stopWorkerStatusSubscribers() {
        for (Worker worker : workers) {
            WorkerStatusManager workerStatusManager = workerStatusSubscriberMap.get(worker.getEndpointId());
            if (workerStatusManager != null) {
                workerStatusManager.stop();
                workerStatusSubscriberMap.remove(worker.getEndpointId());
            }
        }
    }


}