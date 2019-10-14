package uk.co.oliverdelange.wcr_android_kt.mapper

import timber.log.Timber

fun coordsSetToString(coords: List<Pair<Float, Float>>?): String? {
    return coords?.let {
        it.joinToString(",", transform = { pair -> "${pair.first}:${pair.second}" })
    }
}

fun stringToCoordsSet(coords: String?): List<Pair<Float, Float>>? {
    return coords?.let {
        try {

            val stringPairs = coords.split(",")
            val pairs = stringPairs.map {
                val parts = it.split(":").map { it.toFloat() }
                Pair(parts[0], parts[1])
            }
            pairs
        } catch (e: Exception) {
            Timber.e(e, "Error whilst converting topo route path string into Set<Pair<Int, Int>>")
            emptyList()
        }
    }
}
