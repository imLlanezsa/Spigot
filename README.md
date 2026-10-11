Carbon Spigot fork made by Mathssw and Sakiio_ but with some fixes by SuperMario99 and llanezsa


<div align="center">
  <h1>⚙️ cSpigot Custom</h1>
  <p><i>Last updated: 2025-05-04 02:29:21 UTC by @mathssw</i></p>
  <hr />

  <p>An advanced implementation of the <a href="https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit">Bukkit API</a>, tailored for Minecraft servers</p>
  <p>Maintained by the community — with core contributions from <b>Sakiio_</b> and <b>mathssw</b></p>

  <img src="https://img.shields.io/badge/Minecraft-1.8.8-green" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Status-Maintained-blue" alt="Status">
  <img src="https://img.shields.io/badge/Java-8+-orange" alt="Java Version">
</div>

## 📋 Table of Contents
- [Overview](#-overview)
- [Security Features](#️-security-features)
- [Performance Optimizations](#-performance-optimizations)
- [Bug Reports & Feature Requests](#-bug-reports--feature-requests)
- [Building from Source](#️-building-from-source)

## 🧩 Overview

**cSpigot Custom** provides a clean and stable integration of the Bukkit plugin interface on top of Minecraft server internals. This fork is focused on:

- 🚀 **Performance optimization** for high-traffic servers
- 🧪 **Stability improvements** to prevent crashes and data loss
- 🔌 **Modular extensibility** for plugin developers
- 💻 **Modern development ergonomics** for today's coders

<hr />

## 🛡️ Security Features

Our implementation includes robust security measures to protect servers from common exploits:

### 🚫 Anti-Crash Protection

The `cSpigotAntiCrash` system prevents various attack vectors:

- ✅ **Movement Packet Validation**: Blocks malicious packets with invalid position data
- ✅ **Sign Exploit Protection**: Prevents JSON-based sign exploits that could crash clients
- ✅ **Boundary Enforcement**: Ensures players can't send packets with invalid coordinates

### 🔍 Exploit Packet Listener

The `ExploitPacketListener` provides additional protection against known vulnerabilities:

- ✅ **FAWE Exploit Prevention**: Blocks attempts to use the FastAsyncWorldEdit exploit
- ✅ **Multiverse Crash Protection**: Prevents regex-based attacks targeting Multiverse plugin
- ✅ **Tab Completion Filtering**: Sanitizes tab completion requests to prevent command injection

### 📋 Sign Exploit Protection

Our comprehensive sign validation system protects against:

- ✅ **Malicious JSON formatting**: Detects and blocks invalid JSON structures
- ✅ **Nested Structure Attacks**: Prevents deeply nested JSON that can crash clients
- ✅ **Oversized Content**: Blocks sign content that exceeds safe character limits

### 🔄 Custom Payload Protection

Our payload validation system prevents various types of exploits:

- ✅ **LiquidBounce Detection**: Blocks known exploit channels from hacked clients
- ✅ **Size Validation**: Prevents oversized payloads that can cause memory issues
- ✅ **Channel Filtering**: Blocks unauthorized or malicious packet channels

<details>
<summary>📊 <b>Security Impact</b> (Click to expand)</summary>
<br>
These features protect against:

- LiquidBounce crash exploits
- Coordinate-based server crashes
- JSON malformation attacks
- Nested structure overflow attempts
- Plugin-specific vulnerabilities (FAWE, Multiverse)
</details>

## 🚀 Performance Optimizations

cSpigot Custom includes numerous performance improvements:

- ⚡ **Optimized tick loop** for improved TPS stability
- 🧵 **Enhanced threading model** for better multi-core utilization
- 🧮 **Improved chunk loading algorithm** for faster world loading
- 📦 **Reduced memory footprint** through more efficient data structures

### ⚡ Expected Performance

#### 📊 Player Capacity:
- **400-600+ concurrent players** supported
- Optimized for network-intensive gameplay

#### 🕒 TPS Stability:
- **Rock-solid 20 TPS** with 250-300 players
- **19+ TPS** even with 400+ players under normal conditions
- Anti-exploit features maintain stability during attack attempts

<details>
<summary>⚙️ <b>Configuration Options</b> (Click to expand)</summary>
<br>
Key performance settings:

- `async-chunks: true` - Process chunk loading asynchronously
- `optimize-explosions: true` - Use optimized explosion algorithm
- `mob-spawn-range: 6` - Reduce mob spawning range
- `entity-activation-range` - Customize entity activation ranges
</details>

## 🐞 Bug Reports & Feature Requests

Feedback is welcome! You can:
- 📝 Report bugs by opening an issue in this repository
- 💡 Suggest improvements via pull requests
- 📨 Contact the maintainers directly

When reporting bugs, please include your server's version information, steps to reproduce, and any relevant logs.

## 🏗️ Building from Source

This project uses [Maven 3](https://maven.apache.org/) for dependency management and compilation.

### ✅ Prerequisites

- [Java 8](https://www.oracle.com/java/technologies/javase/javase8-archive-downloads.html) or higher
- [Maven 3+](https://maven.apache.org/download.cgi)
- [Git](https://git-scm.com/)

### 🔨 Compile Instructions

```bash
# Clone the repository
git clone https://github.com/your-username/cSpigot-Custom.git

# Navigate to project directory
cd cSpigot-Custom

# Build with Maven
mvn clean install
