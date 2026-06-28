package com.example.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VideoRepository(private val videoDao: VideoDao) {

    val allVideos: Flow<List<VideoItem>> = videoDao.getAllVideos()

    suspend fun insertVideo(video: VideoItem) = withContext(Dispatchers.IO) {
        videoDao.insertVideo(video)
    }

    suspend fun updatePlaybackPosition(id: String, position: Long) = withContext(Dispatchers.IO) {
        videoDao.updatePlaybackPosition(id, position)
    }

    suspend fun deleteVideo(id: String) = withContext(Dispatchers.IO) {
        videoDao.deleteVideoById(id)
    }

    // Scans the Android device's MediaStore for local video files
    suspend fun scanLocalVideos(context: Context) = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val queryUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        context.contentResolver.query(
            queryUri,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn) ?: "Video_$id"
                val duration = cursor.getLong(durationColumn)
                val size = cursor.getLong(sizeColumn)
                val dateAdded = cursor.getLong(dateColumn) * 1000L // convert to ms

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                ).toString()

                val videoItem = VideoItem(
                    id = contentUri,
                    title = name,
                    duration = duration,
                    size = size,
                    isStreaming = false,
                    thumbnailUrl = null,
                    addedDate = dateAdded
                )
                // Save it to database if it's not already added or to keep metadata synchronized
                videoDao.insertVideo(videoItem)
            }
        }
    }
}
