package com.team.financeapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying bills
 */
public class BillAdapter extends RecyclerView.Adapter<BillAdapter.BillViewHolder> {

    private List<Bill> bills;
    private OnBillItemClickListener listener;

    public interface OnBillItemClickListener {
        void onBillClick(Bill bill);

        void onBillLongClick(Bill bill);
    }

    public BillAdapter(List<Bill> bills) {
        this.bills = bills;
    }

    public BillAdapter(List<Bill> bills, OnBillItemClickListener listener) {
        this.bills = bills;
        this.listener = listener;
    }

    public void setOnBillItemClickListener(OnBillItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public BillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.bill_item, parent, false);
        return new BillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BillViewHolder holder, int position) {
        Bill bill = bills.get(position);
        holder.bind(bill, listener);
    }

    @Override
    public int getItemCount() {
        return bills.size();
    }

    /**
     * Update the bills list
     */
    public void updateBills(List<Bill> newBills) {
        this.bills = newBills;
        notifyDataSetChanged();
    }

    /**
     * ViewHolder for bill items
     */
    static class BillViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCategoryIcon;
        private View statusIndicator;
        private TextView tvBillName;
        private TextView tvDueDate;
        private TextView tvAmount;

        public BillViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.iv_bill_icon);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
            tvBillName = itemView.findViewById(R.id.tv_bill_name);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            tvAmount = itemView.findViewById(R.id.tv_bill_amount);
        }

        public void bind(Bill bill, OnBillItemClickListener listener) {
            DrawableUtils.safeSetImageResource(ivCategoryIcon, bill.getCategoryIcon(), R.drawable.ic_receipt);
            DrawableUtils.safeSetBackgroundResource(statusIndicator, bill.getIndicatorColor(), R.drawable.circle_blue_light);
            tvBillName.setText(bill.getName());
            
            // Format due date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(bill.getDueDate()));
            tvDueDate.setText("Due: " + formattedDate);
            
            tvAmount.setText(String.format("LKR %.2f", bill.getAmount()));

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBillClick(bill);
                }
            });
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onBillLongClick(bill);
                    return true;
                }
                return false;
            });
        }
    }
}
