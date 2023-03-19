package com.thangadurai.distancetracker.utils

import android.os.Build

fun isGreaterThenOrEqualSnowCone (): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

fun isGreaterThenOrEqualNougat (): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
}

fun isGreaterThenOrEqualOreo (): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}