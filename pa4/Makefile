CC = gcc
CFLAGS = -Wall -Wextra

# Default build (no -g)
all: sender receiver

# The debug target
debug: CFLAGS += -g -O0 -DDEBUG -DEBUG
debug: clean all

sender: sender.c multicast.c checksum.c
	$(CC) $(CFLAGS) -o sender sender.c multicast.c checksum.c

receiver: receiver.c multicast.c checksum.c
	$(CC) $(CFLAGS) -o receiver receiver.c multicast.c checksum.c

clean:
	rm -f sender receiver *.o
