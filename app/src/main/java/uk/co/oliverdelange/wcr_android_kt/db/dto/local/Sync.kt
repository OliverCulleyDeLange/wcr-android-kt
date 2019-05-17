package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Sync(@PrimaryKey(autoGenerate = true) val id: Long = 0,
                val epochSeconds: Long,
                val syncType: String
//                val successIds: String
)