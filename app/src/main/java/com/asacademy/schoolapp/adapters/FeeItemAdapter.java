package com.asacademy.schoolapp.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asacademy.schoolapp.R;
import com.asacademy.schoolapp.models.FeeSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FeeItemAdapter extends RecyclerView.Adapter<FeeItemAdapter.ViewHolder> {

    private final Context context;
    private List<FeeSummary.FeeItem> feeItems = new ArrayList<>();

    public FeeItemAdapter(Context context) {
        this.context = context;
    }

    public void setFeeItems(List<FeeSummary.FeeItem> items) {
        this.feeItems = items != null ? items : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_fee, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FeeSummary.FeeItem item = feeItems.get(position);

        holder.tvHeadName.setText(item.headName);
        holder.tvAmount.setText(String.format(Locale.getDefault(), "Total: ₹%.0f", item.amount));
        holder.tvPaid.setText(String.format(Locale.getDefault(), "Paid: ₹%.0f", item.paidAmount));
        holder.tvBalance.setText(String.format(Locale.getDefault(), "Due: ₹%.0f", item.balance));

        if (item.isPaid || item.balance <= 0) {
            holder.tvStatus.setText("PAID");
            holder.tvStatus.setTextColor(Color.parseColor("#059669"));
            holder.tvStatus.setBackgroundResource(R.drawable.badge_success_bg);
        } else {
            holder.tvStatus.setText("DUE");
            holder.tvStatus.setTextColor(Color.parseColor("#dc2626"));
            holder.tvStatus.setBackgroundResource(R.drawable.badge_danger_bg);
        }
    }

    @Override
    public int getItemCount() {
        return feeItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeadName, tvAmount, tvPaid, tvBalance, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeadName = itemView.findViewById(R.id.tvFeeHeadName);
            tvAmount = itemView.findViewById(R.id.tvFeeAmount);
            tvPaid = itemView.findViewById(R.id.tvFeePaid);
            tvBalance = itemView.findViewById(R.id.tvFeeBalance);
            tvStatus = itemView.findViewById(R.id.tvFeeStatus);
        }
    }
}
