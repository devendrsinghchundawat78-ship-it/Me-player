package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "video_items")
data class VideoItem(
    @PrimaryKey val id: String, // Path or Streaming URL acts as unique key
    val title: String,
    val duration: Long = 0L,
    val size: Long = 0L,
    val lastPlaybackPosition: Long = 0L,
    val isStreaming: Boolean = false,
    val thumbnailUrl: String? = null,
    val addedDate: Long = System.currentTimeMillis()
) : Serializable
