package com.cdpindia.location_demo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.internal.ConnectionCallbacks
import com.google.android.gms.location.*
import com.google.android.gms.tasks.OnCompleteListener


class LocationActivity: AppCompatActivity() {
    private lateinit var latitude: TextView
    private lateinit var longitude: TextView
    private lateinit var getLoc: Button
    val PERMISSION_ID = 42
    lateinit var mFusedLocationClient: FusedLocationProviderClient

    lateinit var googleApiClient: GoogleApiClient
    lateinit var sharedpreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        latitude = findViewById(R.id.latitude1)
        longitude = findViewById(R.id.longitude1)
        getLoc = findViewById(R.id.getlocation1)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sharedpreferences = getSharedPreferences("loc", Context.MODE_PRIVATE);


        getLoc.setOnClickListener {
            getLastLocation()
        }
        if(checkPermissions()){
            getLastLocation()
        }else{
            customeAlertPrompt()
        }
    }



    private fun customeAlertPrompt() {

        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Alert")
        alertDialog.setMessage("Turn on your System Location")

        alertDialog.setPositiveButton("Ok") { _, _ ->
            getLastLocation()
        }
        alertDialog.create()
        alertDialog.show()
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        if (checkPermissions()){
            if (isLocationEnable()){
                mFusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->

                    var location = task.result

                    if (location == null){
                        requestNewLocationData()
                        enableGPSLocation()
                    }
                    else{
                        latitude.text = location.latitude.toString()
                        longitude.text = location.longitude.toString()
                    }

                }
            }
            else{
                enableGPSLocation()
            }
        }
        else{
            requestPermission()
        }
    }

    private fun requestPermission() {

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_ID) {


            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            }
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_DENIED)) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //Show permission explanation dialog...
                    Log.e("one","one");
                }else{
                    //startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS));
                    //Never ask again selected, or device policy prohibits the app from having that permission.
                    //So, disable that feature, or fall back to another situation...
                    Log.e("two","two");
                    val i = Intent()
                    i.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    i.addCategory(Intent.CATEGORY_DEFAULT)
                    i.data = Uri.parse("package:" + getPackageName())
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    startActivity(i)
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
        {
            return true
        }
        return false

    }

    private fun isLocationEnable(): Boolean {

        var locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

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
            latitude.text= lastlocation.latitude.toString()
            longitude.text = lastlocation.longitude.toString()
        }
    }

    private fun enableGPSLocation() {
        val REQUEST_LOCATION = 2
        googleApiClient = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : ConnectionCallbacks,
                GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(bundle: Bundle?) {
                   // Toast.makeText(this, "onConnected", Toast.LENGTH_SHORT).show()
                }

                override fun onConnectionSuspended(i: Int) {
                    googleApiClient.connect()
                   // Toast.makeText(this, "onConnectionSuspended", Toast.LENGTH_SHORT).show()
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

        var result = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build())
        result.addOnCompleteListener(OnCompleteListener<LocationSettingsResponse?> { task ->
            try {
                val response = task.getResult(ApiException::class.java)
                Log.e("location_response",response.toString())
            } catch (exception: ApiException) {
                when (exception.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED ->                         // Location settings are not satisfied. But could be fixed by showing the
                        // user a dialog.
                        try {
                            // Cast to a resolvable exception.
                            val resolvable = exception as ResolvableApiException
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            resolvable.startResolutionForResult(this, 2001)
                            //break
                        } catch (e: IntentSender.SendIntentException) {
                            // Ignore the error.
                        } catch (e: ClassCastException) {
                            // Ignore, should be an impossible error.
                        }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {}
                }
            }
        });
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2001 && resultCode == Activity.RESULT_OK) {
            getLastLocation()
        }
    }
}