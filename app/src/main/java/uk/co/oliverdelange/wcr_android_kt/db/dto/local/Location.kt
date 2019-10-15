package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.Exclude

//https://stackoverflow.com/questions/40116244/firebase-exclude-with-kotlin-data-class

//@Parcelize
@Entity
data class Location(@PrimaryKey(autoGenerate = true) @get:Exclude override var id: Long = 0,
                    @DocumentId override var firebaseId: DocumentReference?,
//                    override var uuid: String = "",
                    @get:Exclude var parentLocationId: Long? = null,
//                @Ignore var parentLocationFirebaseId: DocumentReference,
                    var name: String = "",
                    var lat: Double = 0.0,
                    var lng: Double = 0.0,
                    var type: String = "",
                    override var uploadedAt: Long = -1,
                    override var uploaderId: String = ""
) : BaseEntity()