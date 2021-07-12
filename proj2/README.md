# SDIS Project

SDIS Project for group T7G26.

## Compiling the code:

```
$ ./scripts/compile.sh
```

## Running the code:

### As a client
```
$ ./scripts/client.sh <address:port> BACKUP <filepath> <replicationDegree>
$ ./scripts/client.sh <address:port> RESTORE <filepath>
$ ./scripts/client.sh <address:port> RECLAIM <diskSpace>
$ ./scripts/client.sh <address:port> STATE
```

### As a peer
```
$ ./scripts/server.sh <address:port> [knownHostKey <knownHostAddress:knownHostPort>]
```

Group members:

1. Alexandre Abreu (up201800168@fe.up.pt)
2. Iohan Soares (up201801011@fe.up.pt)
3. João Pinto (up201806667@fe.up.pt)
4. José Maçães (up201806622@fe.up.pt)
