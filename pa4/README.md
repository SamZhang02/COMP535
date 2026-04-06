# UDP Multicast

NORM oriented multicast protocol with reliable and full file history transmission.

## Requirements

gcc

## Running

```shell
make
```

```
./sender -c [chunk-size] [files]
```

```
./receiver -d [received_file_dir]
```

## Developing

```shell
make debug
```

Enables all debug level logs, lets you use lldb as debugger.
