package com.awalsh128.thetowerbot

import com.malinskiy.adam.AndroidDebugBridgeClientFactory
import com.malinskiy.adam.interactor.StartAdbInteractor
import kotlinx.coroutines.*

fun main(): Unit {
  runBlocking {
    StartAdbInteractor().execute()
    val adb = AdbDevice(deviceSerial = "2B281FDH2002ZE", AndroidDebugBridgeClientFactory().build())

    println("screen size = ${adb.getScreenSize()}")
    for (eventGroup in adb.getEventGroups(this, "/dev/input/event4")) {
      println(eventGroup.toString())
    }
  }
}
