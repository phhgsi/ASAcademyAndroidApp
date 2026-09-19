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
import com.asacademy.schoolapp.models.AttendanceStudent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.ViewHolder> {

    private final Context context;
    private List<AttendanceStudent> list = new ArrayList<>();

    public AttendanceAdapter(Context context) {
        this.context = context;
    }

    public void setStudents(List<AttendanceStudent> students) {
        this.list = students != null ? students : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<AttendanceStudent> getStudents() {
        return list;
    }

    public void markAll(String status) {
        for (AttendanceStudent s : list) {
            s.status = status;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_attendance_student, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceStudent s = list.get(position);

        String rollText = (s.rollNumber != null && s.rollNumber > 0) ? String.format(Locale.getDefault(), "%02d", s.rollNumber) : String.format(Locale.getDefault(), "%02d", position + 1);
        holder.tvRollBadge.setText(rollText);
        holder.tvStudentName.setText(s.name != null ? s.name : "Student");
        holder.tvScholarSub.setText(s.scholarNumber != null ? s.scholarNumber : "");

        updateStatusButtons(holder, s.status != null ? s.status : "Present");

        holder.btnStatusP.setOnClickListener(v -> {
            s.status = "Present";
            updateStatusButtons(holder, "Present");
        });

        holder.btnStatusA.setOnClickListener(v -> {
            s.status = "Absent";
            updateStatusButtons(holder, "Absent");
        });

        holder.btnStatusL.setOnClickListener(v -> {
            s.status = "Leave";
            updateStatusButtons(holder, "Leave");
        });
    }

    private void updateStatusButtons(ViewHolder holder, String status) {
        // Reset all
        holder.btnStatusP.setBackgroundColor(Color.parseColor("#0C2849"));
        holder.btnStatusP.setTextColor(Color.parseColor("#94A3B8"));

        holder.btnStatusA.setBackgroundColor(Color.parseColor("#0C2849"));
        holder.btnStatusA.setTextColor(Color.parseColor("#94A3B8"));

        holder.btnStatusL.setBackgroundColor(Color.parseColor("#0C2849"));
        holder.btnStatusL.setTextColor(Color.parseColor("#94A3B8"));

        if ("Absent".equalsIgnoreCase(status)) {
            holder.btnStatusA.setBackgroundColor(Color.parseColor("#EF4444"));
            holder.btnStatusA.setTextColor(Color.WHITE);
        } else if ("Leave".equalsIgnoreCase(status)) {
            holder.btnStatusL.setBackgroundColor(Color.parseColor("#F59E0B"));
            holder.btnStatusL.setTextColor(Color.WHITE);
        } else { // Present
            holder.btnStatusP.setBackgroundColor(Color.parseColor("#10B981"));
            holder.btnStatusP.setTextColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRollBadge, tvStudentName, tvScholarSub;
        TextView btnStatusP, btnStatusA, btnStatusL;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRollBadge = itemView.findViewById(R.id.tvRollBadge);
            tvStudentName = itemView.findViewById(R.id.tvStudentName);
            tvScholarSub = itemView.findViewById(R.id.tvScholarSub);
            btnStatusP = itemView.findViewById(R.id.btnStatusP);
            btnStatusA = itemView.findViewById(R.id.btnStatusA);
            btnStatusL = itemView.findViewById(R.id.btnStatusL);
        }
    }
}
