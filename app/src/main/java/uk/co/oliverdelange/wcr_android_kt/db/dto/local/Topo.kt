package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("id"), Index("locationId")],
        foreignKeys = (arrayOf(
        ForeignKey(
                entity = Location::class,
                onUpdate = ForeignKey.CASCADE,
                parentColumns = arrayOf("id"),
                childColumns = arrayOf("locationId")
        )
)))
data class Topo(@PrimaryKey override var id: String = "",
                var locationId: String = "",
                var name: String = "",
                var image: String = "",
                override var uploadedAt: Long = -1,
                override var uploaderId: String = ""
) : BaseEntity()