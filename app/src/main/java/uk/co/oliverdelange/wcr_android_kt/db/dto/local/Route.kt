package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("id", unique = true), Index("topoId"), Index("name")],
        foreignKeys = [(
                ForeignKey(
                        entity = Topo::class,
                        parentColumns = arrayOf("id"),
                        childColumns = arrayOf("topoId"))
                )])
data class Route(@PrimaryKey override var id: String = "",
                 var topoId: String = "",

                 var name: String = "",
                 var grade: String = "",
                 var gradeColour: String = "",
                 var type: String = "",
                 var description: String = "",
                 var path: String = "",
                 override var uploadedAt: Long = -1,
                 override var uploaderId: String = ""
) : BaseEntity()