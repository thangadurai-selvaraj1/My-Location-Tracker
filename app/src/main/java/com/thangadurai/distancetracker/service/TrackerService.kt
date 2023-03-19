package com.thangadurai.distancetracker.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.thangadurai.distancetracker.utils.Constants
import com.thangadurai.distancetracker.utils.Constants.NOTIFICATION_CHANNEL_ID
import com.thangadurai.distancetracker.utils.Constants.NOTIFICATION_CHANNEL_NAME
import com.thangadurai.distancetracker.utils.Constants.NOTIFICATION_ID
import com.thangadurai.distancetracker.utils.isGreaterThenOrEqualNougat
import com.thangadurai.distancetracker.utils.isGreaterThenOrEqualOreo
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@SuppressLint("MissingPermission")
@AndroidEntryPoint
class TrackerService : LifecycleService() {

    @Inject
    lateinit var notification: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManager

    @Inject
    lateinit var locationRequest: LocationRequest

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        val serviceIsStarted = MutableLiveData<Boolean>()
        val locationList = MutableLiveData<MutableList<LatLng>>()
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            super.onLocationResult(result)
            result.locations.forEach {
                updateLocationList(it)
                updateNotificationPeriodically()
            }
        }
    }

    override fun onCreate() {
        serviceIsStarted.postValue(false)
        locationList.postValue(mutableListOf())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                Constants.START_ACTION_SERVICE -> {
                    serviceIsStarted.postValue(true)
                    startForegroundService()
                    startLocationUpdates()
                }
                Constants.STOP_ACTION_SERVICE -> {
                    serviceIsStarted.postValue(false)
                    stopForegroundService()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback, Looper.getMainLooper()
        )
    }

    private fun updateLocationList(location: Location) {
       locationList.value?.apply {
           add(LatLng(location.latitude,location.longitude))
           locationList.postValue(this)
       }
    }

    private fun startForegroundService() {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification.build())
    }

    private fun stopForegroundService() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            NOTIFICATION_ID
        )
        fusedLocationClient.removeLocationUpdates(locationCallback)

        if (isGreaterThenOrEqualNougat()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (isGreaterThenOrEqualOreo()) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotificationPeriodically() {
        notification.apply {
            setContentTitle("Distance Travelled")
            setContentText( "${locationList.value?.last()} km")
        }
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }

}