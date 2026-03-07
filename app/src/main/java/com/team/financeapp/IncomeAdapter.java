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
 * RecyclerView adapter for displaying incomes with date grouping.
 */
public class IncomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_DATE_HEADER = 0;
    private static final int TYPE_INCOME_ITEM = 1;

    private final List<Object> items = new ArrayList<>();
    private List<IncomeEntry> incomes;

    public IncomeAdapter(List<IncomeEntry> incomes) {
        this.incomes = incomes;
        groupByDate();
    }

    private void groupByDate() {
        items.clear();
        if (incomes == null || incomes.isEmpty()) {
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = null;

        List<IncomeEntry> sorted = new ArrayList<>(incomes);
        sorted.sort((i1, i2) -> Long.compare(i2.getDate(), i1.getDate()));

        for (IncomeEntry income : sorted) {
            String incomeDate = dateFormat.format(new Date(income.getDate()));
            if (!incomeDate.equals(currentDate)) {
                currentDate = incomeDate;
                items.add(getDateLabel(income.getDate()));
            }
            items.add(income);
        }
    }

    private String getDateLabel(long timestamp) {
        Calendar today = Calendar.getInstance();
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

        String value = keyFormat.format(new Date(timestamp));
        String todayValue = keyFormat.format(today.getTime());
        String yesterdayValue = keyFormat.format(yesterday.getTime());

        if (value.equals(todayValue)) {
            return "Today\n" + displayFormat.format(new Date(timestamp));
        }
        if (value.equals(yesterdayValue)) {
            return "Yesterday\n" + displayFormat.format(new Date(timestamp));
        }
        return displayFormat.format(new Date(timestamp));
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_DATE_HEADER : TYPE_INCOME_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_DATE_HEADER) {
            return new DateHeaderViewHolder(inflater.inflate(R.layout.item_date_header, parent, false));
        }
        return new IncomeViewHolder(inflater.inflate(R.layout.income_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateHeaderViewHolder) {
            ((DateHeaderViewHolder) holder).bind((String) items.get(position));
            return;
        }
        ((IncomeViewHolder) holder).bind((IncomeEntry) items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateIncomes(List<IncomeEntry> updated) {
        incomes = updated;
        groupByDate();
        notifyDataSetChanged();
    }

    private static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvDateLabel;

        DateHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateLabel = itemView.findViewById(R.id.tv_date_label);
        }

        void bind(String label) {
            tvDateLabel.setText(label);
        }
    }

    private static class IncomeViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIncomeIcon;
        private final TextView tvSourceName;
        private final TextView tvIncomeNote;
        private final TextView tvIncomeTime;
        private final TextView tvIncomeAmount;

        IncomeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIncomeIcon = itemView.findViewById(R.id.iv_income_icon);
            tvSourceName = itemView.findViewById(R.id.tv_source_name);
            tvIncomeNote = itemView.findViewById(R.id.tv_income_note);
            tvIncomeTime = itemView.findViewById(R.id.tv_income_time);
            tvIncomeAmount = itemView.findViewById(R.id.tv_income_amount);
        }

        void bind(IncomeEntry income) {
            ivIncomeIcon.setImageResource(income.getSourceIcon());
            tvSourceName.setText(income.getSource());
            tvIncomeNote.setText(income.getNote());
            tvIncomeTime.setText(income.getTime());
            tvIncomeAmount.setText(String.format(Locale.getDefault(), "+LKR %,.2f", income.getAmount()));
        }
    }
}

