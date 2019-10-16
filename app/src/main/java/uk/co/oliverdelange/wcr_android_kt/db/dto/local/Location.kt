package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// https://stackoverflow.com/questions/40116244/firebase-exclude-with-kotlin-data-class
// All fields must have default value for kotlin to supply a no args constructor, which is required
// for (de)serialisation
//@Parcelize
@Entity(indices = [Index("id", unique = true), Index("parentLocationId"), Index("name")])
data class Location(@PrimaryKey override var id: String = "",
                    var parentLocationId: String? = "",

                    var name: String = "",
                    var lat: Double = 0.0,
                    var lng: Double = 0.0,
                    var type: String = "",
                    override var uploadedAt: Long = -1,
                    override var uploaderId: String = ""
) : BaseEntity()