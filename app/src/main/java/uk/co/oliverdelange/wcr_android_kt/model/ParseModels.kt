package uk.co.oliverdelange.wcr_android_kt.model

import com.parse.ParseClassName
import com.parse.ParseObject
import org.json.JSONArray

@ParseClassName("Location")
class ParseLocation() : ParseObject() {

    constructor(locationId: Long,
                parentId: Long?,
                name: String,
                lat: Double,
                lng: Double,
                type: String) : this() {
        this.id = locationId
        parentId?.let { this.parentId = it }
        this.name = name
        this.lat = lat
        this.lng = lng
        this.type = type
    }

    var id: Long
        get() {
            return getLong("id")
        }
        set(value) {
            put("id", value)
        }
    var parentId: Long
        get() {
            return getLong("parentId")
        }
        set(value) {
            put("parentId", value)
        }
    var name: String
        get() {
            return getString("name")
        }
        set(value) {
            put("name", value)
        }
    var lat: Double
        get() {
            return getDouble("lat")
        }
        set(value) {
            put("lat", value)
        }
    var lng: Double
        get() {
            return getDouble("lng")
        }
        set(value) {
            put("lng", value)
        }
    var type: String
        get() {
            return getString("type")
        }
        set(value) {
            put("type", value)
        }
}

@ParseClassName("Topo")
class ParseTopo() : ParseObject() {

    constructor(topoId: Long,
                name: String,
                image: String,
                routes: List<ParseRoute>) : this() {
        this.id = topoId
        this.name = name
        this.image = image
        this.routes = routes
    }

    var id: Long
        get() {
            return getLong("id")
        }
        set(value) {
            put("id", value)
        }
    var name: String
        get() {
            return getString("name")
        }
        set(value) {
            put("name", value)
        }
    var image: String
        get() {
            return getString("type")
        }
        set(value) {
            put("type", value)
        }
    var routes: List<ParseRoute>
        get() {
            return getList("routes")
        }
        set(value) {
            put("routes", value)
        }
}

@ParseClassName("Route")
class ParseRoute() : ParseObject() {

    constructor(routeId: Long?,
                name: String,
                grade: String?,
                type: String?,
                description: String?,
                path: JSONArray) : this() {
        this.id = routeId
        this.name = name
        this.grade = grade
        this.type = type
        this.description = description
        this.path = path
    }

    var id: Long?
        get() {
            return getLong("id")
        }
        set(value) {
            put("id", value)
        }
    var name: String
        get() {
            return getString("name")
        }
        set(value) {
            put("name", value)
        }
    var grade: String?
        get() {
            return getString("grade")
        }
        set(value) {
            put("grade", value)
        }
    var type: String?
        get() {
            return getString("type")
        }
        set(value) {
            put("type", value)
        }
    var description: String?
        get() {
            return getString("description")
        }
        set(value) {
            put("description", value)
        }
    var path: JSONArray
        get() {
            return getJSONArray("path")
        }
        set(value) {
            put("path", value)
        }
}