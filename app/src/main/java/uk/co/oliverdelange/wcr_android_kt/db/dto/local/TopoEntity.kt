package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("id", unique = true), Index("locationId")],
        tableName = "Topo",
        foreignKeys = (arrayOf(
                ForeignKey(
                        entity = LocationEntity::class,
                        onUpdate = ForeignKey.CASCADE,
                        parentColumns = arrayOf("id"),
                        childColumns = arrayOf("locationId")
                )
        )))
data class TopoEntity(@PrimaryKey override var id: String = "",
                      var locationId: String = "",

                      var name: String = "",
                      var image: String = "",
                      override var uploadedAt: Long = -1,
                      override var uploaderId: String = ""
) : BaseEntity()