Cannons
=======

by derPavlov
forked by Arhke

Bukkit: https://dev.bukkit.org/projects/cannons  
Spigot: https://www.spigotmc.org/resources/cannons.56764/  
Discord: https://discord.gg/taqPnRD389

## Install:

1. Copy `Cannons-{version}.jar` into your '_plugins/_' folder.
2. Start your server.
3. Edit the newly created '`config.yml`' in '_plugins/Cannons/_' and set your preferences
4. Restart your server.

## Uninstall:

1. Stop Server
2. Delete the '_plugins/Cannons/_' directory.
3. Delete 'plugins/Cannons.jar'
4. Start Server

## Have fun

-----------------------

## Building

Building is simple, with Apache Maven.

### Prerequisites

There are three major prerequisites to building this maven project.

1. You need a copy of OpenJDK 21 (any vendor)
   - [Microsoft build](https://microsoft.com/openjdk) || [Eclipse Temurin](https://adoptium.net/temurin/releases/) || [Amazon Corretto](https://aws.amazon.com/corretto)
   - Microsoft's, via WinGet (in PowerShell): `winget install --id "Microsoft.OpenJDK.21"`
   - macOS / Linux / WSL: You can also install any of these with [SDKMan!](https://sdkman.io/).


2. A copy of the [Apache Maven](https://maven.apache.org/) build tool.
   - You may find this bundled with your IDE, or available through your OS's package manager.
   - macOS / Linux / WSL: You can also install it with [SDKMan!](https://sdkman.io/).


3. You need to run [SpigotMC BuildTools](https://www.spigotmc.org/wiki/buildtools/), and generate remapped sources.
   - Run the following (at minimum): `java -jar BuildTools.jar --rev 1.21.1 --remapped`

### Using Maven

- Clean generated files: `mvn clean`


- Build Jar: `mvn package`
  - Creates two jar files in the `target/` directory:
    - _original-Cannons-*.jar_ (**Ignore this one.**)
    - _Cannons-*.jar_




