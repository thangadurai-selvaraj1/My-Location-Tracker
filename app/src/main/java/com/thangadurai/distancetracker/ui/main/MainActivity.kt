package com.thangadurai.distancetracker.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.ButtCap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.thangadurai.distancetracker.R
import com.thangadurai.distancetracker.databinding.ActivityMainBinding
import com.thangadurai.distancetracker.service.TrackerService
import com.thangadurai.distancetracker.utils.Constants
import com.thangadurai.distancetracker.utils.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnClickListener {

    private lateinit var mMap: GoogleMap
    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel by viewModels<MainViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        setUpMapViews()
        setUpViews()
    }

    private fun observeServices() {
        TrackerService.locationList.observe(this) {
            if (it.isNotEmpty())
                drawPolyLines(it.last())
        }

        TrackerService.serviceIsStarted.observe(this) {
            mainBinding.button.text = if (it) {
                getString(R.string.stop)
            } else {
                getString(R.string.start)
            }
        }
    }

    private fun observeCurrentLocation() {
        viewModel.currentLocation.observe(this) {
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(it.latitude, it.longitude),
                    15f
                ),
            )
        }
    }

    private fun setUpMapViews() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setUpViews() {
        mainBinding.button.setOnClickListener(this)
    }

    override fun onMapReady(map: GoogleMap) {
        mMap = map
        if (PermissionUtils.hasPermissions(this)) {
            setUpMapSettings()
            viewModel.getCurrentLocation(fusedLocationClient)
            observeCurrentLocation()
            observeServices()
        } else {
            Toast.makeText(this, "Pls enable the location Permissions", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun setUpMapSettings() {
        if (PermissionUtils.hasPermissions(this)) {
            mMap.apply {
                uiSettings.isMyLocationButtonEnabled = true
                isMyLocationEnabled = true
            }
        } else {
            Toast.makeText(this, "Pls enable the location Permissions", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawPolyLines(lastLocation: LatLng) {
        mMap.addPolyline(PolylineOptions().apply {
            color(Color.BLACK)
            width(10f)
            jointType(JointType.ROUND)
            startCap(ButtCap())
            endCap(ButtCap())
            add(LatLng(lastLocation.latitude, lastLocation.longitude))
        })
        mMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(lastLocation.latitude, lastLocation.longitude),
                15f
            ),
        )
    }

    private fun startOrStopTrackerService(startOrStopFlag: String) {
        Intent(
            this,
            TrackerService::class.java
        ).apply {
            action = startOrStopFlag
            startService(this)
        }
    }

    override fun onClick(v: View) {
        mainBinding.button.apply {
            when (v) {
                this -> {
                    if (!PermissionUtils.hasPermissions(this@MainActivity)) {
                        Toast.makeText(
                            this@MainActivity,
                            "Pls enable the location Permissions",
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    text = if (text == getString(R.string.start)) {
                        startOrStopTrackerService(Constants.START_ACTION_SERVICE)
                        getString(R.string.stop)
                    } else {
                        startOrStopTrackerService(Constants.STOP_ACTION_SERVICE)
                        getString(R.string.start)
                    }
                }
            }
        }

    }

}