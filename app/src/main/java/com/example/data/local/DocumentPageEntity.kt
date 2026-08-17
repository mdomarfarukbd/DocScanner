package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "document_pages",
    indices = [Index(value = ["documentId"])]
)
data class DocumentPageEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val pageNumber: Int,
    val rawEncryptedPath: String,
    val processedEncryptedPath: String,
    val cropCorners: String = "0.0,0.0;1.0,0.0;1.0,1.0;0.0,1.0",
    val filterType: String = "MAGIC_COLOR",
    val rotation: Int = 0,
    val ocrText: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
