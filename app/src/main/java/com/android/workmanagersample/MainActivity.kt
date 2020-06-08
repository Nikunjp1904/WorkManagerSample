package com.android.workmanagersample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import androidx.work.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var oneTimeRequest: OneTimeWorkRequest
    lateinit var periodicRequest: PeriodicWorkRequest
    lateinit var cancelRequest: OneTimeWorkRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val constraints =
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val inputDataOneTime = Data.Builder()
                .putString(getString(R.string.b_input_data), "1")
                .build()

        val inputDataPeriodic = Data.Builder()
                .putString(getString(R.string.b_input_data), "2")
                .build()

        val inputDataStop = Data.Builder()
                .putString(getString(R.string.b_input_data), "3")
                .build()

        oneTimeRequest = OneTimeWorkRequest.Builder(LocationWorker::class.java)
                .setConstraints(constraints)
                .setInputData(inputDataOneTime)
                .build()

        periodicRequest = PeriodicWorkRequest.Builder(LocationWorker::class.java, 16, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(inputDataPeriodic)
                .build()

        cancelRequest = OneTimeWorkRequest.Builder(LocationWorker::class.java)
                .setConstraints(constraints)
                .setInputData(inputDataStop)
                .build()

        initView()
    }

    private fun initView() {
        btnOneTimeRequest.setOnClickListener(this)
        btnPeriodicRequest.setOnClickListener(this)
        btnCancelRequest.setOnClickListener(this)

        // Observe one time work request status
        WorkManager.getInstance().getWorkInfoByIdLiveData(oneTimeRequest.id)
                .observe(this, Observer { t ->
                    // Handle work success
                    if (t?.state == WorkInfo.State.SUCCEEDED) {
                        val locString = t.outputData.getString(getString(R.string.b_lat)) + " " + t.outputData.getString(getString(R.string.b_lon))
                        tvInfo.append(locString + "\n")
                    } else if (t?.state == WorkInfo.State.FAILED) { // Handle Work failed output
                        Toast.makeText(this, t.outputData.getString(getString(R.string.b_error_name)), Toast.LENGTH_SHORT)
                                .show()
                        val errorcode = t.outputData.getString(getString(R.string.b_error_code))
                        if (errorcode == "1") {
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
                        } else {
                            showAlert()
                        }
                    }
                })

        // Observe periodic work request status
        WorkManager.getInstance().getWorkInfoByIdLiveData(periodicRequest.id)
                .observe(this, Observer { t ->
                    if (t?.state?.isFinished!!) {
                        val locString = t.outputData.getString(getString(R.string.b_lat)) + " " + t.outputData.getString(getString(R.string.b_lon))
                        tvInfo.append(locString + "\n")
                    }
                })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnOneTimeRequest -> {
                tvInfo.text = ""
                WorkManager.getInstance()
                        .enqueueUniqueWork("location", ExistingWorkPolicy.REPLACE, oneTimeRequest)
            }
            R.id.btnPeriodicRequest -> {
                tvInfo.text = ""
                WorkManager.getInstance().enqueueUniquePeriodicWork("location",
                        ExistingPeriodicWorkPolicy.REPLACE, periodicRequest)
            }
            R.id.btnCancelRequest -> {
                tvInfo.text = ""
                WorkManager.getInstance()
                        .enqueueUniqueWork("location", ExistingWorkPolicy.REPLACE, cancelRequest)
                WorkManager.getInstance().cancelAllWork()
            }
        }
    }

    // Show location enable alert
    private fun showAlert() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " + "use this app")
                .setPositiveButton("Location Settings") { _, _ ->
                    val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(myIntent)
                }.setNegativeButton("Cancel") { _, _ -> }
        dialog.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            100 -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "You Cannot use this functionality", Toast.LENGTH_SHORT)
                            .show()
                } else {
                    WorkManager.getInstance()
                            .enqueueUniqueWork("location", ExistingWorkPolicy.REPLACE, oneTimeRequest)
                }
            }
        }
    }
}