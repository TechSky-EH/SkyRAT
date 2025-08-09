# SkyRAT Installation Guide

Complete installation guide for the SkyRAT Android Security Testing Framework.

## 📋 System Requirements

### **Operating System Support**
- **Linux**: Ubuntu 18.04+, Debian 10+, CentOS 7+, Arch Linux
- **macOS**: macOS 10.15+ (Catalina and newer)
- **Windows**: Windows 10/11 with WSL2 (recommended) or native

### **Hardware Requirements**
- **RAM**: Minimum 4GB, recommended 8GB+
- **Storage**: 5GB free space for SDK and tools
- **Network**: Internet connection for downloads and ngrok (optional)

### **Software Prerequisites**
- **Python**: 3.7 or higher
- **Java**: JDK 8 or higher (OpenJDK recommended)
- **Git**: For repository management
- **pip3**: Python package manager

## 🚀 Quick Installation

### **Method 1: Automated Setup (Recommended)**
```bash
# Clone repository
git clone https://github.com/techsky-eh/skyrat.git
cd skyrat

# Run automated setup
pip3 install -r requirements.txt
chmod +x tools/android-sdk-setup.sh
./tools/android-sdk-setup.sh

# Verify installation
python3 skyrat.py --help
```

### **Method 2: Manual Installation**
Follow the detailed steps below for manual installation.

## 🔧 Detailed Installation Steps

### **Step 1: Install System Dependencies**

#### **Ubuntu/Debian**
```bash
# Update package list
sudo apt update

# Install required packages
sudo apt install -y python3 python3-pip python3-venv git curl wget unzip

# Install Java (OpenJDK 11)
sudo apt install -y openjdk-11-jdk

# Verify Java installation
java -version
javac -version
```

#### **CentOS/RHEL/Fedora**
```bash
# Install required packages
sudo dnf install -y python3 python3-pip git curl wget unzip

# Install Java
sudo dnf install -y java-11-openjdk-devel

# Verify installation
java -version
javac -version
```

#### **macOS**
```bash
# Install Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install required packages
brew install python3 git curl wget

# Install Java
brew install openjdk@11

# Add Java to PATH (add to ~/.zshrc or ~/.bash_profile)
echo 'export PATH="/opt/homebrew/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

#### **Windows (WSL2 Recommended)**
```bash
# Install WSL2 and Ubuntu
wsl --install

# Inside WSL2, follow Ubuntu instructions above
# Or use Windows native installation below
```

#### **Windows (Native)**
1. **Install Python**: Download from https://python.org (3.7+)
2. **Install Java**: Download OpenJDK from https://adoptium.net
3. **Install Git**: Download from https://git-scm.com
4. **Add to PATH**: Ensure Python, Java, and Git are in system PATH

### **Step 2: Clone SkyRAT Repository**
```bash
# Clone the repository
git clone https://github.com/techsky-eh/skyrat.git
cd skyrat

# Verify repository structure
ls -la
```

### **Step 3: Setup Python Environment**

#### **Option A: Virtual Environment (Recommended)**
```bash
# Create virtual environment
python3 -m venv skyrat-env

# Activate virtual environment
source skyrat-env/bin/activate  # Linux/macOS
# or
skyrat-env\Scripts\activate     # Windows

# Install Python dependencies
pip3 install -r requirements.txt
```

#### **Option B: System-wide Installation**
```bash
# Install dependencies system-wide
pip3 install -r requirements.txt
```

### **Step 4: Android SDK Setup**

#### **Automated Setup (Recommended)**
```bash
# Run the automated Android SDK setup script
chmod +x tools/android-sdk-setup.sh
./tools/android-sdk-setup.sh
```

The script will:
- Download and install Android command line tools
- Setup Android SDK in `~/Android/Sdk` (or custom location)
- Install required SDK packages
- Configure environment variables
- Verify installation

#### **Manual Android SDK Setup**
If the automated script fails, follow these manual steps:

##### **1. Download Android Command Line Tools**
```bash
# Create Android SDK directory
mkdir -p ~/Android/Sdk
cd ~/Android/Sdk

# Download command line tools (Linux)
wget https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip
unzip commandlinetools-linux-9477386_latest.zip

# Create proper directory structure
mkdir -p cmdline-tools
mv cmdline-tools latest_temp
mv latest_temp cmdline-tools/latest

# Clean up
rm commandlinetools-linux-9477386_latest.zip
```

##### **2. Setup Environment Variables**
Add to your shell configuration file (`~/.bashrc`, `~/.zshrc`, etc.):
```bash
# Android SDK Environment Variables
export ANDROID_HOME="$HOME/Android/Sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/build-tools/34.0.0:$PATH"
```

##### **3. Install SDK Packages**
```bash
# Reload environment
source ~/.bashrc  # or ~/.zshrc

# Accept licenses
yes | sdkmanager --licenses

# Install required packages
sdkmanager "platform-tools" "platforms;android-33" "platforms;android-34" "build-tools;33.0.2" "build-tools;34.0.0"
```

### **Step 5: Verify Installation**

#### **Test Python Framework**
```bash
# Test SkyRAT main script
python3 skyrat.py --help

# Should show SkyRAT help information
```

#### **Test Android Build System**
```bash
# Navigate to Android project
cd android

# Test Gradle wrapper
./gradlew tasks

# Should show available Gradle tasks
```

#### **Test Full Build Process**
```bash
# Return to project root
cd ..

# Test APK building (replace with your IP)
python3 skyrat.py --build -i 127.0.0.1 -p 8000 -o test.apk

# Should create test.apk in build/ directory
```

## 🔧 Configuration

### **Environment Variables**
Ensure these environment variables are set:
```bash
# Check Android SDK
echo $ANDROID_HOME
# Should output: /home/username/Android/Sdk

# Check PATH includes Android tools
echo $PATH | grep android
# Should show Android SDK paths
```

### **Network Configuration**
For external access using ngrok:
```bash
# Install ngrok (optional)
# Download from https://ngrok.com/download

# Or install via package manager
# Ubuntu/Debian:
sudo snap install ngrok

# macOS:
brew install ngrok

# Setup ngrok authtoken (required for tunnels)
ngrok authtoken YOUR_AUTH_TOKEN
```

## 🛠️ Troubleshooting Installation

### **Common Issues**

#### **Java Not Found**
```bash
# Error: java: command not found
# Solution: Install Java and add to PATH

# Check Java installation
which java
java -version

# If not found, install Java (Ubuntu/Debian)
sudo apt install openjdk-11-jdk
```

#### **Python Dependencies Failed**
```bash
# Error: pip install failed
# Solution: Update pip and try again

pip3 install --upgrade pip
pip3 install -r requirements.txt
```

#### **Android SDK Issues**
```bash
# Error: ANDROID_HOME not set
# Solution: Set environment variable

export ANDROID_HOME="$HOME/Android/Sdk"
echo 'export ANDROID_HOME="$HOME/Android/Sdk"' >> ~/.bashrc
source ~/.bashrc
```

#### **Permission Denied (Android SDK)**
```bash
# Error: Permission denied when running sdkmanager
# Solution: Fix permissions

chmod +x $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager
```

#### **Gradle Build Failed**
```bash
# Error: Gradle build failed
# Solution: Clean and rebuild

cd android
./gradlew clean
./gradlew build
```

### **Platform-Specific Issues**

#### **macOS Issues**
```bash
# Error: Command Line Tools not found
# Solution: Install Xcode Command Line Tools
xcode-select --install

# Error: Java not in PATH
# Solution: Add Java to PATH
echo 'export PATH="/opt/homebrew/opt/openjdk@11/bin:$PATH"' >> ~/.zshrc
```

#### **Windows Issues**
```bash
# Error: 'python3' not recognized
# Solution: Use 'python' instead or add python3 alias
python skyrat.py --help

# Error: Permission denied (WSL2)
# Solution: Fix file permissions
chmod +x tools/android-sdk-setup.sh
```

#### **Linux Issues**
```bash
# Error: Missing 32-bit libraries (Ubuntu)
# Solution: Install additional libraries
sudo apt install libc6:i386 libncurses5:i386 libstdc++6:i386 lib32z1 libbz2-1.0:i386
```

## 🔄 Updating SkyRAT

### **Update from Git**
```bash
# Navigate to project directory
cd skyrat

# Pull latest changes
git pull origin main

# Update Python dependencies
pip3 install -r requirements.txt --upgrade

# Update Android SDK if needed
./tools/android-sdk-setup.sh
```

### **Clean Reinstall**
```bash
# Remove existing installation
rm -rf skyrat/

# Clone fresh copy
git clone https://github.com/techsky-eh/skyrat.git
cd skyrat

# Run installation process
pip3 install -r requirements.txt
./tools/android-sdk-setup.sh
```

## 🧪 Development Installation

### **Additional Development Tools**
```bash
# Install development dependencies
pip3 install -r requirements-dev.txt

# Install pre-commit hooks
pre-commit install

# Run tests to verify setup
python3 -m pytest
```

### **IDE Setup**
For development with IDEs:

#### **VS Code**
1. Install Python extension
2. Install Kotlin extension  
3. Open project folder
4. Configure Python interpreter to virtual environment

#### **PyCharm**
1. Open project directory
2. Configure Python interpreter
3. Install Kotlin plugin
4. Configure Android SDK path

## ✅ Installation Verification Checklist

- [ ] Python 3.7+ installed and working
- [ ] Java JDK 8+ installed and in PATH
- [ ] Git installed and working
- [ ] SkyRAT repository cloned
- [ ] Python dependencies installed
- [ ] Android SDK installed and configured
- [ ] Environment variables set correctly
- [ ] `python3 skyrat.py --help` works
- [ ] `cd android && ./gradlew tasks` works
- [ ] Test APK build successful

## 🆘 Getting Help

If you encounter issues during installation:

1. **Check logs**: Look for error messages in terminal output
2. **Search issues**: Check [GitHub Issues](https://github.com/techsky-eh/skyrat/issues)
3. **Ask for help**: Create new issue with:
   - Operating system and version
   - Python and Java versions
   - Complete error message
   - Steps you've tried

## 📚 Next Steps

After successful installation:
1. Read the [Usage Guide](usage.md)
2. Try the [Quick Setup](../QUICK_SETUP.md)
3. Review [Command Reference](commands.md)
4. Check [Examples](../examples/)

---

**Congratulations! SkyRAT is now installed and ready for Android security testing.**