package com.cdpindia.location_demo

import android.Manifest.permission
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.location.*


class MainActivity : AppCompatActivity() {

    var context: Context = this
    var location_permission: TextView? = null
    lateinit var gps_permission: TextView
    lateinit var gps_button : Button
    lateinit var googleApiClient: GoogleApiClient
    lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    lateinit var mFusedLocationClient :FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val lm = context.getSystemService(LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false
        location_permission = findViewById(R.id.location_permission)
        gps_permission = findViewById(R.id.gps_permission)
        gps_button = findViewById(R.id.gps_button)

        if (checkLocationPermission()) {
            if (!checkGPSLocationOnOff()!!) {
                enableGPSLocation()
            } else {
                //set UI for enable gps location
                requestNewLocationData()
            }
        } else {
            gps_permission.text = "No GPS Location"
            customeAlertPrompt()
        }

    }

    private fun getCurrentLocation() {




        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        //        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
        if (checkLocationPermission()) {
            fusedLocationClient.lastLocation
                .addOnSuccessListener(
                    this
                ) { location ->
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Logic to handle location object
                        gps_permission.text = location.toString()
                        Toast.makeText(context, "" + location.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun checkGPSLocationOnOff(): Boolean? {
        var gps_enabled = false
        val lm = context.getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: java.lang.Exception) {
        }
        if (gps_enabled) {
            gps_enabled = true
        }
        Log.e("checkGPSLocationOnOff", "" + gps_enabled)
        return gps_enabled
    }

    private fun customeAlertPrompt() {
        AlertDialog.Builder(context)
            .setMessage("Enable Location Permission")
            .setPositiveButton(
                "ok"
            ) { paramDialogInterface, paramInt -> launchLocationPermission() }
            .setNegativeButton("cancel", null)
            .show()
    }

    private fun launchLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                permission.ACCESS_FINE_LOCATION,
                permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun checkLocationPermission(): Boolean {
        locationPermissionRequest = registerForActivityResult(RequestMultiplePermissions(),
            ActivityResultCallback<Map<String?, Boolean?>> { result: Map<String?, Boolean?> ->
                var fineLocationGranted: Boolean? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fineLocationGranted = result.getOrDefault(
                        permission.ACCESS_FINE_LOCATION, false
                    )
                }
                var coarseLocationGranted: Boolean? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    coarseLocationGranted = result.getOrDefault(
                        permission.ACCESS_COARSE_LOCATION, false
                    )
                }
                Toast.makeText(context, "" + result.toString(), Toast.LENGTH_SHORT).show()
                if (fineLocationGranted != null && fineLocationGranted) {
                    // Precise location access granted.
                    location_permission!!.text = "Precise location access granted."
                    enableGPSLocation()
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    // Only approximate location access granted.
                    location_permission!!.text = "Only approximate location access granted."
                    enableGPSLocation()
                } else {
                    // No location access granted.
                    location_permission!!.text = "No location access granted."
                    //context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    gps_button.visibility = View.VISIBLE
                }
            })
        return if (ContextCompat.checkSelfPermission(context, permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context, permission.ACCESS_COARSE_LOCATION
            ) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            false
        }
    }

    private fun enableGPSLocation() {
        val REQUEST_LOCATION = 2
        googleApiClient = GoogleApiClient.Builder(this@MainActivity)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : ConnectionCallbacks {
                override fun onConnected(bundle: Bundle?) {
                    Toast.makeText(context, "onConnected", Toast.LENGTH_SHORT).show()
                }

                override fun onConnectionSuspended(i: Int) {
                    googleApiClient.connect()
                    Toast.makeText(context, "onConnectionSuspended", Toast.LENGTH_SHORT).show()
                }
            })
            .addOnConnectionFailedListener { connectionResult ->
                Log.d(
                    "Location error",
                    "Location error " + connectionResult.errorCode
                )
            }.build()
        googleApiClient.connect()

        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = (1 * 1000).toLong()
        locationRequest.fastestInterval = 1000
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        builder.setAlwaysShow(true)
        val result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build())

        result.setResultCallback { result ->
            val status = result.status
            Toast.makeText(context, "status", Toast.LENGTH_SHORT).show()
            when (status.statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    status.startResolutionForResult(this@MainActivity, REQUEST_LOCATION)
                    Toast.makeText(context, "step 3", Toast.LENGTH_SHORT).show()
                    //                                finish();
                } catch (e: SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    fun getLocationLatLong(view: View) {
        if (checkGPSLocationOnOff()!!){
            requestNewLocationData()
        }else{
            enableGPSLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {

        var mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1
        //mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mFusedLocationClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback,
            Looper.myLooper()!!)

    }
    private val mLocationCallback = object : LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            var lastlocation = locationResult.lastLocation
            gps_permission.text = lastlocation.toString()
        }
    }
}

