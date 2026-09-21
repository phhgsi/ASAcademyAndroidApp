package com.asacademy.schoolapp.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.asacademy.schoolapp.R;
import com.asacademy.schoolapp.models.Student;
import com.asacademy.schoolapp.network.ApiClient;

import java.util.ArrayList;
import java.util.List;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    public interface OnStudentClickListener {
        void onStudentClick(Student student);
        void onCameraClick(Student student);
    }

    private final Context context;
    private List<Student> students = new ArrayList<>();
    private final OnStudentClickListener listener;
    private final ApiClient apiClient;

    public StudentAdapter(Context context, OnStudentClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.apiClient = ApiClient.getInstance(context);
    }

    public void setStudents(List<Student> students) {
        this.students = students != null ? students : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Student> getStudents() {
        return students;
    }

    public void updateStudent(Student updated) {
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_student, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Student s = students.get(position);

        holder.tvName.setText(s.fullName);
        holder.tvHindiName.setText(s.nameHindi != null && !s.nameHindi.isEmpty() ? s.nameHindi : "");
        holder.tvHindiName.setVisibility(s.nameHindi != null && !s.nameHindi.isEmpty() ? View.VISIBLE : View.GONE);
        holder.tvScholar.setText(s.scholarNumber);
        holder.tvClass.setText(s.className != null && !s.className.isEmpty() ? s.className : "Class -");
        holder.tvFather.setText("F: " + (s.fatherName != null && !s.fatherName.isEmpty() ? s.fatherName : "-"));
        holder.tvMobile.setText("📱 " + (s.mobile != null && !s.mobile.isEmpty() ? s.mobile : "-"));

        if (holder.tvWing != null) {
            String w = s.wing != null ? s.wing : (s.scholarNumber != null && s.scholarNumber.toUpperCase().startsWith("S") ? "High School" : "Primary");
            if ("Secondary".equalsIgnoreCase(w) || "High School".equalsIgnoreCase(w) || (s.scholarNumber != null && s.scholarNumber.toUpperCase().startsWith("S"))) {
                holder.tvWing.setText("🎓 High School");
                holder.tvWing.setTextColor(android.graphics.Color.parseColor("#FBBF24"));
            } else {
                holder.tvWing.setText("🎒 Primary");
                holder.tvWing.setTextColor(android.graphics.Color.parseColor("#22D3EE"));
            }
        }

        // Avatar placeholder
        String initial = s.firstName != null && !s.firstName.isEmpty() ? s.firstName.substring(0, 1).toUpperCase() : "S";
        holder.tvAvatarInitial.setText(initial);
        holder.ivAvatar.setVisibility(View.GONE);
        holder.tvAvatarInitial.setVisibility(View.VISIBLE);

        if (s.photoUrl != null && !s.photoUrl.trim().isEmpty()) {
            holder.ivAvatar.setTag(s.photoUrl);
            apiClient.loadImage(s.photoUrl, new ApiClient.ApiCallback<Bitmap>() {
                @Override
                public void onSuccess(Bitmap result) {
                    if (s.photoUrl.equals(holder.ivAvatar.getTag())) {
                        holder.ivAvatar.setImageBitmap(result);
                        holder.ivAvatar.setVisibility(View.VISIBLE);
                        holder.tvAvatarInitial.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    holder.ivAvatar.setVisibility(View.GONE);
                    holder.tvAvatarInitial.setVisibility(View.VISIBLE);
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onStudentClick(s);
        });

        holder.btnCamera.setOnClickListener(v -> {
            if (listener != null) listener.onCameraClick(s);
        });
    }

    @Override
    public int getItemCount() {
        return students.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvAvatarInitial;
        TextView tvName, tvHindiName, tvScholar, tvClass, tvFather, tvMobile, tvWing;
        View btnCamera;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvAvatarInitial = itemView.findViewById(R.id.tvAvatarInitial);
            tvName = itemView.findViewById(R.id.tvName);
            tvHindiName = itemView.findViewById(R.id.tvHindiName);
            tvScholar = itemView.findViewById(R.id.tvScholar);
            tvClass = itemView.findViewById(R.id.tvClass);
            tvWing = itemView.findViewById(R.id.tvWing);
            tvFather = itemView.findViewById(R.id.tvFather);
            tvMobile = itemView.findViewById(R.id.tvMobile);
            btnCamera = itemView.findViewById(R.id.btnCamera);
        }
    }
}
