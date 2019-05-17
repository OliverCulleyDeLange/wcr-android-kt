package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Parcelize
@Entity
data class Location(@PrimaryKey override var id: String = "",
                    var parentLocation: String? = null,
                    var name: String = "",
                    var lat: Double = 0.0,
                    var lng: Double = 0.0,
                    var type: String = "",
                    var greens: Int = 0,
                    var oranges: Int = 0,
                    var reds: Int = 0,
                    var blacks: Int = 0,
                    var boulders: Int = 0,
                    var sports: Int = 0,
                    var trads: Int = 0,
                    override var uploadedAt: Long = -1,
                    var uploaderId: String = ""
) : Parcelable, BaseEntity()