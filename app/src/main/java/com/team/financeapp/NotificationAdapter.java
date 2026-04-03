package com.team.financeapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for rendering in-app notifications.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<NotificationItem> notifications;

    public NotificationAdapter(List<NotificationItem> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationItem item = notifications.get(position);
        DrawableUtils.safeSetImageResource(holder.icon, item.getIconRes(), R.drawable.ic_notification);
        holder.title.setText(item.getTitle());
        holder.message.setText(item.getMessage());
        holder.time.setText(item.getTimeLabel());
        holder.unreadDot.setVisibility(item.isUnread() ? View.VISIBLE : View.GONE);

        holder.itemView.setOnClickListener(v -> {
            if (item.isUnread()) {
                item.setUnread(false);
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final TextView message;
        private final TextView time;
        private final View unreadDot;

        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.iv_notification_icon);
            title = itemView.findViewById(R.id.tv_notification_title);
            message = itemView.findViewById(R.id.tv_notification_message);
            time = itemView.findViewById(R.id.tv_notification_time);
            unreadDot = itemView.findViewById(R.id.view_unread_dot);
        }
    }
}

