package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.VideoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM video_items ORDER BY addedDate DESC")
    fun getAllVideos(): Flow<List<VideoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideo(video: VideoItem)

    @Query("UPDATE video_items SET lastPlaybackPosition = :position WHERE id = :id")
    suspend fun updatePlaybackPosition(id: String, position: Long)

    @Query("DELETE FROM video_items WHERE id = :id")
    suspend fun deleteVideoById(id: String)
}
