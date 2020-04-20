package uk.co.oliverdelange.wcr_android_kt.mapper

import timber.log.Timber
import uk.co.oliverdelange.wcr_android_kt.model.PathSegment
import uk.co.oliverdelange.wcr_android_kt.model.flattened

//TODO Test me
fun coordsSetToString(coords: List<PathSegment>): String {
    return coords.flattened().let {
        it.joinToString(",", transform = { pair -> "${pair.first}:${pair.second}" })
    }
}

fun stringToCoordsSet(coords: String?): List<PathSegment>? {
    return coords?.let {
        try {
            val stringPairs = coords.split(",")
            val pathSegment = stringPairs.map { stringPair ->
                val parts = stringPair.split(":").map { it.toFloat() }
                Pair(parts[0], parts[1])
            }
            listOf(PathSegment(pathSegment)) //Not a true re-incarnation of the action stack
        } catch (e: Exception) {
            Timber.e(e, "Error whilst converting topo route path string into Set<Pair<Int, Int>>")
            emptyList()
        }
    }
}
