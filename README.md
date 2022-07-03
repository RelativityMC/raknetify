# Raknetify
A Fabric mod / Velocity plugin / BungeeCord plugin that uses RakNet to improve multiplayer experience significantly
under unreliable and rate-limited connections.

# Features
- Higher reliability and lower latency under unreliable and rate-limited client connections.
- Uses RakNet's multiple channels with priorities to achieve higher responsiveness. 
- Supports ViaVersion client-side and ViaVersion server-side. (MultiConnect compatibility is unknown)

# How to use it?

## Prerequisites
- Raknetify is currently confirmed to be working on 1.19, 1.18.2, 1.18.1 and 1.17.1, 
  other Minecraft versions are not tested.  
  It may work on newer Minecraft versions. If it doesn't, feel free to report to us.  
  Note: On proxies such as Velocity and BungeeCord, **unsupported client version** will cause
  multi-channelling failing to initialize, causing **reduced responsiveness**.  
- You need to have a UDP port opened at the same port number of your normal server port. 

## Installation and usage
- Download the latest release from 
  [GitHub](https://github.com/RelativityMC/raknetify/releases) 
  [Modrinth (Fabric)](https://modrinth.com/mod/raknetify/versions) 
  [CurseForge (Fabric)](https://www.curseforge.com/minecraft/mc-mods/raknetify/files)
  [SpigotMC (BungeeCord)](https://www.spigotmc.org/resources/raknetify-bungeecord.102509/)
  or development builds from [CodeMC](https://ci.codemc.io/job/RelativityMC/job/raknetify/)
- Install the mod on both client and server. (Installation on backend servers are not needed if using on proxies) 
- Prefix your server address with `raknet;` (or `raknetl;` to use high mtu) and save or connect directly. 
  (e.g. `raknet;example.com`)
- Enjoy!

