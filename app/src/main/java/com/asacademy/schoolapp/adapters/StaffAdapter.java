package com.asacademy.schoolapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asacademy.schoolapp.R;
import com.asacademy.schoolapp.models.StaffMember;

import java.util.ArrayList;
import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {

    private final Context context;
    private List<StaffMember> list = new ArrayList<>();

    public StaffAdapter(Context context) {
        this.context = context;
    }

    public void setStaff(List<StaffMember> staffList) {
        this.list = staffList != null ? staffList : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_staff, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StaffMember s = list.get(position);

        holder.tvStaffName.setText(s.fullName != null ? s.fullName : "Staff Member");
        holder.tvStaffDesignation.setText((s.designation != null ? s.designation : "Staff") + " • " + (s.department != null ? s.department : ""));
        holder.tvStaffMobile.setText(s.mobile != null && !s.mobile.isEmpty() ? s.mobile : "No Phone");

        holder.btnCallStaff.setOnClickListener(v -> {
            if (s.mobile != null && !s.mobile.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + s.mobile));
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "No phone number available.", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnWhatsappStaff.setOnClickListener(v -> {
            if (s.mobile != null && !s.mobile.isEmpty()) {
                String cleanNum = s.mobile.replaceAll("[^0-9]", "");
                if (!cleanNum.startsWith("91") && cleanNum.length() == 10) {
                    cleanNum = "91" + cleanNum;
                }
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=" + cleanNum));
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "No phone number available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStaffAvatar, tvStaffName, tvStaffDesignation, tvStaffMobile, btnWhatsappStaff;
        ImageView btnCallStaff;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStaffAvatar = itemView.findViewById(R.id.tvStaffAvatar);
            tvStaffName = itemView.findViewById(R.id.tvStaffName);
            tvStaffDesignation = itemView.findViewById(R.id.tvStaffDesignation);
            tvStaffMobile = itemView.findViewById(R.id.tvStaffMobile);
            btnCallStaff = itemView.findViewById(R.id.btnCallStaff);
            btnWhatsappStaff = itemView.findViewById(R.id.btnWhatsappStaff);
        }
    }
}
