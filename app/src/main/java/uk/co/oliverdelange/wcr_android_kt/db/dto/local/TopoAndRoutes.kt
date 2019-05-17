package uk.co.oliverdelange.wcr_android_kt.db.dto.local

import androidx.room.Embedded
import androidx.room.Relation

class TopoAndRoutes {
    @Embedded
    lateinit var topo: Topo
    @Relation(parentColumn = "id", entityColumn = "topoId")
    lateinit var routes: List<Route>
}