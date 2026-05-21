package com.example.data

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {
    val allScans: Flow<List<ScanItem>> = scanDao.getAllScans()
    val favoriteScans: Flow<List<ScanItem>> = scanDao.getFavoriteScans()
    val scanCount: Flow<Int> = scanDao.getScanCount()
    val favoriteCount: Flow<Int> = scanDao.getFavoriteCount()
    val categoryCounts: Flow<List<CategoryCount>> = scanDao.getScanCountByCategory()

    suspend fun getScanById(id: Long): ScanItem? {
        return scanDao.getScanById(id)
    }

    suspend fun insertScan(scan: ScanItem): Long {
        return scanDao.insertScan(scan)
    }

    suspend fun updateScan(scan: ScanItem) {
        scanDao.updateScan(scan)
    }

    suspend fun deleteScan(scan: ScanItem) {
        scanDao.deleteScan(scan)
    }

    suspend fun deleteScanById(id: Long) {
        scanDao.deleteScanById(id)
    }
}
