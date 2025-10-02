package com.example.flutter_boca_systems

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.content.Context
import bocasystems.com.sdk.BocaSystemsSDK

/** FlutterBocaSystemsPlugin */
class FlutterBocaSystemsPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private var bocaSDK: BocaSystemSDKWrapper? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_boca_systems")
        channel.setMethodCallHandler(this)
        bocaSDK = BocaSystemSDKWrapper(flutterPluginBinding.applicationContext as Context)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "openSessionBT" -> {
                val device = call.argument<String>("device")
                val success = bocaSDK?.openSessionBT(device, channel) ?: false
                result.success(success)
            }
            "openSessionUSB" -> {
                val success = bocaSDK?.openSessionUSB(channel) ?: false
                result.success(success)
            }
            "openSessionWIFI" -> {
                val ipAddress = call.argument<String>("ipAddress")
                val success = bocaSDK?.openSessionWIFI(ipAddress, channel) ?: false
                result.success(success)
            }
            "closeSessionBT" -> {
                bocaSDK?.closeSessionBT()
                result.success(true)
            }
            "closeSessionUSB" -> {
                bocaSDK?.closeSessionUSB()
                result.success(true)
            }
            "closeSessionWIFI" -> {
                bocaSDK?.closeSessionWIFI()
                result.success(true)
            }
            "verifyConnectionBT" -> {
                val connected = bocaSDK?.verifyConnectionBT() ?: false
                result.success(connected)
            }
            "verifyConnectionUSB" -> {
                val connected = bocaSDK?.verifyConnectionUSB() ?: false
                result.success(connected)
            }
            "verifyConnectionWIFI" -> {
                val connected = bocaSDK?.verifyConnectionWIFI() ?: false
                result.success(connected)
            }
            "sendString" -> {
                val string = call.argument<String>("string")
                bocaSDK?.sendString(string)
                result.success(true)
            }
            "sendFile" -> {
                val filename = call.argument<String>("filename")
                val row = call.argument<Int>("row") ?: 0
                val column = call.argument<Int>("column") ?: 0
                val success = bocaSDK?.sendFile(filename, row, column) ?: false
                result.success(success)
            }
            "downloadLogo" -> {
                val filename = call.argument<String>("filename")
                val idnum = call.argument<Int>("idnum") ?: 0
                val success = bocaSDK?.downloadLogo(filename, idnum) ?: false
                result.success(success)
            }
            "printLogo" -> {
                val idnum = call.argument<Int>("idnum") ?: 0
                val row = call.argument<Int>("row") ?: 0
                val column = call.argument<Int>("column") ?: 0
                val success = bocaSDK?.printLogo(idnum, row, column) ?: false
                result.success(success)
            }
            "changeConfiguration" -> {
                val path = call.argument<String>("path") ?: "<P1>"
                val resolution = call.argument<Int>("resolution") ?: 300
                val scaled = call.argument<Boolean>("scaled") ?: false
                val dithered = call.argument<Boolean>("dithered") ?: true
                val stocksizeindex = call.argument<Int>("stocksizeindex") ?: 0
                val orientation = call.argument<String>("orientation") ?: "<LM>"
                bocaSDK?.changeConfiguration(path, resolution, scaled, dithered, stocksizeindex, orientation)
                result.success(true)
            }
            "clearMemory" -> {
                bocaSDK?.clearMemory()
                result.success(true)
            }
            "printCut" -> {
                bocaSDK?.printCut()
                result.success(true)
            }
            "printNoCut" -> {
                bocaSDK?.printNoCut()
                result.success(true)
            }
            "getStatus" -> {
                val status = bocaSDK?.getStatus() ?: ""
                result.success(status)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        bocaSDK?.closeAllSessions()
        bocaSDK = null
    }
}

// Wrapper class for the Boca Systems SDK
class BocaSystemSDKWrapper(context: Context) : BocaSystemsSDK() {
    private var mContext: Context = context
    private var statusCallbackChannel: MethodChannel? = null
    private var statusBuffer: String = ""

    fun openSessionBT(device: String?, channel: MethodChannel): Boolean {
        this.statusCallbackChannel = channel
        return OpenSessionBT(device ?: "", mContext)
    }

    fun openSessionUSB(channel: MethodChannel): Boolean {
        this.statusCallbackChannel = channel
        return OpenSessionUSB(mContext)
    }

    fun openSessionWIFI(ipAddress: String?, channel: MethodChannel): Boolean {
        this.statusCallbackChannel = channel
        return OpenSessionWIFI(ipAddress ?: "", mContext)
    }

    fun closeSessionBT() {
        CloseSessionBT()
    }

    fun closeSessionUSB() {
        CloseSessionUSB()
    }

    fun closeSessionWIFI() {
        CloseSessionWIFI()
    }

    fun verifyConnectionBT(): Boolean {
        return VerifyConnectionBT()
    }

    fun verifyConnectionUSB(): Boolean {
        return VerifyConnectionUSB()
    }

    fun verifyConnectionWIFI(): Boolean {
        return VerifyConnectionWIFI()
    }

    fun sendString(string: String?) {
        if (!string.isNullOrBlank()) {
            SendString(string)
        }
    }

    fun sendFile(filename: String?, row: Int, column: Int): Boolean {
        return if (!filename.isNullOrBlank()) {
            SendFile(filename, row, column)
        } else {
            false
        }
    }

    fun downloadLogo(filename: String?, idnum: Int): Boolean {
        return if (!filename.isNullOrBlank()) {
            DownloadLogo(filename, idnum)
        } else {
            false
        }
    }

    fun printLogo(idnum: Int, row: Int, column: Int): Boolean {
        return PrintLogo(idnum, row, column)
    }

    fun changeConfiguration(
        path: String,
        resolution: Int,
        scaled: Boolean,
        dithered: Boolean,
        stocksizeindex: Int,
        orientation: String
    ) {
        ChangeConfiguration(path, resolution, scaled, dithered, stocksizeindex, orientation)
    }

    fun clearMemory() {
        ClearMemory()
    }

    fun printCut() {
        PrintCut()
    }

    fun printNoCut() {
        PrintNoCut()
    }

    fun getStatus(): String {
        return getStatusFromSDK()
    }
    
    private fun getStatusFromSDK(): String {
        // Since StatusReturned is static but package-private, we need to handle it differently
        // Instead, we'll use our own status buffer
        return statusBuffer
    }

    fun closeAllSessions() {
        if (VerifyConnectionBT()) {
            CloseSessionBT()
        }
        if (VerifyConnectionUSB()) {
            CloseSessionUSB()
        }
        if (VerifyConnectionWIFI()) {
            CloseSessionWIFI()
        }
    }

    override fun StatusReportCallback(statusReport: String?) {
        if (!statusReport.isNullOrBlank()) {
            statusBuffer += statusReport + "\n"
            // Send status to Flutter via method channel
            statusCallbackChannel?.invokeMethod("onStatusUpdate", mapOf("status" to statusReport))
        }
    }

    override fun getMemorySizeInBytes(): Long {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            val activityManager = mContext.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            memoryInfo.totalMem
        } else {
            0
        }
    }
}