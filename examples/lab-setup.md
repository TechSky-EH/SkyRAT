# Security Testing Lab Setup Guide

Complete guide for setting up a professional Android security testing laboratory using SkyRAT.

## 🎯 Lab Overview

A proper security testing lab provides:
- **Isolated environment** for safe testing
- **Multiple test devices** with different Android versions
- **Network isolation** to prevent data leakage
- **Documentation system** for findings and procedures
- **Legal compliance** framework for authorized testing

## 🏗️ Physical Lab Setup

### **Essential Hardware**

#### **Server Infrastructure**
```
Primary Server:
- CPU: Intel i7/AMD Ryzen 7 (8+ cores)
- RAM: 32GB minimum, 64GB recommended
- Storage: 1TB SSD + 2TB HDD for data
- Network: Gigabit Ethernet + WiFi
- OS: Ubuntu 22.04 LTS / CentOS 9

Backup Server (optional):
- Lower specs for redundancy
- Network storage for backup
```

#### **Test Devices**
```
Android Device Matrix:
┌─────────────────┬─────────────┬──────────────┬─────────────┐
│ Device Type     │ Android Ver │ API Level    │ Purpose     │
├─────────────────┼─────────────┼──────────────┼─────────────┤
│ Pixel 4a        │ 11          │ 30           │ Stock tests │
│ Galaxy S21      │ 12          │ 31           │ Samsung UX  │
│ OnePlus 9       │ 13          │ 33           │ OxygenOS    │
│ Emulator x86    │ 14          │ 34           │ Latest API  │
│ Rooted device   │ Various     │ -            │ Root tests  │
└─────────────────┴─────────────┴──────────────┴─────────────┘
```

#### **Network Equipment**
```
Network Infrastructure:
- Managed switch (24+ ports)
- Isolated WiFi router for test devices
- Firewall/router for internet gateway
- Network cables and WiFi access points
- Network monitoring tools
```

### **Lab Network Architecture**

#### **Network Segmentation**
```
Internet
    │
┌───▼───┐    ┌─────────────────────────────────────┐
│Gateway│    │          ISOLATED LAB NETWORK       │
│Router │    │                                     │
└───┬───┘    │  ┌─────────────┐  ┌─────────────┐   │
    │        │  │   Server    │  │   Devices   │   │
    │        │  │ 192.168.2.1 │  │192.168.2.x  │   │
    │        │  └─────────────┘  └─────────────┘   │
    │        │                                     │
    │        └─────────────────────────────────────┘
    │
┌───▼───┐
│Main   │
│Network│
└───────┘
```

#### **IP Address Scheme**
```bash
# Management Network
192.168.1.0/24  - Main office network

# Lab Network (Isolated)
192.168.2.0/24  - Lab infrastructure
192.168.2.1     - Lab server
192.168.2.10-50 - Static devices
192.168.2.100+  - DHCP range for test devices

# DMZ Network (optional)
192.168.3.0/24  - External access testing
```

## 💻 Software Infrastructure

### **Server Setup**

#### **Ubuntu Server Configuration**
```bash
# System update
sudo apt update && sudo apt upgrade -y

# Install essential packages
sudo apt install -y python3 python3-pip python3-venv git curl wget unzip
sudo apt install -y openjdk-11-jdk android-tools-adb android-tools-fastboot
sudo apt install -y wireshark tcpdump nmap netcat-openbsd

# Install Docker for containerized testing
sudo apt install -y docker.io docker-compose
sudo usermod -aG docker $USER

# Install network monitoring tools
sudo apt install -y iftop nethogs iotop htop
```

#### **SkyRAT Installation**
```bash
# Clone SkyRAT repository
git clone https://github.com/techsky-eh/skyrat.git /opt/skyrat
cd /opt/skyrat

# Create dedicated user
sudo useradd -r -s /bin/bash -d /opt/skyrat skyrat
sudo chown -R skyrat:skyrat /opt/skyrat

# Setup Python environment
sudo -u skyrat python3 -m venv /opt/skyrat/venv
sudo -u skyrat /opt/skyrat/venv/bin/pip install -r requirements.txt

# Setup Android SDK
sudo -u skyrat ./tools/android-sdk-setup.sh
```

#### **Service Configuration**
```bash
# Create systemd service for SkyRAT
sudo tee /etc/systemd/system/skyrat.service > /dev/null << 'EOF'
[Unit]
Description=SkyRAT Security Testing Framework
After=network.target

[Service]
Type=simple
User=skyrat
WorkingDirectory=/opt/skyrat
Environment=PATH=/opt/skyrat/venv/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
ExecStart=/opt/skyrat/venv/bin/python skyrat.py --shell -i 0.0.0.0 -p 8000
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# Enable and start service
sudo systemctl enable skyrat
sudo systemctl start skyrat
```

### **Database Setup for Logging**

#### **PostgreSQL Database**
```bash
# Install PostgreSQL
sudo apt install -y postgresql postgresql-contrib

# Create database and user
sudo -u postgres createdb skyrat_logs
sudo -u postgres psql -c "CREATE USER skyrat_user WITH PASSWORD 'secure_password';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE skyrat_logs TO skyrat_user;"

# Create logging tables
sudo -u postgres psql -d skyrat_logs << 'EOF'
CREATE TABLE test_sessions (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(50) UNIQUE,
    device_id VARCHAR(100),
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    operator VARCHAR(50),
    authorization_ref VARCHAR(100),
    status VARCHAR(20)
);

CREATE TABLE commands_log (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(50),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    command TEXT,
    response_size INTEGER,
    execution_time INTEGER,
    status VARCHAR(20)
);

CREATE TABLE extracted_data (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(50),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    data_type VARCHAR(50),
    file_path TEXT,
    file_size INTEGER,
    checksum VARCHAR(64)
);
EOF
```

## 🔒 Security Configuration

### **Network Security**

#### **Firewall Configuration**
```bash
# Install and configure UFW
sudo ufw --force reset
sudo ufw default deny incoming
sudo ufw default allow outgoing

# Allow SSH (change port as needed)
sudo ufw allow 22/tcp

# Allow SkyRAT server
sudo ufw allow 8000:8010/tcp

# Allow ADB debugging
sudo ufw allow 5555/tcp

# Allow lab network
sudo ufw allow from 192.168.2.0/24

# Enable firewall
sudo ufw --force enable
```

#### **Network Monitoring**
```bash
# Install network monitoring
sudo apt install -y ntopng suricata

# Configure ntopng for traffic analysis
sudo tee /etc/ntopng/ntopng.conf > /dev/null << 'EOF'
-i=eth0
-d=/var/lib/ntopng
-w=3000
-P=/etc/ntopng/ntopng.pid
-U=ntopng
-p=/etc/ntopng/ntopng.conf
EOF

# Start monitoring services
sudo systemctl enable ntopng
sudo systemctl start ntopng
```

### **Data Protection**

#### **Encryption Setup**
```bash
# Install encryption tools
sudo apt install -y encfs ecryptfs-utils

# Create encrypted storage for sensitive data
mkdir /home/skyrat/secure-storage
encfs /home/skyrat/.encrypted /home/skyrat/secure-storage

# Backup encryption key
sudo cp /home/skyrat/.encfs6.xml /root/skyrat-encryption-backup.xml
```

#### **Access Control**
```bash
# Setup proper file permissions
chmod 700 /opt/skyrat
chmod 750 /opt/skyrat/dumps
chmod 640 /opt/skyrat/logs/*

# Setup sudo access for specific operations
sudo tee /etc/sudoers.d/skyrat > /dev/null << 'EOF'
skyrat ALL=(root) NOPASSWD: /usr/bin/adb
skyrat ALL=(root) NOPASSWD: /usr/bin/fastboot
skyrat ALL=(root) NOPASSWD: /bin/systemctl restart skyrat
EOF
```

## 📱 Device Management

### **Android Device Preparation**

#### **Standard Device Setup**
```bash
#!/bin/bash
# prepare-test-device.sh

DEVICE_SERIAL="$1"
DEVICE_NAME="$2"

if [ -z "$DEVICE_SERIAL" ]; then
    echo "Usage: $0 <device_serial> <device_name>"
    exit 1
fi

echo "Preparing device: $DEVICE_NAME ($DEVICE_SERIAL)"

# Enable developer options and USB debugging
adb -s "$DEVICE_SERIAL" shell settings put global development_settings_enabled 1
adb -s "$DEVICE_SERIAL" shell settings put global adb_enabled 1

# Disable automatic updates
adb -s "$DEVICE_SERIAL" shell settings put global auto_update_enabled 0

# Set screen timeout to maximum
adb -s "$DEVICE_SERIAL" shell settings put system screen_off_timeout 1800000

# Disable lock screen
adb -s "$DEVICE_SERIAL" shell settings put secure lockscreen.disabled 1

# Install test certificates
adb -s "$DEVICE_SERIAL" push lab-ca-cert.crt /sdcard/
adb -s "$DEVICE_SERIAL" shell am start -a android.credentials.INSTALL

echo "Device preparation complete"
```

#### **Device Inventory Management**
```bash
# Create device inventory
tee /opt/skyrat/device-inventory.txt > /dev/null << 'EOF'
# Device Inventory
# Format: SERIAL|MODEL|ANDROID_VERSION|STATUS|NOTES

HT7B1234567|Pixel4a|11|ACTIVE|Primary test device
R58N5678901|GalaxyS21|12|ACTIVE|Samsung testing
GM1917234567|OnePlus9|13|ACTIVE|OxygenOS testing
emulator-5554|Emulator|14|ACTIVE|Latest Android
EOF

# Device status checker
#!/bin/bash
# check-devices.sh

echo "=== LAB DEVICE STATUS ==="
while IFS='|' read -r serial model android status notes; do
    if [[ $serial == \#* ]]; then continue; fi
    
    if adb devices | grep -q "$serial"; then
        online_status="ONLINE"
    else
        online_status="OFFLINE"
    fi
    
    printf "%-15s %-12s %-8s %-8s %s\n" "$serial" "$model" "$android" "$online_status" "$notes"
done < /opt/skyrat/device-inventory.txt
```

### **Emulator Management**

#### **Android Emulator Setup**
```bash
# Install Android emulator
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "emulator" "system-images;android-34;google_apis;x86_64"

# Create test emulators
$ANDROID_HOME/emulator/emulator -create-avd -n Android11-Test -k "system-images;android-30;google_apis;x86_64"
$ANDROID_HOME/emulator/emulator -create-avd -n Android13-Test -k "system-images;android-33;google_apis;x86_64"
$ANDROID_HOME/emulator/emulator -create-avd -n Android14-Test -k "system-images;android-34;google_apis;x86_64"

# Emulator management script
#!/bin/bash
# manage-emulators.sh

case "$1" in
    start)
        echo "Starting emulator: $2"
        $ANDROID_HOME/emulator/emulator -avd "$2" -no-audio -no-window &
        ;;
    stop)
        echo "Stopping all emulators"
        pkill -f emulator
        ;;
    list)
        echo "Available emulators:"
        $ANDROID_HOME/emulator/emulator -list-avds
        ;;
    *)
        echo "Usage: $0 {start|stop|list} [emulator_name]"
        ;;
esac
```

## 📊 Monitoring and Logging

### **Comprehensive Logging System**

#### **Central Logging Configuration**
```bash
# Install ELK stack for log management
sudo apt install -y elasticsearch logstash kibana

# Configure Logstash for SkyRAT logs
sudo tee /etc/logstash/conf.d/skyrat.conf > /dev/null << 'EOF'
input {
  file {
    path => "/opt/skyrat/logs/*.log"
    start_position => "beginning"
    tags => ["skyrat"]
  }
}

filter {
  if "skyrat" in [tags] {
    grok {
      match => { "message" => "%{TIMESTAMP_ISO8601:timestamp} %{LOGLEVEL:level} %{GREEDYDATA:message}" }
    }
  }
}

output {
  elasticsearch {
    hosts => ["localhost:9200"]
    index => "skyrat-logs-%{+YYYY.MM.dd}"
  }
}
EOF
```

#### **Real-time Monitoring Dashboard**
```python
#!/usr/bin/env python3
# monitoring-dashboard.py

import psutil
import time
import json
from datetime import datetime
import subprocess

class LabMonitor:
    def __init__(self):
        self.start_time = datetime.now()
        
    def get_system_stats(self):
        return {
            'cpu_percent': psutil.cpu_percent(interval=1),
            'memory_percent': psutil.virtual_memory().percent,
            'disk_usage': psutil.disk_usage('/').percent,
            'network_io': psutil.net_io_counters()._asdict(),
            'timestamp': datetime.now().isoformat()
        }
    
    def get_skyrat_status(self):
        try:
            result = subprocess.run(['systemctl', 'is-active', 'skyrat'], 
                                  capture_output=True, text=True)
            return result.stdout.strip()
        except:
            return 'unknown'
    
    def get_connected_devices(self):
        try:
            result = subprocess.run(['adb', 'devices'], 
                                  capture_output=True, text=True)
            lines = result.stdout.strip().split('\n')[1:]
            devices = []
            for line in lines:
                if '\t' in line:
                    serial, status = line.split('\t')
                    devices.append({'serial': serial, 'status': status})
            return devices
        except:
            return []
    
    def generate_report(self):
        report = {
            'timestamp': datetime.now().isoformat(),
            'uptime': str(datetime.now() - self.start_time),
            'system': self.get_system_stats(),
            'skyrat_status': self.get_skyrat_status(),
            'connected_devices': self.get_connected_devices()
        }
        return report

# Usage
if __name__ == "__main__":
    monitor = LabMonitor()
    while True:
        report = monitor.generate_report()
        print(json.dumps(report, indent=2))
        time.sleep(60)
```

## 🔧 Automation Scripts

### **Lab Management Automation**

#### **Daily Lab Operations**
```bash
#!/bin/bash
# daily-lab-operations.sh

LOG_DIR="/opt/skyrat/logs"
BACKUP_DIR="/opt/skyrat/backups"
DATE=$(date +%Y%m%d)

# Create daily log directory
mkdir -p "$LOG_DIR/$DATE"

# System health check
echo "=== DAILY LAB HEALTH CHECK - $DATE ===" | tee "$LOG_DIR/$DATE/health-check.log"

# Check system resources
echo "System Resources:" | tee -a "$LOG_DIR/$DATE/health-check.log"
df -h | tee -a "$LOG_DIR/$DATE/health-check.log"
free -h | tee -a "$LOG_DIR/$DATE/health-check.log"

# Check SkyRAT service
echo "SkyRAT Service Status:" | tee -a "$LOG_DIR/$DATE/health-check.log"
systemctl status skyrat | tee -a "$LOG_DIR/$DATE/health-check.log"

# Check connected devices
echo "Connected Devices:" | tee -a "$LOG_DIR/$DATE/health-check.log"
adb devices | tee -a "$LOG_DIR/$DATE/health-check.log"

# Backup important data
echo "Creating backups..." | tee -a "$LOG_DIR/$DATE/health-check.log"
rsync -av /opt/skyrat/dumps/ "$BACKUP_DIR/$DATE/dumps/"
rsync -av /opt/skyrat/logs/ "$BACKUP_DIR/$DATE/logs/"

# Clean old logs (keep 30 days)
find "$LOG_DIR" -type d -name "202*" -mtime +30 -exec rm -rf {} \;
find "$BACKUP_DIR" -type d -name "202*" -mtime +30 -exec rm -rf {} \;

echo "Daily operations complete" | tee -a "$LOG_DIR/$DATE/health-check.log"
```

#### **Automated Testing Pipeline**
```bash
#!/bin/bash
# automated-testing.sh

DEVICE_LIST=("HT7B1234567" "R58N5678901" "GM1917234567")
TEST_SERVER="192.168.2.1"
TEST_PORT="8000"

for device in "${DEVICE_LIST[@]}"; do
    echo "=== Testing device: $device ==="
    
    # Check device connectivity
    if ! adb -s "$device" shell echo "test" > /dev/null 2>&1; then
        echo "Device $device not accessible, skipping"
        continue
    fi
    
    # Build and install APK
    python3 /opt/skyrat/skyrat.py --build -i "$TEST_SERVER" -p "$TEST_PORT" -o "test-$device.apk"
    
    if [ $? -eq 0 ]; then
        # Install APK
        adb -s "$device" install -r "/opt/skyrat/build/test-$device.apk"
        
        # Wait for connection and run basic tests
        sleep 30
        
        # Basic automated commands
        echo "deviceInfo" | nc "$TEST_SERVER" "$TEST_PORT" > "test-results-$device.txt"
        echo "getIP" | nc "$TEST_SERVER" "$TEST_PORT" >> "test-results-$device.txt"
        echo "sysinfo" | nc "$TEST_SERVER" "$TEST_PORT" >> "test-results-$device.txt"
        
        echo "Automated testing complete for $device"
    else
        echo "APK build failed for $device"
    fi
done
```

## 📋 Lab Procedures

### **Standard Operating Procedures**

#### **Pre-Test Checklist**
```markdown
## Pre-Test Checklist

### Authorization
- [ ] Written authorization obtained and documented
- [ ] Test scope clearly defined
- [ ] Legal compliance verified
- [ ] Incident response plan reviewed

### Technical Setup
- [ ] Lab network isolated and configured
- [ ] All test devices prepared and verified
- [ ] SkyRAT server operational
- [ ] Monitoring systems active
- [ ] Backup systems verified

### Documentation
- [ ] Test plan documented
- [ ] Expected outcomes defined
- [ ] Risk assessment completed
- [ ] Data handling procedures reviewed

### Safety
- [ ] Emergency procedures reviewed
- [ ] Data protection measures active
- [ ] Access controls verified
- [ ] Audit logging enabled
```

#### **Post-Test Procedures**
```markdown
## Post-Test Procedures

### Data Management
- [ ] All extracted data inventoried
- [ ] Sensitive data encrypted
- [ ] Backup copies created
- [ ] Original data securely stored

### Device Cleanup
- [ ] Test APKs removed from devices
- [ ] Device settings restored
- [ ] Temporary files cleaned
- [ ] Factory reset if required

### Documentation
- [ ] Test results documented
- [ ] Findings analyzed and categorized
- [ ] Recommendations prepared
- [ ] Final report completed

### Security
- [ ] All network connections closed
- [ ] Access logs reviewed
- [ ] Security incidents documented
- [ ] Lessons learned captured
```

## 🔍 Quality Assurance

### **Testing Validation**

#### **Lab Certification Process**
```bash
#!/bin/bash
# lab-certification.sh

echo "=== LAB CERTIFICATION PROCESS ==="

# Network isolation test
echo "Testing network isolation..."
if ping -c 1 8.8.8.8 > /dev/null 2>&1; then
    echo "❌ FAIL: Lab has internet access"
    exit 1
else
    echo "✅ PASS: Network properly isolated"
fi

# Device connectivity test
echo "Testing device connectivity..."
device_count=$(adb devices | grep -v "List of devices" | grep -c "device")
if [ "$device_count" -gt 0 ]; then
    echo "✅ PASS: $device_count devices connected"
else
    echo "❌ FAIL: No devices connected"
    exit 1
fi

# SkyRAT service test
echo "Testing SkyRAT service..."
if systemctl is-active --quiet skyrat; then
    echo "✅ PASS: SkyRAT service running"
else
    echo "❌ FAIL: SkyRAT service not running"
    exit 1
fi

# Security configuration test
echo "Testing security configuration..."
if ufw status | grep -q "Status: active"; then
    echo "✅ PASS: Firewall active"
else
    echo "❌ FAIL: Firewall not active"
    exit 1
fi

echo "=== LAB CERTIFICATION COMPLETE ==="
```

## 📚 Training and Documentation

### **Lab User Training**

#### **Training Modules**
1. **Legal and Ethical Requirements**
   - Authorization procedures
   - Data protection laws
   - Responsible disclosure
   - Incident reporting

2. **Technical Operations**
   - SkyRAT framework usage
   - Device management
   - Network configuration
   - Troubleshooting procedures

3. **Security Procedures**
   - Access control
   - Data handling
   - Incident response
   - Audit compliance

4. **Documentation Standards**
   - Test planning
   - Results recording
   - Report writing
   - Evidence handling

---

**This comprehensive lab setup provides a professional foundation for Android security testing with SkyRAT. Ensure all procedures are followed and regularly updated based on new requirements and lessons learned.**