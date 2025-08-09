#!/bin/bash

# SkyRAT Android SDK Setup Script
# Automatically sets up Android SDK for command line APK building

set -e  # Exit on any error

echo "================================================"
echo "SkyRAT Android SDK Setup"
echo "Tech Sky - Security Research Team"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check operating system
detect_os() {
    if [[ "$OSTYPE" == "linux-gnu"* ]]; then
        OS="linux"
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="mac"
    elif [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
        OS="windows"
    else
        print_error "Unsupported operating system: $OSTYPE"
        exit 1
    fi
    print_info "Detected OS: $OS"
}

# Check Java installation
check_java() {
    print_info "Checking Java installation..."
    
    if command_exists java && command_exists javac; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
        print_success "Java found: $JAVA_VERSION"
        
        # Check if Java version is 8 or higher
        JAVA_MAJOR=$(echo $JAVA_VERSION | cut -d'.' -f1)
        if [ "$JAVA_MAJOR" -ge "8" ] || [[ "$JAVA_VERSION" == "1.8"* ]]; then
            print_success "Java version is compatible"
        else
            print_warning "Java 8 or higher recommended, found: $JAVA_VERSION"
        fi
    else
        print_error "Java not found. Please install JDK 8 or higher"
        print_info "Ubuntu/Debian: sudo apt install openjdk-11-jdk"
        print_info "CentOS/RHEL: sudo yum install java-11-openjdk-devel"
        print_info "macOS: brew install openjdk@11"
        exit 1
    fi
}

# Set up Android SDK directory
setup_android_home() {
    print_info "Setting up Android SDK directory..."
    
    if [ -z "$ANDROID_HOME" ]; then
        # Set default Android SDK location
        case $OS in
            "linux"|"mac")
                DEFAULT_ANDROID_HOME="$HOME/Android/Sdk"
                ;;
            "windows")
                DEFAULT_ANDROID_HOME="$HOME/AppData/Local/Android/Sdk"
                ;;
        esac
        
        echo -e "\nAndroid SDK location:"
        echo "Press Enter to use default: $DEFAULT_ANDROID_HOME"
        echo "Or enter custom path:"
        read -r CUSTOM_PATH
        
        if [ -n "$CUSTOM_PATH" ]; then
            ANDROID_HOME="$CUSTOM_PATH"
        else
            ANDROID_HOME="$DEFAULT_ANDROID_HOME"
        fi
    fi
    
    print_info "Android SDK location: $ANDROID_HOME"
    
    # Create directory if it doesn't exist
    mkdir -p "$ANDROID_HOME"
    export ANDROID_HOME
}

# Download and install Android command line tools
install_android_tools() {
    print_info "Installing Android command line tools..."
    
    # Command line tools download URLs
    case $OS in
        "linux")
            TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip"
            ;;
        "mac")
            TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-mac-9477386_latest.zip"
            ;;
        "windows")
            TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-win-9477386_latest.zip"
            ;;
    esac
    
    TOOLS_ZIP="$ANDROID_HOME/commandlinetools.zip"
    
    # Download command line tools if not already present
    if [ ! -d "$ANDROID_HOME/cmdline-tools" ]; then
        print_info "Downloading Android command line tools..."
        
        if command_exists wget; then
            wget -O "$TOOLS_ZIP" "$TOOLS_URL"
        elif command_exists curl; then
            curl -L -o "$TOOLS_ZIP" "$TOOLS_URL"
        else
            print_error "wget or curl required to download Android tools"
            exit 1
        fi
        
        # Extract tools
        print_info "Extracting command line tools..."
        cd "$ANDROID_HOME"
        unzip -q "$TOOLS_ZIP"
        
        # Move to correct directory structure
        mkdir -p cmdline-tools
        mv cmdline-tools latest_temp
        mv latest_temp cmdline-tools/latest
        
        # Clean up
        rm "$TOOLS_ZIP"
        
        print_success "Android command line tools installed"
    else
        print_info "Command line tools already installed"
    fi
}

# Install required SDK packages
install_sdk_packages() {
    print_info "Installing required SDK packages..."
    
    SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
    
    # Make sdkmanager executable
    chmod +x "$SDKMANAGER"
    
    # Accept licenses
    yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
    
    # Install required packages
    PACKAGES=(
        "platform-tools"           # adb, fastboot
        "platforms;android-33"     # Android 13 (API 33)
        "platforms;android-34"     # Android 14 (API 34)
        "build-tools;33.0.2"      # Build tools
        "build-tools;34.0.0"      # Latest build tools
    )
    
    for package in "${PACKAGES[@]}"; do
        print_info "Installing $package..."
        "$SDKMANAGER" "$package"
    done
    
    print_success "SDK packages installed"
}

# Set up environment variables
setup_environment() {
    print_info "Setting up environment variables..."
    
    # Determine shell configuration file
    case $SHELL in
        */bash)
            SHELL_RC="$HOME/.bashrc"
            ;;
        */zsh)
            SHELL_RC="$HOME/.zshrc"
            ;;
        */fish)
            SHELL_RC="$HOME/.config/fish/config.fish"
            ;;
        *)
            SHELL_RC="$HOME/.profile"
            ;;
    esac
    
    # Add Android environment variables
    ENV_VARS="
# Android SDK Environment Variables (added by SkyRAT setup)
export ANDROID_HOME=\"$ANDROID_HOME\"
export PATH=\"\$ANDROID_HOME/cmdline-tools/latest/bin:\$PATH\"
export PATH=\"\$ANDROID_HOME/platform-tools:\$PATH\"
export PATH=\"\$ANDROID_HOME/build-tools/34.0.0:\$PATH\"
"
    
    # Check if already configured
    if grep -q "ANDROID_HOME" "$SHELL_RC" 2>/dev/null; then
        print_warning "Android environment variables already configured in $SHELL_RC"
    else
        echo "$ENV_VARS" >> "$SHELL_RC"
        print_success "Environment variables added to $SHELL_RC"
        print_info "Run 'source $SHELL_RC' or restart your terminal"
    fi
    
    # Export for current session
    export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
    export PATH="$ANDROID_HOME/platform-tools:$PATH"
    export PATH="$ANDROID_HOME/build-tools/34.0.0:$PATH"
}

# Verify installation
verify_installation() {
    print_info "Verifying Android SDK installation..."
    
    # Test if tools are accessible
    if [ -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
        print_success "sdkmanager: OK"
    else
        print_error "sdkmanager not found"
        return 1
    fi
    
    if [ -x "$ANDROID_HOME/platform-tools/adb" ]; then
        print_success "adb: OK"
    else
        print_error "adb not found"
        return 1
    fi
    
    # Test if gradle can find Android SDK
    cd "$(dirname "$0")/../android"
    if [ -f "./gradlew" ]; then
        chmod +x ./gradlew
        if ./gradlew tasks >/dev/null 2>&1; then
            print_success "Gradle Android integration: OK"
        else
            print_warning "Gradle may have issues (this is normal on first run)"
        fi
    fi
    
    print_success "Android SDK setup completed successfully!"
}

# Main setup process
main() {
    echo
    print_info "Starting Android SDK setup for SkyRAT..."
    echo
    
    detect_os
    check_java
    setup_android_home
    install_android_tools
    install_sdk_packages
    setup_environment
    verify_installation
    
    echo
    echo "================================================"
    print_success "Android SDK setup completed!"
    echo "================================================"
    echo
    print_info "Next steps:"
    echo "1. Run 'source ~/.bashrc' (or restart terminal)"
    echo "2. Test APK building: python3 skyrat.py --build -i YOUR_IP -p 8000"
    echo "3. Check tools: adb version"
    echo
    print_warning "Remember to restart your terminal or run 'source $SHELL_RC'"
    echo
}

# Run main function
main "$@"