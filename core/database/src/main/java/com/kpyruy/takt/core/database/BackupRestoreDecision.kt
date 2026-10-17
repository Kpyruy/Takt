package com.kpyruy.takt.core.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** Commits atomically with imported Room rows; resolves a cross-store undo journal after process death. */
@Entity(tableName = "backup_restore_decision")
data class BackupRestoreDecision(@PrimaryKey val id: String, val committed: Boolean)

@Dao
interface BackupRestoreDecisionDao {
    @Query("SELECT * FROM backup_restore_decision WHERE id = :id LIMIT 1")
    suspend fun find(id: String): BackupRestoreDecision?
    @Query("DELETE FROM backup_restore_decision WHERE id = :id")
    suspend fun delete(id: String)
    @Query("DELETE FROM backup_restore_decision")
    suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(decision: BackupRestoreDecision)
}
