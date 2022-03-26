# RaknetFabric
A Fabric mod that allows using RakNet as Minecraft networking backend.

# Features
- Higher reliability and lower latency under unreliable and rate-limited client connections.
- Uses RakNet's multiple channels to achieve higher responsiveness. 
- Supports ViaVersion client-side and ViaVersion server-side. (MultiConnect compatibility is unknown)
- Currently, the networking behavior is similar to TCP Slow Start.

# How to use it?

## Prerequisites
- RaknetFabric is currently confirmed to be working on 1.18.2, 1.18.1 and 1.17.1, 
  other Minecraft versions are not tested.
- You need to have a UDP port opened at the same port number of your normal server port. 

## Installation and usage
- Download the latest release from [GitHub](https://github.com/RelativityMC/RaknetFabric/releases) 
  or development builds from [CodeMC](https://ci.codemc.io/job/RelativityMC/job/RaknetFabric/)
- Install the mod on both client and server.
- Prefix your server address with `raknet;` (or `raknetl;` to use high mtu) and save or connect directly. 
  (e.g. `raknet;example.com`)
- Enjoy!

