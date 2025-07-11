package com.scholix.app;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private Context context;
    private ArrayList<JSONObject> items;

    public ScheduleAdapter(Context context, ArrayList<JSONObject> items) {
        this.context = context;
        this.items = items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public MaterialCardView cardView;
        public TextView hourView, subjectView, teacherView, changeView;
        public ViewHolder(View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            hourView = itemView.findViewById(R.id.text_hour);
            subjectView = itemView.findViewById(R.id.text_subject);
            teacherView = itemView.findViewById(R.id.text_teacher);
            changeView = itemView.findViewById(R.id.text_change);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        JSONObject item = items.get(position);
        try {
            holder.hourView.setText(item.getString("num"));
            holder.subjectView.setText(item.getString("subject"));
            holder.teacherView.setText(item.getString("teacher"));
            if (!item.getString("changes").isEmpty()) {
                holder.changeView.setVisibility(View.VISIBLE);
                holder.changeView.setText(item.getString("changes"));
            }
            else if (!item.getString("exams").isEmpty()){
                holder.changeView.setVisibility(View.VISIBLE);
                holder.changeView.setText(item.getString("exams"));

            }
            else {
                holder.changeView.setVisibility(View.GONE);
            }

            String bgColorStr;
            switch (item.getString("colorClass")) {
                case "pink-cell":
                    bgColorStr = "#ffffebef";
                    break;
                case "lightgreen-cell":
                    bgColorStr = "#ffd8ffd6";
                    break;
                case "lightpink-cell":
                    bgColorStr = "#ffffe6f0";
                    break;
                case "lightyellow-cell":
                    bgColorStr = "#fff5f8d2";
                    break;
                case "lightblue-cell":
                    bgColorStr = "#ffcceae7";
                    break;
                case "lightred-cell":
                    bgColorStr = "#fffdd9d5";
                    break;
                case "lightpurple-cell":
                    bgColorStr = "#ffe1d6f1";
                    break;
                case "lightorange-cell":
                    bgColorStr = "#ffffdaba";
                    break;
                case "blue-cell":
                    bgColorStr = "#ffc6d0ff";
                    break;
                case "lime-cell":
                    bgColorStr = "#ffebffbc";
                    break;
                case "lightgrey-cell":
                    bgColorStr = "#ffdfe5e8";
                    break;
                case "cancel-cell":
                    bgColorStr = "#7d5b5d";
                    break;
                case "custom-red-cell":
                    bgColorStr = "#ffffc0c0";
                    break;
                case "custom-green-cell":
                    bgColorStr = "#ffc0ffc0";
                    break;
                case "custom-blue-cell":
                    bgColorStr = "#ffc0cfff";
                    break;
                case "custom-orange-cell":
                    bgColorStr = "#ffffe0c0";
                    break;
                case "custom-yellow-cell":
                    bgColorStr = "#ffffffc0";
                    break;
                case "custom-purple-cell":
                    bgColorStr = "#ffe0c0ff";
                    break;
                case "custom-teal-cell":
                    bgColorStr = "#ffc0ffff";
                    break;
                case "custom-lime-cell":
                    bgColorStr = "#ffe0ffc0";
                    break;
                case "custom-pink-cell":
                    bgColorStr = "#ffffc0d0";
                    break;
                default:
                    bgColorStr = "#ffffffff";
                    break;
            }

            int bgColor = Color.parseColor(bgColorStr);
            holder.cardView.setCardBackgroundColor(bgColor);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
