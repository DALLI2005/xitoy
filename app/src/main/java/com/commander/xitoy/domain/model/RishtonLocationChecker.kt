package com.commander.xitoy.domain.model

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine

data class LocationResult(
    val distanceKm: Double?,
    val latitude: Double?,
    val longitude: Double?
)

object RishtonLocationChecker {
    private const val RISHTON_LAT = 40.35667
    private const val RISHTON_LNG = 71.28472
    private const val RISHTON_RADIUS_KM = 18.0

    @SuppressLint("MissingPermission")
    suspend fun getLocationResult(context: Context): LocationResult {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return LocationResult(null, null, null)
        }

        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cancellationTokenSource = CancellationTokenSource()

            val location = suspendCancellableCoroutine<Location?> { cont ->
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                )
                    .addOnSuccessListener { loc -> cont.resume(loc) {} }
                    .addOnFailureListener { cont.resume(null) {} }

                cont.invokeOnCancellation { cancellationTokenSource.cancel() }
            } ?: return LocationResult(null, null, null)

            val results = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                RISHTON_LAT, RISHTON_LNG,
                results
            )
            LocationResult(
                distanceKm = results[0] / 1000.0,
                latitude = location.latitude,
                longitude = location.longitude
            )
        } catch (e: Exception) {
            LocationResult(null, null, null)
        }
    }

    suspend fun getDistanceFromRishtonKm(context: Context): Double? =
        getLocationResult(context).distanceKm

    fun isOutsideRishton(distanceKm: Double?): Boolean =
        distanceKm != null && distanceKm > RISHTON_RADIUS_KM
}
