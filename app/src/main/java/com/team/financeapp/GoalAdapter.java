package com.team.financeapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying savings goals
 */
public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalViewHolder> {

    private List<Goal> goals;

    public GoalAdapter(List<Goal> goals) {
        this.goals = goals;
    }

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.goal_item, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        Goal goal = goals.get(position);
        
        // Set goal name
        holder.tvGoalName.setText(goal.getName());
        
        // Set target date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        String targetDateStr = dateFormat.format(new Date(goal.getTargetDate()));
        holder.tvTargetDate.setText(targetDateStr);
        
        // Set category icon
        holder.ivCategoryIcon.setImageResource(goal.getCategoryIcon());
        holder.ivCategoryIcon.setBackgroundResource(goal.getProgressCircleBackground());
        
        // Set progress percentage
        int progress = goal.getProgressPercentage();
        holder.tvProgressPercentage.setText(progress + "%");
        holder.progressBar.setProgress(progress);
        
        // Set progress status
        holder.tvProgressStatus.setText("Complete");
        
        // Set current amount
        holder.tvCurrentAmount.setText(String.format("LKR%.0f", goal.getCurrentAmount()));
        holder.tvCurrentLabel.setText("Current");
        
        // Set target amount
        holder.tvTargetAmount.setText(String.format("LKR%.0f", goal.getTargetAmount()));
        holder.tvTargetLabel.setText("Target");
        
        // Set remaining amount
        holder.tvRemainingAmount.setText(String.format("LKR%.0f", goal.getRemainingAmount()));
        holder.tvRemainingLabel.setText("To Go");
    }

    @Override
    public int getItemCount() {
        return goals != null ? goals.size() : 0;
    }

    /**
     * Update goals list and refresh the adapter
     */
    public void updateGoals(List<Goal> newGoals) {
        this.goals = newGoals;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder class for goal items
     */
    public static class GoalViewHolder extends RecyclerView.ViewHolder {
        TextView tvGoalName;
        TextView tvTargetDate;
        ImageView ivCategoryIcon;
        TextView tvProgressPercentage;
        ProgressBar progressBar;
        TextView tvProgressStatus;
        TextView tvCurrentAmount;
        TextView tvCurrentLabel;
        TextView tvTargetAmount;
        TextView tvTargetLabel;
        TextView tvRemainingAmount;
        TextView tvRemainingLabel;

        public GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGoalName = itemView.findViewById(R.id.tv_goal_name);
            tvTargetDate = itemView.findViewById(R.id.tv_target_date);
            ivCategoryIcon = itemView.findViewById(R.id.iv_goal_icon);
            tvProgressPercentage = itemView.findViewById(R.id.tv_progress_percentage);
            progressBar = itemView.findViewById(R.id.progress_bar);
            tvProgressStatus = itemView.findViewById(R.id.tv_progress_status);
            tvCurrentAmount = itemView.findViewById(R.id.tv_current_amount);
            tvCurrentLabel = itemView.findViewById(R.id.tv_current_label);
            tvTargetAmount = itemView.findViewById(R.id.tv_target_amount);
            tvTargetLabel = itemView.findViewById(R.id.tv_target_label);
            tvRemainingAmount = itemView.findViewById(R.id.tv_remaining_amount);
            tvRemainingLabel = itemView.findViewById(R.id.tv_remaining_label);
        }
    }
}
