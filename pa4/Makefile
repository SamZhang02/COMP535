CC = gcc
CFLAGS = -Wall -Wextra

# Default build (no -g)
all: sender receiver send_nack

# The debug target
debug: CFLAGS += -g -O0 -DDEBUG -DEBUG
debug: clean all

sender: sender.c multicast.c checksum.c protocol.c
	$(CC) $(CFLAGS) -o sender sender.c multicast.c checksum.c protocol.c

receiver: receiver.c multicast.c checksum.c file_tracker.c dlist.c protocol.c
	$(CC) $(CFLAGS) -o receiver receiver.c multicast.c checksum.c file_tracker.c dlist.c protocol.c

send_nack: testing/send_nack.c multicast.c protocol.c
	$(CC) $(CFLAGS) -o testing/send_nack testing/send_nack.c multicast.c protocol.c

clean:
	rm -f sender receiver testing/send_nack *.o
