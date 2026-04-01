package com.team.financeapp.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.team.financeapp.Goal;
import com.team.financeapp.R;
import com.team.financeapp.data.local.AppDatabase;
import com.team.financeapp.data.local.SyncState;
import com.team.financeapp.data.local.dao.GoalDao;
import com.team.financeapp.data.local.entity.GoalEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Repository for managing Goals
 * Handles both local database operations and remote Firebase Firestore sync
 */
public class GoalRepository {

    public interface LoadGoalsCallback {
        void onGoalsLoaded(List<Goal> goals);

        void onError(String message);
    }

    public interface SaveGoalCallback {
        void onSuccess(Goal goal);

        void onError(String message);
    }

    public interface UpdateGoalCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface DeleteGoalCallback {
        void onSuccess();

        void onError(String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final GoalDao goalDao;
    private final FirebaseFirestore firestore;
    private final Handler mainHandler;

    public GoalRepository(@NonNull Context context) {
        this.goalDao = AppDatabase.getInstance(context).goalDao();
        this.firestore = FirebaseFirestore.getInstance();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Load all goals for a user from local database
     * Also attempts to refresh from remote if connected
     */
    public void loadGoals(@NonNull String userId, @NonNull LoadGoalsCallback callback) {
        IO.execute(() -> {
            List<GoalEntity> localEntities = goalDao.getByUser(userId);
            List<Goal> localGoals = toGoals(localEntities);
            mainHandler.post(() -> callback.onGoalsLoaded(localGoals));
        });

        refreshFromRemote(userId, callback);
    }

    /**
     * Save a new goal
     * Saves to local database first, then syncs to Firestore
     */
    public void saveGoal(@NonNull String userId, @NonNull Goal goal, @NonNull SaveGoalCallback callback) {
        GoalEntity entity = fromGoal(userId, goal);
        entity.remoteId = UUID.randomUUID().toString();
        entity.syncState = SyncState.PENDING;
        entity.createdAt = System.currentTimeMillis();
        entity.updatedAt = entity.createdAt;
        entity.deleted = false;

        IO.execute(() -> {
            long localId = goalDao.insert(entity);
            entity.localId = localId;

            // Convert back to Goal with the new ID
            Goal savedGoal = new Goal(
                    (int) localId,
                    entity.name,
                    entity.description,
                    entity.targetAmount,
                    entity.currentAmount,
                    entity.targetDate,
                    entity.category,
                    entity.categoryIcon,
                    entity.progressCircleBackground
            );

            mainHandler.post(() -> callback.onSuccess(savedGoal));

            // Try to push to remote
            pushGoalToRemote(entity, new SaveGoalCallback() {
                @Override
                public void onSuccess(Goal goal) {
                    // Remote sync successful
                }

                @Override
                public void onError(String message) {
                    // Log error but don't propagate - goal is already saved locally
                }
            });
        });
    }

    /**
     * Update an existing goal
     * Updates local database and syncs to Firestore
     */
    public void updateGoal(@NonNull String userId, @NonNull Goal goal, @NonNull UpdateGoalCallback callback) {
        IO.execute(() -> {
            // Retrieve existing entity to preserve remoteId
            GoalEntity existingEntity = goalDao.getById(goal.getId());

            if (existingEntity != null) {
                // Update only the changed fields while preserving remoteId
                existingEntity.userId = userId;
                existingEntity.name = goal.getName();
                existingEntity.description = goal.getDescription();
                existingEntity.targetAmount = goal.getTargetAmount();
                existingEntity.currentAmount = goal.getCurrentAmount();
                existingEntity.targetDate = goal.getTargetDate();
                existingEntity.category = goal.getCategory();
                existingEntity.categoryIcon = goal.getCategoryIcon();
                existingEntity.progressCircleBackground = goal.getProgressCircleBackground();
                existingEntity.syncState = SyncState.PENDING;
                existingEntity.updatedAt = System.currentTimeMillis();
                existingEntity.deleted = false;

                // Update in local database
                goalDao.update(existingEntity);
                mainHandler.post(callback::onSuccess);

                // Try to push to remote
                pushGoalToRemote(existingEntity, new SaveGoalCallback() {
                    @Override
                    public void onSuccess(Goal goal) {
                        // Remote sync successful
                    }

                    @Override
                    public void onError(String message) {
                        // Log error but don't propagate - goal is already updated locally
                    }
                });
            } else {
                mainHandler.post(() -> callback.onError("Goal not found"));
            }
        });
    }

    /**
     * Delete a goal (soft delete)
     * Marks as deleted in local database and syncs to Firestore
     */
    public void deleteGoal(@NonNull String userId, int goalLocalId, @NonNull DeleteGoalCallback callback) {
        IO.execute(() -> {
            // For soft delete, fetch the entity, mark as deleted, and update
            GoalEntity entity = goalDao.getById(goalLocalId);
            if (entity != null) {
                entity.deleted = true;
                entity.syncState = SyncState.PENDING;
                entity.updatedAt = System.currentTimeMillis();
                goalDao.update(entity);

                mainHandler.post(callback::onSuccess);

                // Try to sync deletion to remote
                pushGoalDeletionToRemote(entity);
            } else {
                mainHandler.post(() -> callback.onError("Goal not found"));
            }
        });
    }

    /**
     * Refresh goals from Firestore
     */
    private void refreshFromRemote(@NonNull String userId, @NonNull LoadGoalsCallback callback) {
        firestore.collection("goals")
                .whereEqualTo("userId", userId)
                .whereEqualTo("deleted", false)
                .get()
                .addOnSuccessListener(querySnapshot -> IO.execute(() -> {
                    List<GoalEntity> remoteEntities = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        remoteEntities.add(fromDocument(document));
                    }

                    // Merge with local data: update existing, add new
                    for (GoalEntity remoteEntity : remoteEntities) {
                        GoalEntity localEntity = goalDao.getByRemoteId(remoteEntity.remoteId);
                        if (localEntity == null) {
                            goalDao.insert(remoteEntity);
                        } else {
                            remoteEntity.localId = localEntity.localId;
                            goalDao.update(remoteEntity);
                        }
                    }

                    List<Goal> latest = toGoals(goalDao.getByUser(userId));
                    mainHandler.post(() -> callback.onGoalsLoaded(latest));
                }))
                .addOnFailureListener(e -> mainHandler.post(() -> callback.onError(
                        e.getMessage() == null ? "Failed to refresh goals" : e.getMessage()
                )));
    }

    /**
     * Push a goal to Firestore
     */
    private void pushGoalToRemote(@NonNull GoalEntity entity, @NonNull SaveGoalCallback callback) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", entity.userId);
        payload.put("name", entity.name);
        payload.put("description", entity.description);
        payload.put("targetAmount", entity.targetAmount);
        payload.put("currentAmount", entity.currentAmount);
        payload.put("targetDate", entity.targetDate);
        payload.put("category", entity.category);
        payload.put("categoryIcon", entity.categoryIcon);
        payload.put("progressCircleBackground", entity.progressCircleBackground);
        payload.put("deleted", entity.deleted);
        payload.put("syncState", SyncState.SYNCED);
        payload.put("createdAt", entity.createdAt);
        payload.put("updatedAt", System.currentTimeMillis());

        firestore.collection("goals")
                .document(entity.remoteId)
                .set(payload)
                .addOnSuccessListener(unused -> IO.execute(() -> {
                    entity.syncState = SyncState.SYNCED;
                    entity.updatedAt = System.currentTimeMillis();
                    goalDao.update(entity);

                    Goal goal = new Goal(
                            (int) entity.localId,
                            entity.name,
                            entity.description,
                            entity.targetAmount,
                            entity.currentAmount,
                            entity.targetDate,
                            entity.category,
                            entity.categoryIcon,
                            entity.progressCircleBackground
                    );
                    mainHandler.post(() -> callback.onSuccess(goal));
                }))
                .addOnFailureListener(e -> IO.execute(() -> {
                    entity.syncState = SyncState.FAILED;
                    entity.updatedAt = System.currentTimeMillis();
                    goalDao.update(entity);
                    mainHandler.post(() -> callback.onError(
                            e.getMessage() == null ? "Saved locally but cloud sync failed" : e.getMessage()
                    ));
                }));
    }

    /**
     * Push goal deletion to Firestore
     */
    private void pushGoalDeletionToRemote(@NonNull GoalEntity entity) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("deleted", true);
        payload.put("syncState", SyncState.SYNCED);
        payload.put("updatedAt", System.currentTimeMillis());

        firestore.collection("goals")
                .document(entity.remoteId)
                .update(payload)
                .addOnSuccessListener(unused -> IO.execute(() -> {
                    entity.syncState = SyncState.SYNCED;
                    entity.updatedAt = System.currentTimeMillis();
                    goalDao.update(entity);
                }))
                .addOnFailureListener(e -> IO.execute(() -> {
                    entity.syncState = SyncState.FAILED;
                    entity.updatedAt = System.currentTimeMillis();
                    goalDao.update(entity);
                }));
    }

    /**
     * Convert GoalEntity to Goal model
     */
    private List<Goal> toGoals(List<GoalEntity> entities) {
        List<Goal> goals = new ArrayList<>();
        for (GoalEntity entity : entities) {
            goals.add(new Goal(
                    (int) entity.localId,
                    entity.name,
                    entity.description,
                    entity.targetAmount,
                    entity.currentAmount,
                    entity.targetDate,
                    entity.category,
                    entity.categoryIcon,
                    entity.progressCircleBackground
            ));
        }
        return goals;
    }

    /**
     * Convert Goal model to GoalEntity
     */
    private GoalEntity fromGoal(String userId, Goal goal) {
        GoalEntity entity = new GoalEntity();
        entity.userId = userId;
        entity.name = goal.getName();
        entity.description = goal.getDescription();
        entity.targetAmount = goal.getTargetAmount();
        entity.currentAmount = goal.getCurrentAmount();
        entity.targetDate = goal.getTargetDate();
        entity.category = goal.getCategory();
        entity.categoryIcon = goal.getCategoryIcon();
        entity.progressCircleBackground = goal.getProgressCircleBackground();
        return entity;
    }

    /**
     * Convert Firestore DocumentSnapshot to GoalEntity
     */
    private GoalEntity fromDocument(DocumentSnapshot document) {
        GoalEntity entity = new GoalEntity();
        entity.remoteId = document.getId();
        entity.userId = getString(document, "userId", "");
        entity.name = getString(document, "name", "");
        entity.description = getString(document, "description", "");
        entity.targetAmount = getDouble(document, "targetAmount", 0.0d);
        entity.currentAmount = getDouble(document, "currentAmount", 0.0d);
        entity.targetDate = getLong(document, "targetDate", 0L);
        entity.category = getString(document, "category", "Other");
        entity.categoryIcon = (int) getLong(document, "categoryIcon", R.drawable.ic_wallet);
        entity.progressCircleBackground = (int) getLong(document, "progressCircleBackground", R.drawable.circle_primary_light);
        entity.deleted = document.getBoolean("deleted") != null && Boolean.TRUE.equals(document.getBoolean("deleted"));
        entity.syncState = SyncState.SYNCED;
        entity.createdAt = getLong(document, "createdAt", System.currentTimeMillis());
        entity.updatedAt = getLong(document, "updatedAt", System.currentTimeMillis());
        return entity;
    }

    private static String getString(DocumentSnapshot doc, String key, String fallback) {
        String value = doc.getString(key);
        return value == null ? fallback : value;
    }

    private static long getLong(DocumentSnapshot doc, String key, long fallback) {
        Long value = doc.getLong(key);
        return value == null ? fallback : value;
    }

    private static double getDouble(DocumentSnapshot doc, String key, double fallback) {
        Double value = doc.getDouble(key);
        return value == null ? fallback : value;
    }
}


