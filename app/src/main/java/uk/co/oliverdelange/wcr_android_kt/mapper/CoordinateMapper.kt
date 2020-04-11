package uk.co.oliverdelange.wcr_android_kt.mapper

import timber.log.Timber

//TODO Test me
fun coordsSetToString(coords: List<List<Pair<Float, Float>>>?): String? {
    return coords?.flatten()?.let {
        it.joinToString(",", transform = { pair -> "${pair.first}:${pair.second}" })
    }
}

fun stringToCoordsSet(coords: String?): List<List<Pair<Float, Float>>>? {
    return coords?.let {
        try {
            val stringPairs = coords.split(",")
            val pairs = stringPairs.map { stringPair ->
                val parts = stringPair.split(":").map { it.toFloat() }
                Pair(parts[0], parts[1])
            }
            listOf(pairs) //Not a true re-incarnation of the action stack
        } catch (e: Exception) {
            Timber.e(e, "Error whilst converting topo route path string into Set<Pair<Int, Int>>")
            emptyList()
        }
    }
}
