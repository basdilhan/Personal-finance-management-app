package com.team.financeapp.notifications;

import android.content.Context;
import android.content.SharedPreferences;

import com.team.financeapp.NotificationItem;
import com.team.financeapp.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Lightweight persistent store for in-app notification center history.
 */
public final class NotificationCenterStore {

    private static final String PREF_NAME = "notification_center_store";
    private static final String KEY_ITEMS = "items";
    private static final int MAX_ITEMS = 200;

    private NotificationCenterStore() {
    }

    public static synchronized void add(Context context, int notificationId, String title, String message) {
        List<StoredItem> items = readStored(context);
        StoredItem item = new StoredItem();
        item.id = UUID.randomUUID().toString();
        item.title = title;
        item.message = message;
        item.createdAt = System.currentTimeMillis();
        item.notificationId = notificationId;
        item.unread = true;
        item.iconRes = resolveIcon(title);
        items.add(item);

        items.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
        if (items.size() > MAX_ITEMS) {
            items = new ArrayList<>(items.subList(0, MAX_ITEMS));
        }
        writeStored(context, items);
    }

    public static synchronized List<NotificationItem> getAll(Context context) {
        List<StoredItem> items = readStored(context);
        items.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));

        List<NotificationItem> mapped = new ArrayList<>();
        for (StoredItem item : items) {
            mapped.add(new NotificationItem(
                    item.id,
                    item.title,
                    item.message,
                    formatTimeLabel(item.createdAt),
                    item.iconRes,
                    item.createdAt,
                    item.notificationId,
                    item.unread
            ));
        }
        return mapped;
    }

    public static synchronized void markRead(Context context, String id) {
        List<StoredItem> items = readStored(context);
        boolean changed = false;
        for (StoredItem item : items) {
            if (item.id.equals(id) && item.unread) {
                item.unread = false;
                changed = true;
                break;
            }
        }
        if (changed) {
            writeStored(context, items);
        }
    }

    public static synchronized void delete(Context context, String id) {
        List<StoredItem> items = readStored(context);
        boolean changed = items.removeIf(item -> item.id.equals(id));
        if (changed) {
            writeStored(context, items);
        }
    }

    public static synchronized void clearAll(Context context) {
        preferences(context).edit().remove(KEY_ITEMS).apply();
    }

    private static int resolveIcon(String title) {
        String normalized = title == null ? "" : title.toLowerCase(Locale.ROOT);
        if (normalized.contains("goal")) {
            return R.drawable.ic_savings;
        }
        if (normalized.contains("bill")) {
            return R.drawable.ic_bills;
        }
        return R.drawable.ic_notification;
    }

    private static String formatTimeLabel(long createdAt) {
        long diff = System.currentTimeMillis() - createdAt;
        long minute = 60_000L;
        long hour = 60L * minute;
        long day = 24L * hour;

        if (diff < minute) {
            return "Just now";
        }
        if (diff < hour) {
            return (diff / minute) + "m ago";
        }
        if (diff < day) {
            return (diff / hour) + "h ago";
        }
        return (diff / day) + "d ago";
    }

    private static List<StoredItem> readStored(Context context) {
        String raw = preferences(context).getString(KEY_ITEMS, "[]");
        List<StoredItem> items = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                StoredItem item = new StoredItem();
                item.id = object.optString("id", UUID.randomUUID().toString());
                item.title = object.optString("title", "Notification");
                item.message = object.optString("message", "");
                item.createdAt = object.optLong("createdAt", System.currentTimeMillis());
                item.notificationId = object.optInt("notificationId", 0);
                item.iconRes = object.optInt("iconRes", R.drawable.ic_notification);
                item.unread = object.optBoolean("unread", true);
                items.add(item);
            }
        } catch (Exception ignored) {
            // Corrupt data should not crash app; reset on next write.
        }
        return items;
    }

    private static void writeStored(Context context, List<StoredItem> items) {
        JSONArray array = new JSONArray();
        for (StoredItem item : items) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", item.id);
                object.put("title", item.title);
                object.put("message", item.message);
                object.put("createdAt", item.createdAt);
                object.put("notificationId", item.notificationId);
                object.put("iconRes", item.iconRes);
                object.put("unread", item.unread);
                array.put(object);
            } catch (Exception ignored) {
            }
        }
        preferences(context).edit().putString(KEY_ITEMS, array.toString()).apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static final class StoredItem {
        String id;
        String title;
        String message;
        long createdAt;
        int notificationId;
        int iconRes;
        boolean unread;
    }
}
