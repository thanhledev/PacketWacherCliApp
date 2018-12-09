# PacketWacherCliApp
PacketWacher Command line app

## PacketWatcher Command line app - version 1.0

### Commandline arguments:

1. TCP (UDP) server, mode auto, listening to port 10000

```
-m server -t auto -p tcp/udp -l 10000
```

2. TCP (UDP) server, mode manual, listening to port 10000

```
-m server -t manual -p tcp/udp -l 10000
```

3. TCP/UDP client, mode auto, server listening at 192.168.1.29 (depends on each network configuration) port 10000, sending out 50000 packets with maximum sending delay of 200ms

```
-m client -t auto -p tcp/udp -si 192.168.1.29 -sp 10000 -ps 50000 -d 200
```

4. TCP/UDP client, mode manual, server listening at 192.168.1.29 (depends on each network configuration) port 10000, send outgoing packet one by one manually.

```
-m client -t manual -p tcp/udp -si 192.168.1.29 -sp 10000
```
