package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude

@Entity(indices = [Index("id"), Index("topoId")],
        foreignKeys = [(
                ForeignKey(
                        entity = Topo::class,
                        parentColumns = arrayOf("id"),
                        childColumns = arrayOf("topoId"))
                )])
data class Route(@get:Exclude @PrimaryKey(autoGenerate = true) override var id: Long = 0,
                 @DocumentId override var firebaseId: DocumentReference?,
//                 override var uuid: String = "",
                 @get:Exclude var topoId: Long = 0,
//                 @Ignore var topoFirebaseId: DocumentReference,
                 var name: String = "",
                 var grade: String = "",
                 var gradeColour: String = "",
                 var type: String = "",
                 var description: String = "",
                 var path: String = "",
                 override var uploadedAt: Long = -1,
                 override var uploaderId: String = ""
) : BaseEntity()