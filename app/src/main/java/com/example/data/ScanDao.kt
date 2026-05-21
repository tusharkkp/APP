package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanItem>>

    @Query("SELECT * FROM scans WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteScans(): Flow<List<ScanItem>>

    @Query("SELECT * FROM scans WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: Long): ScanItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanItem): Long

    @Update
    suspend fun updateScan(scan: ScanItem)

    @Delete
    suspend fun deleteScan(scan: ScanItem)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun deleteScanById(id: Long)

    @Query("SELECT COUNT(*) FROM scans")
    fun getScanCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scans WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT type, COUNT(*) as count FROM scans GROUP BY type")
    fun getScanCountByCategory(): Flow<List<CategoryCount>>
}

data class CategoryCount(
    val type: String,
    val count: Int
)
