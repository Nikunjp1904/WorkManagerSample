package com.android.workmanagersample

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.concurrent.futures.ResolvableFuture
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.common.util.concurrent.ListenableFuture

class LocationWorker(context: Context, workerParams: WorkerParameters) : ListenableWorker(context, workerParams) {

    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLastLocation: Location? = null
    private var intent: Intent? = null
    lateinit var mFuture: ResolvableFuture<Result>
    lateinit var locationManager: LocationManager

    override fun startWork(): ListenableFuture<Result> {
        mFuture = ResolvableFuture.create()

        if (isLocationEnabled() && checkPermission()) {

            val requestType = inputData.getString(applicationContext.getString(R.string.b_input_data))

            if (requestType == "1") {
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

                getLastLocation()
            } else if (requestType == "2") {
                if (!isServiceRunning(LocationService::class.java)) {
                    intent = Intent(applicationContext, LocationService::class.java)
                    applicationContext.startService(intent)
                }
            } else {
                if (!isServiceRunning(LocationService::class.java)) {
                    applicationContext.stopService(intent)
                }
            }
        }

        return mFuture
    }

    // Get current Location
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        mFusedLocationClient!!.lastLocation.addOnCompleteListener { task: Task<Location> ->
            if (task.isSuccessful && task.result != null) {
                mLastLocation = task.result

                val outputData = Data.Builder()
                    .putString(applicationContext.getString(R.string.b_lat), mLastLocation?.latitude.toString())
                    .putString(applicationContext.getString(R.string.b_lon), mLastLocation?.longitude.toString())
                    .build()

                mFuture.set(Result.success(outputData))
            }
        }
    }

    // Check Location permission
    private fun checkPermission(): Boolean {
        val permission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            val outputDataError = Data.Builder()
                .putString(applicationContext.getString(R.string.b_error_name), "Please give Permission")
                .putString(applicationContext.getString(R.string.b_error_code), "1")
                .build()
            mFuture.set(Result.failure(outputDataError))
            return false
        } else {
            return true
        }
    }

    // Check GPS is on or not
    private fun isLocationEnabled(): Boolean {
        locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )
        ) {
            return true
        } else {
            val outputDataError = Data.Builder()
                .putString(applicationContext.getString(R.string.b_error_name), "Please turn on location")
                .putString(applicationContext.getString(R.string.b_error_code), "2")
                .build()
            mFuture.set(Result.failure(outputDataError))
            return false
        }
    }

    // Check Service is running or not
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = applicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        @Suppress("deprecation")
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                // If the service is running then return true
                return true
            }
        }
        return false
    }
}