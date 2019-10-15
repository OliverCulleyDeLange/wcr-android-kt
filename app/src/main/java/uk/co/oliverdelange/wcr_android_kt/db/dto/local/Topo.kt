package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude

@Entity(indices = [Index("id"), Index("locationId")],
        foreignKeys = (arrayOf(
                ForeignKey(
                        entity = Location::class,
                        onUpdate = ForeignKey.CASCADE,
                        parentColumns = arrayOf("id"),
                        childColumns = arrayOf("locationId")
                )
        )))
data class Topo(@get:Exclude @PrimaryKey(autoGenerate = true) override var id: Long = 0,
                @DocumentId override var firebaseId: DocumentReference?,
//                override var uuid: String = "",
                @get:Exclude var locationId: Long = 0,
//                @Ignore var locationFirebaseId: DocumentReference,
                var name: String = "",
                var image: String = "",
                override var uploadedAt: Long = -1,
                override var uploaderId: String = ""
) : BaseEntity()