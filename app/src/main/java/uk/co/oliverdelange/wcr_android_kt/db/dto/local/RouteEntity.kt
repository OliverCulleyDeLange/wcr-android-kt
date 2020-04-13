package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("id", unique = true), Index("topoId"), Index("name")],
        tableName = "Route",
        foreignKeys = [(
                ForeignKey(
                        entity = TopoEntity::class,
                        parentColumns = arrayOf("id"),
                        childColumns = arrayOf("topoId"))
                )])
data class RouteEntity(@PrimaryKey override var id: String = "",
                       var topoId: String = "",
                       var name: String = "",
                       var grade: String = "",
                       var gradeColour: String = "",
                       var type: String = "",
                       var description: String = "",
                       var path: String = "",
                       override var uploadedAt: Long = -1,
                       override var uploaderId: String = ""
) : BaseEntity() {
    //FIXME this was done to order the paths on the topo from left to right
    // Now i look back, its a bad way of doing it as any other comparison will now only
    // compare the first x coord of the route path, nothing else.
    // Write a custom sort function for routes, that doesn't override the compare of the whole class.
    // Commented out and will re-implement properly later
//    override fun compareTo(other: RouteEntity): Int {
//        fun getFirstXCoord(pathString: String): Float {
//            return stringToCoordsSet(pathString)?.flatten()?.first()?.first ?: 0f
//        }
//        return getFirstXCoord(path).compareTo(getFirstXCoord(other.path))
//    }
}