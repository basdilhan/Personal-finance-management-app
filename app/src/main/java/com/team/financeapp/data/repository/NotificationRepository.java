package com.team.financeapp.data.repository;

import android.content.Context;

import com.team.financeapp.data.remote.ApiClient;
import com.team.financeapp.data.remote.NotificationApiService;
import com.team.financeapp.data.remote.dto.NotificationDto;
import com.team.financeapp.NotificationItem;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationRepository {

    private final NotificationApiService apiService;

    public NotificationRepository(Context context) {
        ApiClient.init(context);
        this.apiService = ApiClient.getClient().create(NotificationApiService.class);
    }

    public interface LoadNotificationsCallback {
        void onNotificationsLoaded(List<NotificationItem> notifications);
        void onError(String message);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public void loadNotifications(LoadNotificationsCallback callback) {
        apiService.getNotifications().enqueue(new Callback<List<NotificationDto>>() {
            @Override
            public void onResponse(Call<List<NotificationDto>> call, Response<List<NotificationDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NotificationItem> items = new ArrayList<>();
                    for (NotificationDto dto : response.body()) {
                        items.add(new NotificationItem(
                                String.valueOf(dto.getId()),
                                dto.getTitle(),
                                dto.getMessage(),
                                parseTimeLabel(dto.getCreatedAt()),
                                getIconForType(dto.getType()),
                                System.currentTimeMillis(), // We can use current time since backend doesn't give us epoch
                                dto.getId(), // notificationId
                                !dto.isRead()
                        ));
                    }
                    callback.onNotificationsLoaded(items);
                } else {
                    callback.onError("Failed to load notifications.");
                }
            }

            @Override
            public void onFailure(Call<List<NotificationDto>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void markAsRead(int notificationId, ActionCallback callback) {
        apiService.markAsRead(notificationId).enqueue(new Callback<NotificationDto>() {
            @Override
            public void onResponse(Call<NotificationDto> call, Response<NotificationDto> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Failed to mark read.");
                }
            }

            @Override
            public void onFailure(Call<NotificationDto> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void deleteNotification(int notificationId, ActionCallback callback) {
        apiService.deleteNotification(notificationId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    callback.onError("Failed to delete.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    private String parseTimeLabel(String createdAt) {
        if (createdAt == null) return "Just now";
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.getDefault());
            java.util.Date date = sdf.parse(createdAt);
            if (date != null) {
                long diff = System.currentTimeMillis() - date.getTime();
                if (diff < 60000) return "Just now";
                if (diff < 3600000) return (diff / 60000) + "m ago";
                if (diff < 86400000) return (diff / 3600000) + "h ago";
                return (diff / 86400000) + "d ago";
            }
        } catch (Exception e) {
            // fallback for missing fractional seconds
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
                java.util.Date date = sdf.parse(createdAt);
                if (date != null) {
                    long diff = System.currentTimeMillis() - date.getTime();
                    if (diff < 60000) return "Just now";
                    if (diff < 3600000) return (diff / 60000) + "m ago";
                    if (diff < 86400000) return (diff / 3600000) + "h ago";
                    return (diff / 86400000) + "d ago";
                }
            } catch(Exception ignored){}
        }
        return "Recent";
    }

    private int getIconForType(String type) {
        if (type == null) return com.team.financeapp.R.drawable.ic_notification;
        switch (type) {
            case "bill_reminder":
                return com.team.financeapp.R.drawable.ic_electricity; // example
            case "goal_reminder":
            case "goal_progress_nudge":
                return com.team.financeapp.R.drawable.ic_target;
            case "budget_alert":
                return com.team.financeapp.R.drawable.ic_wallet;
            default:
                return com.team.financeapp.R.drawable.ic_notification;
        }
    }
}
