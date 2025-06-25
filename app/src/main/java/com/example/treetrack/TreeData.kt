package com.example.treetrack

import com.google.firebase.database.IgnoreExtraProperties
import org.osmdroid.util.GeoPoint
import java.util.Date

@IgnoreExtraProperties
data class TreeData(
    var name: String = "",
    var description: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var datePlanted: Long = 0L,
    var health: Int = 100,
    var id: String = "",
    var healthImageBase64: String? = null,
    var uploadTime: String? = null
) {
    fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
    fun toDate(): Date = Date(datePlanted)
}