package com.example.webunihw003_sporttracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.example.webunihw003_sporttracker.databinding.ActivityMainBinding
import com.example.webunihw003_sporttracker.location.MainLocationManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import java.util.*
import kotlin.concurrent.thread

class  MainActivity : AppCompatActivity(), OnMapReadyCallback, MainLocationManager.OnNewLocationAvailable
     {
    private lateinit var mainLocatoinManager: MainLocationManager
    private lateinit var binding: ActivityMainBinding
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment =  supportFragmentManager?.findFragmentById(R.id.frg_map) as SupportMapFragment?
        if (mapFragment != null) {
            mapFragment.onCreate(savedInstanceState)
            mapFragment.getMapAsync(this)
        }

        mainLocatoinManager = MainLocationManager(this, this)

        binding.buttonStart.setOnClickListener {
            if (previousLocation != null) {
                geocodeLocation(previousLocation!!)
            }
        }

        requestNeededPermission()



    }//ONCREATE

         override fun onMapReady(googleMap: GoogleMap?) {
             googleMap?.let {
                 mMap = googleMap
                 mMap.uiSettings.isCompassEnabled = true
                 mMap.uiSettings.isZoomControlsEnabled = true
                 mMap.uiSettings.isMapToolbarEnabled = true
                 mMap.uiSettings.isMyLocationButtonEnabled = true
             }
         }

    //--------------------------------------------------------------------------------------------
    fun checkGlobalLocationSettings() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest!!)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())


        task.addOnSuccessListener { locationSettingsResponse ->
            Toast.makeText(
                this,
                "Location enabled: ${locationSettingsResponse.locationSettingsStates.isLocationUsable}",
                Toast.LENGTH_LONG
            ).show()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {

                Toast.makeText(this, "${exception.message}", Toast.LENGTH_LONG).show()

                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@MainActivity,
                        1001
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }
    //--------------------------------------------------------------------------------------------

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Toast.makeText(this, "OK - LOCATION $requestCode", Toast.LENGTH_LONG).show()

        super.onActivityResult(requestCode, resultCode, data)
    }

    fun requestNeededPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                101
            )
        } else {
            // we have the permission
            handleLocationStart()
        }
    }

    private fun showLastKnownLocation() {
        mainLocatoinManager.getLastLocation { location ->
            binding.textviewLocation.text = getLocationText(location)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            101 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "ACCESS_FINE_LOCATION perm granted", Toast.LENGTH_SHORT)
                        .show()

                    handleLocationStart()
                } else {
                    Toast.makeText(
                        this,
                        "ACCESS_FINE_LOCATION perm NOT granted", Toast.LENGTH_SHORT
                    ).show()
                }
                return
            }
        }
    }

    private fun handleLocationStart() {
        checkGlobalLocationSettings()
        showLastKnownLocation()
        mainLocatoinManager.startLocationMonitoring()
    }

    var previousLocation: Location? = null
    var distance: Float = 0f

    override fun onNewLocation(location: Location) {
        binding.textviewLocation.text = getLocationText(location)

        if (previousLocation != null && location.accuracy < 20) {
            if (previousLocation!!.time < location.time) {
                distance += previousLocation!!.distanceTo(location)
                binding.textviewDistance.text = "$distance m"
            }
        }

        previousLocation = location
    }

    private fun getLocationText(location: Location): String {
        return """
            Provider: ${location.provider}
            Latitude: ${location.latitude}
            Longitude: ${location.longitude}
            Accuracy: ${location.accuracy}
            Altitude: ${location.altitude}
            Speed: ${location.speed}
            Time: ${Date(location.time).toString()}
        """.trimIndent()
    }

    private fun geocodeLocation(location: Location) {
        thread {
            try {
                val gc = Geocoder(this, Locale.getDefault())
                var addrs: List<Address> =
                    gc.getFromLocation(location.latitude, location.longitude, 3)
                val addr =
                    "${addrs[0].getAddressLine(0)}, ${addrs[0].getAddressLine(1)}, ${addrs[0].getAddressLine(2)}"

                runOnUiThread {
                    Toast.makeText(this, addr, Toast.LENGTH_LONG).show()

                    mMap.apply {
                            addMarker(MarkerOptions().apply {
                                position(LatLng(location.latitude,location.longitude))
                                title("Hely")
                                snippet("Valami")
                            })
                        }
                        //focus on user's location!
                        goToMyLocation()
                    }


            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun goToMyLocation(): Boolean {
        this.let {
            val locationManager = it.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            location.latitude,
                            location.longitude
                        ), 12.0f
                    )
                )
            }
        }
        return true
    }




    override fun onDestroy() {
        super.onDestroy()
        mainLocatoinManager.stopLocationMonitoring()
    }
}