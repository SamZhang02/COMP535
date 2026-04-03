#ifndef __MULTICAST_H__
#define __MULTICAST_H__

#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/poll.h>
#include <sys/socket.h>
#include <sys/types.h>

typedef struct _mcast {
  struct sockaddr_in addr;    // Destination Address
  struct sockaddr_in my_addr; // Local Address
  unsigned int addrlen;       // Memory Size of addr
  unsigned int my_addrlen;    // Memory Size of my_addr
  int sock;                   // SocketFile Descriptor
  struct ip_mreq mreq;        // IP Multicast Request
  struct pollfd fds[2];       // File Descriptors to watch
  int nfds; // Number of file descriptors - Hardcoded to POLLIN (1)
} MCast;

MCast *multicast_init(char *mcast_addr, int sport, int rport);
int multicast_send(MCast *m, void *msg, int msglen);
void multicast_setup_recv(MCast *m);
int multicast_receive(MCast *m, void *buf, int bufsize);
int multicast_check_receive(MCast *m);
void multicast_destroy(MCast *m);

#endif
