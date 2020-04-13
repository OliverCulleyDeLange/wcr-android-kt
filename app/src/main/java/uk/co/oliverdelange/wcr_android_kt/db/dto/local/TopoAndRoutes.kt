package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Embedded
import androidx.room.Relation

//    https://stackoverflow.com/questions/48315261/using-rooms-relation-with-order-by

class TopoAndRoutes {
    @Embedded
    var topo: TopoEntity = TopoEntity()
    @Relation(parentColumn = "id", entityColumn = "topoId")
    var routes: MutableList<RouteEntity> = mutableListOf()
        set(v) {
            // FIXME WTF
            // Don't do this here wtf. This is just a DTO. Sort in the view model as thats handles
            // logic for how things look on screen. Commenting out and will re-implement
//            v.sort()
            field = v
        }
}