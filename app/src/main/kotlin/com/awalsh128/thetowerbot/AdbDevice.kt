package com.awalsh128.thetowerbot

import com.malinskiy.adam.AndroidDebugBridgeClient
import com.malinskiy.adam.request.shell.v1.ChanneledShellCommandRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandRequest
import com.malinskiy.adam.request.shell.v2.ShellCommandResult
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

private val logger = io.github.oshai.kotlinlogging.KotlinLogging.logger {}

class AdbDevice(val deviceSerial: String, private val client: AndroidDebugBridgeClient) {

  /**
   *
   * https://www.kernel.org/doc/Documentation/input/event-codes.txt
   * https://github.com/torvalds/linux/blob/master/include/uapi/linux/input-event-codes.h
   */
  data class Event(val type: String, val code: String, val value: String) {

    val isSeparator: Boolean
      get() = type == "EV_SYN"

    fun valueAsLong(): Long = value.toLong(radix = 16)

    override fun toString() = "$type $code $value"

    companion object {

      /**
       * Parse device events from ADB shell getevent output.
       *
       * Examples
       *
       * Supported raw output using getevent -l:
       *
       * ```
       * EV_KEY       BTN_TOUCH            DOWN
       * EV_ABS       ABS_MT_TRACKING_ID   0000b10f
       * EV_ABS       ABS_MT_POSITION_X    00000264
       * EV_ABS       ABS_MT_POSITION_Y    000006e5
       * EV_ABS       ABS_MT_TOUCH_MAJOR   00000079
       * EV_ABS       ABS_MT_TOUCH_MINOR   00000060
       * EV_ABS       ABS_MT_PRESSURE      0000002e
       * EV_ABS       ABS_MT_ORIENTATION   fffff2b8
       * EV_SYN       SYN_REPORT           00000000
       * EV_ABS       ABS_MT_ORIENTATION   fffff2b7
       * EV_SYN       SYN_REPORT           00000000
       * EV_ABS       ABS_MT_TOUCH_MINOR   00000061
       * EV_ABS       ABS_MT_PRESSURE      0000002f
       * EV_ABS       ABS_MT_ORIENTATION   fffff2b9
       * EV_SYN       SYN_REPORT           00000000
       * EV_ABS       ABS_MT_TOUCH_MAJOR   0000007a
       * EV_ABS       ABS_MT_ORIENTATION   fffff2c7
       * EV_SYN       SYN_REPORT           00000000
       * EV_ABS       ABS_MT_TOUCH_MAJOR   0000007c
       * EV_ABS       ABS_MT_TOUCH_MINOR   00000062
       * EV_ABS       ABS_MT_PRESSURE      00000030
       * EV_ABS       ABS_MT_ORIENTATION   fffff2e9
       * EV_SYN       SYN_REPORT           00000000
       * ```
       */
      fun parse(line: String) =
          line.split(" ").filter { !it.isEmpty() }.let { Event(it[0], it[1], it[2]) }
    }
  }

  class EventGroup(val events: List<Event>) {
    override fun toString() = events.map { it.toString() }.joinToString("\n")
  }

  class ShellException(message: String, val exitCode: Int, val stderr: String) :
      Exception(message) {
    companion object {
      fun fromResult(command: String, result: ShellCommandResult) =
          ShellException(
              "Error encountered executing shell command '$command'.",
              result.exitCode,
              result.errorOutput
          )
    }
  }

  private suspend fun executeShell(command: String): String {
    val result: ShellCommandResult = client.execute(ShellCommandRequest(command), deviceSerial)
    if (result.exitCode != 0) {
      throw ShellException.fromResult(command, result)
    }
    return result.output
  }

  /**
   * Gets the device screen size as (width, height) tuple.
   *
   * e.g. shell output 'Physical size: 1080x2400' becomes (1080, 2400)
   */
  suspend fun getScreenSize(): Point =
      executeShell("wm size").split(":")[1].split("x").let { xy: List<String> ->
        Point(xy[0].trim().toInt(), xy[1].trim().toInt())
      }

  fun getEvents(scope: CoroutineScope, deviceFile: String): ReceiveChannel<Event> =
      @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
      scope.produce {
        for (lines in
            client.execute(
                ChanneledShellCommandRequest("getevent -l $deviceFile"),
                scope,
                deviceSerial
            )) {
          for (line in lines.split("\n").filter { !it.isEmpty() }) {
            logger.debug { "shell getevent -l $deviceFile: '$line'" }
            try {
              send(Event.parse(line))
            } catch (e: Exception) {
              logger.error(e) { "Error encountered parsing event output (line = $line)." }
            }
          }
        }
      }

  fun getEventGroups(scope: CoroutineScope, deviceFile: String): ReceiveChannel<EventGroup> =
      @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
      scope.produce {
        val events = mutableListOf<Event>()
        for (event in getEvents(scope, deviceFile)) {
          events.add(event)
          if (event.isSeparator) {
            send(EventGroup(events))
            events.clear()
          }
        }
      }

  private fun toArgs(p: Point) = "${p.x} ${p.y}"

  suspend fun swipe(from: Point, to: Point): Unit {
    executeShell("touchscreen swipe ${toArgs(from)} ${toArgs(to)}")
  }
}
