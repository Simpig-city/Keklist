<div align="center">
 
 # Keklist

![License](https://img.shields.io/badge/license-GPLv3-blue)
[![Crowdin](https://badges.crowdin.net/keklist/localized.svg)](https://crowdin.com/project/Keklist)
![TeamCity](https://teamcity.lptp.systems/app/rest/builds/buildType:(id:Keklist_build)/statusIcon)
[![Contributors](https://img.shields.io/github/contributors/simpig-city/keklist)](https://github.com/Simpig-city/Keklist/graphs/contributors)
[![Discord](https://img.shields.io/discord/1064505564870230106?color=blue&label=discord)](https://discord.gg/Vseq6Sqcau)

Keklist is an easy to use custom-made black/whitelist for your minecraft server. It's way more customizable than the basic vanilla whitelist and offers way more features. The general idea of this plugin is to have one plugin for managing who can join your server without banning them. Keklist is really lightweight and doesn't use many resources but still full-featured. **Keklist is the only whitelist plugin you'll ever need!**
</div>

---

## Features
Keklist includes many different features to make your whitelist experience as good as possible. Here's a list of all features:
- IPv4 and IPv6 support
- Floodgate/Bedrock support
- Custom MOTD for blacklisted IPs
- Fully customizable messages
  - Including minimessage support
- Discord webhook notifications
- PlaceholderAPI support
- ~~MySQL~~ MariaDB support for syncing your whitelist across multiple servers
- **Ingame GUI**
- Velocity plugin
  - Adds limbo support
  - API for plugin developers

## Installation & Download
You can download the latest version of Keklist on [github releases](https://github.com/Simpig-city/Keklist/releases/latest) or on [modrinth](https://modrinth.com/plugin/keklist). 
All versions including beta releases can be found [here](https://github.com/Simpig-city/Keklist/releases/). Alpha builds are soon available for download, but this is still WiP. 
<br> <br>
To install Keklist you just need to drop the jar file into your plugins folder and restart your server. Keklist will automatically create a config file. You can find the config file in the `plugins/Keklist` folder. **Keklist requires Paper 1.20.1+ (build: 94+) or forks i.g. Purpur**. More about the config file and other features can be found [here](https://github.com/simpig-city/Keklist/wiki/config). 
<br> <br>
Keklist officially supports the following server software: Paper, Pufferfish, Purpur

## Wiki
If you have any problems or need documentation about the plugin, you can just contact us
or visit our wiki here on github!
Every feature and the entire config is documented and explained, but feel free to ask anything!
Our wiki can be found [here](https://github.com/simpig-city/Keklist/wiki)

## bStats
Keklist uses [bStats](https://bstats.org/plugin/bukkit/Keklist/12078) to collect anonymous data about the plugin. You can disable this in the config.yml file.

[![bStats Graph Data](https://bstats.org/signatures/bukkit/Keklist.svg)](https://bstats.org/plugin/bukkit/Keklist/18279)

---

### API
Keklist offers an API for plugin developers to use. You can find the API documentation [here](https://github.com/simpig-city/keklist/wiki/API). *soon* <br>
Our maven/gradle dependency:

Maven
```xml
<dependency>
  <groupId>de.sage.minecraft</groupId>
  <artifactId>keklist</artifactId>
  <version>version</version>
  <scope>provided</scope>
</dependency>
```
Gradle
```kotlin
dependencies{
    compileOnly("de.sage.minecraft:keklist:version")
}
```

### Self compile
If you want to compile the plugin yourself, you can easily do that. Just run:
```bash
git clone https://github.com/Simpig-city/Keklist.git
cd Keklist
mvn package
```
The compiled jar file will be in the `target` folder. ***You need to have maven and java jdk 17 installed.***

### Contributing
If you want to help us with the development of Keklist, you can do that by creating a pull request. We'll review it and if everything is fine, we'll eventually merge it. If you want to add a new feature, please create an issue first, so we can discuss it. If you want to help us with the translation, you can do that [here](https://crowdin.com/project/keklist). If you want to help us with the documentation, you can do that on our discord server.

---

## Contact
If you have any questions or problems, you can contact us on our [discord server](https://discord.gg/Vseq6Sqcau). You can also create an issue on github or email us.
