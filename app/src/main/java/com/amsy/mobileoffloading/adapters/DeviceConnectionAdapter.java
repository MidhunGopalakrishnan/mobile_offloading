package com.amsy.mobileoffloading.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.amsy.mobileoffloading.R;
import com.amsy.mobileoffloading.entities.WorkerDevice;
import com.amsy.mobileoffloading.helper.GobalConstants;

import java.util.List;

public class DeviceConnectionAdapter extends RecyclerView.Adapter<DeviceConnectionAdapter.ViewHolder>{

    private Context context;
    private List<WorkerDevice> workerDevices;

    public DeviceConnectionAdapter(@NonNull Context context, List<WorkerDevice> workerDevices) {
        this.context = context;
        this.workerDevices = workerDevices;
    }

    @NonNull
    @Override
    public DeviceConnectionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View itemView = layoutInflater.inflate(R.layout.connected_device_item, parent, false);

        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceConnectionAdapter.ViewHolder holder, int position) {
        holder.setClientId(workerDevices.get(position).getEndpointId(), workerDevices.get(position).getEndpointName());
        holder.setBatteryLevel(workerDevices.get(position).getDeviceStats().getBatteryLevel());
        holder.setRequestStatus(workerDevices.get(position).getRequestStatus());
    }

    @Override
    public int getItemCount() {
        return workerDevices.size();
    }

    protected class ViewHolder extends RecyclerView.ViewHolder {

        private TextView ClientId;
        private TextView BatteryLevel;
        private ImageView RequestStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ClientId = itemView.findViewById(R.id.client_id);
            BatteryLevel = itemView.findViewById(R.id.battery_level);
            RequestStatus = itemView.findViewById(R.id.request_status_image);
        }

        public void setClientId(String endpointId, String endpointName) {
            this.ClientId.setText(endpointName.toUpperCase()  + "\n(" + endpointId.toUpperCase() + ")");
        }

        public void setBatteryLevel(int batteryLevel) {
            if (batteryLevel > 0 && batteryLevel <= 100) {
                this.BatteryLevel.setText(batteryLevel + "%");
            } else {
                this.BatteryLevel.setText("");
            }
        }

        public void setRequestStatus(String requestStatus) {
            if (requestStatus.equals(GobalConstants.RequestStatus.ACCEPTED)) {
                this.RequestStatus.setBackgroundResource(R.drawable.ic_outline_check_circle_24);
            } else if (requestStatus.equals(GobalConstants.RequestStatus.REJECTED)) {
                this.RequestStatus.setBackgroundResource(R.drawable.ic_outline_cancel_24);
            } else {
                this.RequestStatus.setBackgroundResource(R.drawable.ic_outline_pending_24);
            }
        }
    }
}
