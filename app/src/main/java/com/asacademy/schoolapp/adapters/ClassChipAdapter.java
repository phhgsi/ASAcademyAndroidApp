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
import com.asacademy.schoolapp.models.SchoolClass;

import java.util.ArrayList;
import java.util.List;

public class ClassChipAdapter extends RecyclerView.Adapter<ClassChipAdapter.ViewHolder> {

    public interface OnClassSelectedListener {
        void onClassSelected(int classId);
    }

    private final Context context;
    private List<SchoolClass> classes = new ArrayList<>();
    private int selectedClassId = 0;
    private final OnClassSelectedListener listener;

    public ClassChipAdapter(Context context, OnClassSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setClasses(List<SchoolClass> classes) {
        this.classes = new ArrayList<>();
        // Add "All" option
        SchoolClass all = new SchoolClass();
        all.id = 0;
        all.className = "All Classes";
        all.displayName = "All Classes";
        this.classes.add(all);

        if (classes != null) {
            this.classes.addAll(classes);
        }
        notifyDataSetChanged();
    }

    public void setSelectedClassId(int classId) {
        this.selectedClassId = classId;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_class_chip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SchoolClass c = classes.get(position);
        holder.tvChip.setText(c.displayName);

        boolean isSelected = c.id == selectedClassId;
        if (isSelected) {
            holder.tvChip.setBackgroundResource(R.drawable.bg_glass_pill_selected);
            holder.tvChip.setTextColor(Color.WHITE);
        } else {
            holder.tvChip.setBackgroundResource(R.drawable.bg_glass_pill_unselected);
            holder.tvChip.setTextColor(Color.parseColor("#94A3B8"));
        }

        holder.itemView.setOnClickListener(v -> {
            selectedClassId = c.id;
            notifyDataSetChanged();
            if (listener != null) listener.onClassSelected(c.id);
        });
    }

    @Override
    public int getItemCount() {
        return classes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvChip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvChip = itemView.findViewById(R.id.tvClassChip);
        }
    }
}
