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
import com.asacademy.schoolapp.models.PreviousStudent;

import java.util.ArrayList;
import java.util.List;

public class PreviousStudentAdapter extends RecyclerView.Adapter<PreviousStudentAdapter.ViewHolder> {

    public interface OnPreviousStudentClickListener {
        void onPreviousStudentClick(PreviousStudent student);
    }

    private final Context context;
    private List<PreviousStudent> students = new ArrayList<>();
    private final OnPreviousStudentClickListener listener;

    public PreviousStudentAdapter(Context context, OnPreviousStudentClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setStudents(List<PreviousStudent> students) {
        this.students = students != null ? students : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<PreviousStudent> getStudents() {
        return students;
    }

    public void updateStudent(PreviousStudent updated) {
        if (updated == null || students == null) return;
        for (int i = 0; i < students.size(); i++) {
            if (students.get(i).id == updated.id) {
                students.set(i, updated);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_previous_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PreviousStudent p = students.get(position);

        holder.tvName.setText(p.name);
        holder.tvHindiName.setText(p.nameHindi != null && !p.nameHindi.isEmpty() ? p.nameHindi : "");
        holder.tvHindiName.setVisibility(p.nameHindi != null && !p.nameHindi.isEmpty() ? View.VISIBLE : View.GONE);
        holder.tvScholar.setText(p.scholarNumber != null && !p.scholarNumber.isEmpty() ? p.scholarNumber : "-");
        holder.tvClass.setText(p.className != null && !p.className.isEmpty() ? p.className : "Class -");
        holder.tvFather.setText("F: " + (p.fatherName != null && !p.fatherName.isEmpty() ? p.fatherName : "-"));
        holder.tvSssmid.setText("SSSMID: " + (p.sssmid != null && !p.sssmid.isEmpty() ? p.sssmid : "-"));

        // Wing badge
        String wing = p.wing != null ? p.wing : (p.scholarNumber != null && p.scholarNumber.toUpperCase().startsWith("S") ? "High School" : "Primary");
        if (wing.equalsIgnoreCase("Secondary") || wing.equalsIgnoreCase("High School") || (p.scholarNumber != null && p.scholarNumber.toUpperCase().startsWith("S"))) {
            holder.tvWing.setText("🎓 High School");
            holder.tvWing.setTextColor(Color.parseColor("#FBBF24")); // Amber Gold
        } else {
            holder.tvWing.setText("🎒 Primary");
            holder.tvWing.setTextColor(Color.parseColor("#22D3EE")); // Cyan
        }

        // Status badge
        String status = p.status != null ? p.status : "Currently Studying";
        holder.tvStatus.setText(status);
        if (status.contains("TC")) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#ea580c")); // Orange
            holder.tvStatus.setTextColor(Color.WHITE);
        } else if (status.contains("12th") || status.contains("Pass")) {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#4338ca")); // Indigo
            holder.tvStatus.setTextColor(Color.WHITE);
        } else {
            holder.tvStatus.setBackgroundColor(Color.parseColor("#059669")); // Emerald
            holder.tvStatus.setTextColor(Color.WHITE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPreviousStudentClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvHindiName, tvScholar, tvWing, tvClass, tvFather, tvSssmid, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvHindiName = itemView.findViewById(R.id.tvHindiName);
            tvScholar = itemView.findViewById(R.id.tvScholar);
            tvWing = itemView.findViewById(R.id.tvWing);
            tvClass = itemView.findViewById(R.id.tvClass);
            tvFather = itemView.findViewById(R.id.tvFather);
            tvSssmid = itemView.findViewById(R.id.tvSssmid);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
