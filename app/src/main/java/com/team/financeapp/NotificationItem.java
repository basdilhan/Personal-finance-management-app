package com.team.financeapp;

/**
 * Model class for in-app notifications.
 */
public class NotificationItem {
    private final String id;
    private final String title;
    private final String message;
    private final String timeLabel;
    private final int iconRes;
    private final long createdAt;
    private final int notificationId;
    private boolean unread;

    public NotificationItem(String id,
                            String title,
                            String message,
                            String timeLabel,
                            int iconRes,
                            long createdAt,
                            int notificationId,
                            boolean unread) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.timeLabel = timeLabel;
        this.iconRes = iconRes;
        this.createdAt = createdAt;
        this.notificationId = notificationId;
        this.unread = unread;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public int getIconRes() {
        return iconRes;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }
}

