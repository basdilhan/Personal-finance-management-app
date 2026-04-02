package com.team.financeapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying expenses with date grouping (timeline view)
 */
public class ExpenseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DATE_HEADER = 0;
    private static final int TYPE_EXPENSE_ITEM = 1;

    private List<Object> items; // Mix of String (dates) and Expense objects
    private List<Expense> expenses;
    private OnExpenseItemClickListener listener;

    public interface OnExpenseItemClickListener {
        void onExpenseClick(Expense expense);

        void onExpenseLongClick(Expense expense);
    }

    public ExpenseAdapter(List<Expense> expenses) {
        this.expenses = expenses;
        this.items = new ArrayList<>();
        groupExpensesByDate();
    }

    public ExpenseAdapter(List<Expense> expenses, OnExpenseItemClickListener listener) {
        this(expenses);
        this.listener = listener;
    }

    public void setOnExpenseItemClickListener(OnExpenseItemClickListener listener) {
        this.listener = listener;
    }

    /**
     * Group expenses by date and create items list with headers
     */
    private void groupExpensesByDate() {
        items.clear();
        if (expenses == null || expenses.isEmpty()) {
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMMM dd", Locale.getDefault());
        String currentDate = null;

        // Sort expenses by date (newest first)
        List<Expense> sortedExpenses = new ArrayList<>(expenses);
        sortedExpenses.sort((e1, e2) -> Long.compare(e2.getDate(), e1.getDate()));

        for (Expense expense : sortedExpenses) {
            String expenseDate = dateFormat.format(new Date(expense.getDate()));

            if (!expenseDate.equals(currentDate)) {
                currentDate = expenseDate;
                // Add date header
                String dateLabel = getDateLabel(expense.getDate());
                items.add(dateLabel);
            }

            // Add expense item
            items.add(expense);
        }
    }

    /**
     * Get a human-readable date label (Today, Yesterday, etc.)
     */
    private String getDateLabel(long timestamp) {
        Calendar expenseCalendar = Calendar.getInstance();
        expenseCalendar.setTimeInMillis(timestamp);

        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        String expenseDate = dateFormat.format(new Date(timestamp));
        String todayDate = dateFormat.format(today.getTime());
        String yesterdayDate = dateFormat.format(yesterday.getTime());

        if (expenseDate.equals(todayDate)) {
            return "Today\n" + displayFormat.format(new Date(timestamp)).split(" ")[0] + " " + today.get(Calendar.DAY_OF_MONTH);
        } else if (expenseDate.equals(yesterdayDate)) {
            return "Yesterday\n" + displayFormat.format(new Date(timestamp)).split(" ")[0] + " " + yesterday.get(Calendar.DAY_OF_MONTH);
        } else {
            return displayFormat.format(new Date(timestamp));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof String) {
            return TYPE_DATE_HEADER;
        } else {
            return TYPE_EXPENSE_ITEM;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_DATE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new DateHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.expense_item, parent, false);
            return new ExpenseViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateHeaderViewHolder) {
            String dateLabel = (String) items.get(position);
            ((DateHeaderViewHolder) holder).bind(dateLabel);
        } else if (holder instanceof ExpenseViewHolder) {
            Expense expense = (Expense) items.get(position);
            ((ExpenseViewHolder) holder).bind(expense, listener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Update the expense list
     */
    public void updateExpenses(List<Expense> newExpenses) {
        this.expenses = newExpenses;
        groupExpensesByDate();
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for date header items
     */
    private static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDateLabel;
        private View timeline;

        public DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateLabel = itemView.findViewById(R.id.tv_date_label);
            timeline = itemView.findViewById(R.id.timeline_dot);
        }

        public void bind(String dateLabel) {
            tvDateLabel.setText(dateLabel);
        }
    }

    /**
     * ViewHolder for expense items
     */
    private static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCategoryIcon;
        private TextView tvCategoryName;
        private TextView tvTime;
        private TextView tvAmount;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvCategoryName = itemView.findViewById(R.id.tv_category_name);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvAmount = itemView.findViewById(R.id.tv_amount);
        }

        public void bind(Expense expense, OnExpenseItemClickListener listener) {
            ivCategoryIcon.setImageResource(expense.getCategoryIcon());
            tvCategoryName.setText(expense.getCategory());
            tvTime.setText(expense.getTime());
            tvAmount.setText(String.format("LKR %.2f", expense.getAmount()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onExpenseClick(expense);
                }
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onExpenseLongClick(expense);
                    return true;
                }
                return false;
            });
        }
    }
}
