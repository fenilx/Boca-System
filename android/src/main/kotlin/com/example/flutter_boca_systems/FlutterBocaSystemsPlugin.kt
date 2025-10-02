package com.example.flutter_boca_systems

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.content.Context
import bocasystems.com.sdk.BocaSystemsSDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val success = bocaSDK?.openSessionBT(device, channel) ?: false
                        withContext(Dispatchers.Main) {
                            result.success(success)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("BT_CONNECTION_ERROR", e.message, null)
                        }
                    }
                }
            }
            "openSessionUSB" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val success = bocaSDK?.openSessionUSB(channel) ?: false
                        withContext(Dispatchers.Main) {
                            result.success(success)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("USB_CONNECTION_ERROR", e.message, null)
                        }
                    }
                }
            }
            "openSessionWIFI" -> {
                val ipAddress = call.argument<String>("ipAddress")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val success = bocaSDK?.openSessionWIFI(ipAddress, channel) ?: false
                        withContext(Dispatchers.Main) {
                            result.success(success)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("WIFI_CONNECTION_ERROR", e.message, null)
                        }
                    }
                }
            }
            "closeSessionBT" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        bocaSDK?.closeSessionBT()
                        withContext(Dispatchers.Main) {
                            result.success(true)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("BT_CLOSE_ERROR", e.message, null)
                        }
                    }
                }
            }
            "closeSessionUSB" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        bocaSDK?.closeSessionUSB()
                        withContext(Dispatchers.Main) {
                            result.success(true)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("USB_CLOSE_ERROR", e.message, null)
                        }
                    }
                }
            }
            "closeSessionWIFI" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        bocaSDK?.closeSessionWIFI()
                        withContext(Dispatchers.Main) {
                            result.success(true)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("WIFI_CLOSE_ERROR", e.message, null)
                        }
                    }
                }
            }
            "verifyConnectionBT" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val connected = bocaSDK?.verifyConnectionBT() ?: false
                        withContext(Dispatchers.Main) {
                            result.success(connected)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("BT_VERIFY_ERROR", e.message, null)
                        }
                    }
                }
            }
            "verifyConnectionUSB" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val connected = bocaSDK?.verifyConnectionUSB() ?: false
                        withContext(Dispatchers.Main) {
                            result.success(connected)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("USB_VERIFY_ERROR", e.message, null)
                        }
                    }
                }
            }
            "verifyConnectionWIFI" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val connected = bocaSDK?.verifyConnectionWIFI() ?: false
                        withContext(Dispatchers.Main) {
                            result.success(connected)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("WIFI_VERIFY_ERROR", e.message, null)
                        }
                    }
                }
            }
            "sendString" -> {
                val string = call.argument<String>("string")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        bocaSDK?.sendString(string)
                        withContext(Dispatchers.Main) {
                            result.success(true)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("SEND_STRING_ERROR", e.message, null)
                        }
                    }
                }
            }
            "sendFile" -> {
                val filename = call.argument<String>("filename")
                val row = call.argument<Int>("row") ?: 0
                val column = call.argument<Int>("column") ?: 0
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val success = bocaSDK?.sendFile(filename, row, column) ?: false
                        withContext(Dispatchers.Main) {
                            result.success(success)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("SEND_FILE_ERROR", e.message, null)
                        }
                    }
                }
            }
            "downloadLogo" -> {
                val filename = call.argument<String>("filename")
                val idnum = call.argument<Int>("idnum") ?: 0
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val success = bocaSDK?.downloadLogo(filename, idnum) ?: false
                        withContext(Dispatchers.Main) {
                            result.success(success)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("DOWNLOAD_LOGO_ERROR", e.message, null)
                        }
                    }
                }
            }
            "printLogo" -> {
                val idnum = call.argument<Int>("idnum") ?: 0
                val row = call.argument<Int>("row") ?: 0
                val column = call.argument<Int>("column") ?: 0
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val success = bocaSDK?.printLogo(idnum, row, column) ?: false
                        withContext(Dispatchers.Main) {
                            result.success(success)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("PRINT_LOGO_ERROR", e.message, null)
                        }
                    }
                }
            }
            "changeConfiguration" -> {
                val path = call.argument<String>("path") ?: "<P1>"
                val resolution = call.argument<Int>("resolution") ?: 300
                val scaled = call.argument<Boolean>("scaled") ?: false
                val dithered = call.argument<Boolean>("dithered") ?: true
                val stocksizeindex = call.argument<Int>("stocksizeindex") ?: 0
                val orientation = call.argument<String>("orientation") ?: "<LM>"
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        bocaSDK?.changeConfiguration(path, resolution, scaled, dithered, stocksizeindex, orientation)
                        withContext(Dispatchers.Main) {
                            result.success(true)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("CONFIGURATION_ERROR", e.message, null)
                        }
                    }
                }
            }
            "clearMemory" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        bocaSDK?.clearMemory()
                        withContext(Dispatchers.Main) {
                            result.success(true)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("CLEAR_MEMORY_ERROR", e.message, null)
                        }
                    }
                }
            }
            "printCut" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        bocaSDK?.printCut()
                        withContext(Dispatchers.Main) {
                            result.success(true)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("PRINT_CUT_ERROR", e.message, null)
                        }
                    }
                }
            }
            "printNoCut" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        bocaSDK?.printNoCut()
                        withContext(Dispatchers.Main) {
                            result.success(true)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("PRINT_NO_CUT_ERROR", e.message, null)
                        }
                    }
                }
            }
            "getStatus" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val status = bocaSDK?.getStatus() ?: ""
                        withContext(Dispatchers.Main) {
                            result.success(status)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            result.error("GET_STATUS_ERROR", e.message, null)
                        }
                    }
                }
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

    private fun getStatusFromSDK(): String {
        // Since StatusReturned is static but package-private, we need to handle it differently
        // Instead, we'll use our own status buffer
        return statusBuffer
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