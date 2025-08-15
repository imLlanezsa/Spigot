<div align="center">
  <h1>⚙️ cSpigot Custom</h1>
  <p><i>Last updated: May 3, 2025 at 23:34:11 UTC by @mathssw</i></p>
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
- ✅ **Sign Exploit Protection**: Prevents JSON-based sign exploits that could crash clients or the server
- ✅ **Boundary Enforcement**: Ensures players can't send packets with coordinates outside valid ranges

<div align="center">
  <img src="https://imgur.com/a/fPCvKHU" alt="Position Validation" width="600">
</div>

### 🔍 Exploit Packet Listener

The `ExploitPacketListener` provides additional protection against known vulnerabilities:

- ✅ **FAWE Exploit Prevention**: Blocks attempts to use the FastAsyncWorldEdit exploit
- ✅ **Multiverse Crash Protection**: Prevents regex-based attacks that target the Multiverse plugin
- ✅ **Tab Completion Filtering**: Sanitizes tab completion requests to prevent command injection

<div align="center">
  <img src="https://imgur.com/a/K6GPegy" alt="Exploit Listener" width="600">
</div>

### 📋 Sign Exploit Protection

Our comprehensive sign validation system protects against malicious JSON formatting and nested structure attacks:

<div align="center">
  <img src="https://imgur.com/a/lwTAW6y" alt="Sign Protection" width="600">
</div>

<details>
<summary>📊 <b>Security Impact</b> (Click to expand)</summary>
<br>
These security features help maintain server stability and protect against common attack vectors used by malicious clients including:

- LiquidBounce crash exploits
- Coordinate-based server crashes
- JSON malformation attacks
- Nested structure overflow attempts
- Plugin-specific vulnerabilities (FAWE, Multiverse)
</details>

<hr />

## 🐞 Bug Reports & Feature Requests

Feedback is welcome! You can:
- 📝 Report bugs by opening an issue in this repository
- 💡 Suggest improvements via pull requests
- 📨 Contact the maintainers directly

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