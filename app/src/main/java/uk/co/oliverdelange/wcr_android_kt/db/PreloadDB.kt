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
                Location(1, null, null, null, "L0 London", 51.507363, -0.127755, "CRAG"),
                Location(2, null, null, null, "L1 Derby", 52.923429, -1.471682, "CRAG"),
                Location(3, null, 1, null, "L2 The Arch - Biscuit Factory", 51.494365, -0.062352, "SECTOR")
        )
        instance.topoDao().insertMany(
                Topo(1, null, 3, null, "T0 Buttery Biscuit Base", "http://via.placeholder.com/640x480"),
                Topo(2, null, 3, null, "T1 The Dunker", "http://via.placeholder.com/480x640"),
                Topo(3, null, 3, null, "T2 Rich T", "http://via.placeholder.com/1280x480"),
                Topo(4, null, 3, null, "T3 Caramel Digestif", "http://via.placeholder.com/1280x480")
        )
        instance.routeDao().insertMany(
                Route(1, null, 1, null, "0GREEN ROUTE", "V0", "GREEN", "BOULDERING", "Eating biscuits is good for you", "0.25:0.25,0.25:0.90"),
                Route(2, null, 1, null, "0ORANGE ROUTE", "f4+", "ORANGE", "BOULDERING", "Mmmmm creamy custard", "0.35:0.25,0.35:0.90"),
                Route(3, null, 1, null, "0RED ROUTE", "E1 5b", "RED", "TRAD", "Traditional Rich Tea or Digestive?", "0.45:0.25,0.45:0.90"),
                Route(4, null, 1, null, "0BLACK ROUTE", "8a", "BLACK", "SPORT", "Excuisite", "0.55:0.25,0.55:0.90"),
                Route(5, null, 2, null, "1A biscuit based name that is really long so we know if things look okay when there are really long names", "4b", "GREEN", "SPORT", "Lol...", "0.65:0.25,0.65:0.90"),
                Route(6, null, 3, null, "2GREEN ROUTE", "V0", "GREEN", "BOULDERING", "Eating biscuits is good for you", "0.25:0.25,0.25:0.90"),
                Route(7, null, 3, null, "2ORANGE ROUTE", "f4+", "ORANGE", "BOULDERING", "Mmmmm creamy custard", "0.35:0.25,0.35:0.90"),
                Route(8, null, 3, null, "2RED ROUTE", "E1 5b", "RED", "TRAD", "Traditional Rich Tea or Digestive?", "0.45:0.25,0.45:0.90"),
                Route(9, null, 3, null, "2BLACK ROUTE", "8a", "BLACK", "SPORT", "Excuisite", "0.55:0.25,0.55:0.90"),
                Route(10, null, 4, null, "3A biscuit based name that is really long so we know if things look okay when there are really long names", "4b", "GREEN", "SPORT", "Lol...", "0.75:0.25,0.75:0.90")
        )
    }
}