package com.scholix.app;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GradeAdapter extends RecyclerView.Adapter<GradeAdapter.GradeViewHolder> {

    private ArrayList<JSONObject> gradeList;

    public GradeAdapter(ArrayList gradeList) {
        this.gradeList = gradeList;
    }

    @NonNull
    @Override
    public GradeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grade, parent, false);
        return new GradeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GradeViewHolder holder, int position) {
        JSONObject grade = gradeList.get(position);
        try {
            holder.subjectText.setText(grade.getString("subject"));
            if(grade.getString("subject").equals("ממוצע"))
                holder.subjectText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40); // Bigger text

            holder.nameText.setText(grade.getString("name"));
            String displayGrade = grade.getString("grade");
            holder.gradeText.setText(displayGrade);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public int getItemCount() {
        return gradeList.size();
    }

    static class GradeViewHolder extends RecyclerView.ViewHolder {
        TextView subjectText, nameText, gradeText;

        public GradeViewHolder(@NonNull View itemView) {
            super(itemView);
            subjectText = itemView.findViewById(R.id.subject_text);
            nameText = itemView.findViewById(R.id.name_text);
            gradeText = itemView.findViewById(R.id.grade_text);
        }
    }
}
