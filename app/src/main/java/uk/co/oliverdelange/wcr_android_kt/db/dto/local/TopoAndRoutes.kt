package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Embedded
import androidx.room.Relation

class TopoAndRoutes {
    @Embedded
    var topo: TopoEntity = TopoEntity()
    @Relation(parentColumn = "id", entityColumn = "topoId")
    var routes: MutableList<RouteEntity> = mutableListOf()
}