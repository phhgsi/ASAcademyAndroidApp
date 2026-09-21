package com.asacademy.schoolapp.utils;

import com.asacademy.schoolapp.models.PreviousStudent;
import com.asacademy.schoolapp.models.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory navigation manager for flipping between students sequentially
 * without Intent payload size limitations or activity reloads.
 */
public class StudentNavigationManager {

    private static final List<Student> activeStudents = new ArrayList<>();
    private static int activeCurrentIndex = -1;

    private static final List<PreviousStudent> previousStudents = new ArrayList<>();
    private static int previousCurrentIndex = -1;

    // --- Active Students ---

    public static synchronized void setActiveStudents(List<Student> students, Student currentStudent) {
        activeStudents.clear();
        if (students != null) {
            activeStudents.addAll(students);
        }
        activeCurrentIndex = -1;
        if (currentStudent != null) {
            for (int i = 0; i < activeStudents.size(); i++) {
                if (activeStudents.get(i).id == currentStudent.id) {
                    activeCurrentIndex = i;
                    break;
                }
            }
        }
    }

    public static synchronized boolean hasActiveNext() {
        return activeCurrentIndex >= 0 && activeCurrentIndex < activeStudents.size() - 1;
    }

    public static synchronized boolean hasActivePrev() {
        return activeCurrentIndex > 0 && activeCurrentIndex < activeStudents.size();
    }

    public static synchronized Student getActiveNext() {
        if (hasActiveNext()) {
            activeCurrentIndex++;
            return activeStudents.get(activeCurrentIndex);
        }
        return null;
    }

    public static synchronized Student getActivePrev() {
        if (hasActivePrev()) {
            activeCurrentIndex--;
            return activeStudents.get(activeCurrentIndex);
        }
        return null;
    }

    public static synchronized int getActiveCurrentIndex() {
        return activeCurrentIndex;
    }

    public static synchronized int getActiveTotalCount() {
        return activeStudents.size();
    }

    public static synchronized void updateActiveStudent(Student student) {
        if (student == null) return;
        for (int i = 0; i < activeStudents.size(); i++) {
            if (activeStudents.get(i).id == student.id) {
                activeStudents.set(i, student);
                break;
            }
        }
    }

    // --- Previous Students ---

    public static synchronized void setPreviousStudents(List<PreviousStudent> students, PreviousStudent currentStudent) {
        previousStudents.clear();
        if (students != null) {
            previousStudents.addAll(students);
        }
        previousCurrentIndex = -1;
        if (currentStudent != null) {
            for (int i = 0; i < previousStudents.size(); i++) {
                if (previousStudents.get(i).id == currentStudent.id) {
                    previousCurrentIndex = i;
                    break;
                }
            }
        }
    }

    public static synchronized boolean hasPreviousNext() {
        return previousCurrentIndex >= 0 && previousCurrentIndex < previousStudents.size() - 1;
    }

    public static synchronized boolean hasPreviousPrev() {
        return previousCurrentIndex > 0 && previousCurrentIndex < previousStudents.size();
    }

    public static synchronized PreviousStudent getPreviousNext() {
        if (hasPreviousNext()) {
            previousCurrentIndex++;
            return previousStudents.get(previousCurrentIndex);
        }
        return null;
    }

    public static synchronized PreviousStudent getPreviousPrev() {
        if (hasPreviousPrev()) {
            previousCurrentIndex--;
            return previousStudents.get(previousCurrentIndex);
        }
        return null;
    }

    public static synchronized int getPreviousCurrentIndex() {
        return previousCurrentIndex;
    }

    public static synchronized int getPreviousTotalCount() {
        return previousStudents.size();
    }

    public static synchronized void updatePreviousStudent(PreviousStudent student) {
        if (student == null) return;
        for (int i = 0; i < previousStudents.size(); i++) {
            if (previousStudents.get(i).id == student.id) {
                previousStudents.set(i, student);
                break;
            }
        }
    }
}
