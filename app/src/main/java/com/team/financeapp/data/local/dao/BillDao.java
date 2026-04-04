package com.team.financeapp.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.team.financeapp.data.local.entity.BillEntity;

import java.util.List;

@Dao
public interface BillDao {

    @Query("SELECT * FROM bills WHERE userId = :userId AND deleted = 0 ORDER BY dueDate ASC")
    List<BillEntity> getByUser(String userId);

    @Query("SELECT * FROM bills WHERE userId = :userId ORDER BY dueDate ASC")
    List<BillEntity> getAllByUser(String userId);

    @Query("SELECT * FROM bills WHERE userId = :userId AND syncState != 'SYNCED'")
    List<BillEntity> getPendingSync(String userId);

    @Query("SELECT * FROM bills WHERE localId = :localId LIMIT 1")
    BillEntity getByLocalId(long localId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(BillEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BillEntity> entities);

    @Query("DELETE FROM bills WHERE userId = :userId")
    void deleteAllForUser(String userId);

    @Update
    void update(BillEntity entity);

    @Query("DELETE FROM bills WHERE localId = :localId")
    void deleteByLocalId(long localId);
}
