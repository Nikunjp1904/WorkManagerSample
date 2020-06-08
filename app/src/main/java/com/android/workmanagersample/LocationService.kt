package com.android.workmanagersample

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task

class LocationService : Service() {

    private var mHandler: Handler = Handler()
    private var mRunnable: Runnable = Runnable {  }
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mLastLocation: Location? = null

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

        mRunnable = Runnable {  getLastLocation() }
        mHandler.postDelayed(mRunnable, 7000)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacks(mRunnable)

    }

    // Get current Location
    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        mFusedLocationClient!!.lastLocation.addOnCompleteListener { task: Task<Location> ->
            if (task.isSuccessful && task.result != null) {
                mLastLocation = task.result
                val locationString = mLastLocation?.latitude.toString() + "," + mLastLocation?.longitude.toString()
                Toast.makeText(this,locationString, Toast.LENGTH_SHORT).show()
                mHandler.postDelayed(mRunnable, 7000)
            }
        }
    }
}