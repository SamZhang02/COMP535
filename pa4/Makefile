CC = gcc
CFLAGS = -Wall -Wextra -g

all: sender receiver

sender: sender.c multicast.c protocol.h
	$(CC) $(CFLAGS) -o sender sender.c multicast.c

receiver: receiver.c multicast.c protocol.h
	$(CC) $(CFLAGS) -o receiver receiver.c multicast.c

clean:
	rm -f sender receiver *.o
