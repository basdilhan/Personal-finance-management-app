package com.team.financeapp;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {

    public interface OnDeleteClickListener {
        void onDeleteClick(BudgetProgress progress);
    }

    private List<BudgetProgress> budgets = new ArrayList<>();
    private final OnDeleteClickListener deleteListener;

    public BudgetAdapter(OnDeleteClickListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setBudgets(List<BudgetProgress> budgets) {
        this.budgets = budgets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        BudgetProgress progress = budgets.get(position);
        holder.tvCategory.setText(progress.getLimit().category);

        double spent = progress.getSpentAmount();
        double limit = progress.getLimit().limitAmount;

        holder.tvStatus.setText(String.format("$%.2f / $%.2f", spent, limit));

        int percent = limit > 0 ? (int) ((spent / limit) * 100) : 0;
        holder.progressBar.setProgress(percent);

        if (percent >= 100) {
            holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
        } else if (percent >= 80) {
            holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#F59E0B"))); // Orange
        } else {
            holder.progressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#3B82F6"))); // Blue
        }

        holder.ivDelete.setOnClickListener(v -> deleteListener.onDeleteClick(progress));
    }

    @Override
    public int getItemCount() {
        return budgets.size();
    }

    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvStatus;
        ProgressBar progressBar;
        ImageView ivDelete;

        public BudgetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tv_category_name);
            tvStatus = itemView.findViewById(R.id.tv_budget_status);
            progressBar = itemView.findViewById(R.id.progress_budget);
            ivDelete = itemView.findViewById(R.id.iv_delete_budget);
        }
    }
}
