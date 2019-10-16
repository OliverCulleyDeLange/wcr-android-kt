package uk.co.oliverdelange.wcr_android_kt.db

import io.reactivex.Completable
import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Location
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Route
import uk.co.oliverdelange.wcr_android_kt.db.dto.local.Topo

fun preload(instance: WcrDb): Completable {
    return Completable.fromAction {
        Timber.d("Preloading DB with data")
        instance.locationDao().insertMany(
                Location("L0", null, "L0 London", 51.507363, -0.127755, "CRAG"),
                Location("L1", null, "L1 Derby", 52.923429, -1.471682, "CRAG"),
                Location("L2", "L0", "L2 The Arch - Biscuit Factory", 51.494365, -0.062352, "SECTOR")
        )
        instance.topoDao().insertMany(
                Topo("T0", "L2", "T0 Buttery Biscuit Base", "http://via.placeholder.com/640x480"),
                Topo("T1", "L2", "T1 The Dunker", "http://via.placeholder.com/480x640"),
                Topo("T2", "L2", "T2 Rich T", "http://via.placeholder.com/1280x480"),
                Topo("T3", "L2", "T3 Caramel Digestif", "http://via.placeholder.com/1280x480")
        )
        instance.routeDao().insertMany(
                Route("R0", "T0", "0GREEN ROUTE", "V0", "GREEN", "BOULDERING", "Eating biscuits is good for you", "0.25:0.25,0.25:0.90"),
                Route("R1", "T0", "0ORANGE ROUTE", "f4+", "ORANGE", "BOULDERING", "Mmmmm creamy custard", "0.35:0.25,0.35:0.90"),
                Route("R2", "T0", "0RED ROUTE", "E1 5b", "RED", "TRAD", "Traditional Rich Tea or Digestive?", "0.45:0.25,0.45:0.90"),
                Route("R3", "T0", "0BLACK ROUTE", "8a", "BLACK", "SPORT", "Excuisite", "0.55:0.25,0.55:0.90"),
                Route("R4", "T1", "1A biscuit based name that is really long so we know if things look okay when there are really long names", "4b", "GREEN", "SPORT", "Lol...", "0.65:0.25,0.65:0.90"),
                Route("R5", "T2", "2GREEN ROUTE", "V0", "GREEN", "BOULDERING", "Eating biscuits is good for you", "0.25:0.25,0.25:0.90"),
                Route("R6", "T2", "2ORANGE ROUTE", "f4+", "ORANGE", "BOULDERING", "Mmmmm creamy custard", "0.35:0.25,0.35:0.90"),
                Route("R7", "T2", "2RED ROUTE", "E1 5b", "RED", "TRAD", "Traditional Rich Tea or Digestive?", "0.45:0.25,0.45:0.90"),
                Route("R8", "T2", "2BLACK ROUTE", "8a", "BLACK", "SPORT", "Excuisite", "0.55:0.25,0.55:0.90"),
                Route("R9", "T3", "3A biscuit based name that is really long so we know if things look okay when there are really long names", "4b", "GREEN", "SPORT", "Lol...", "0.75:0.25,0.75:0.90")
        )
    }
}