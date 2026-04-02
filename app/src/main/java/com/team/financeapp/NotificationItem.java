package com.team.financeapp;

/**
 * Model class for in-app notifications.
 */
public class NotificationItem {
    private final String title;
    private final String message;
    private final String timeLabel;
    private final int iconRes;
    private boolean unread;

    public NotificationItem(String title, String message, String timeLabel, int iconRes, boolean unread) {
        this.title = title;
        this.message = message;
        this.timeLabel = timeLabel;
        this.iconRes = iconRes;
        this.unread = unread;
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

    public boolean isUnread() {
        return unread;
    }

    public void setUnread(boolean unread) {
        this.unread = unread;
    }
}

