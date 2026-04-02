#ifndef CHECKSUM_H
#define CHECKSUM_H

#include "protocol.h"
#include <stddef.h>

unsigned char checksum_encode(const unsigned char *payload, size_t payload_len);
int checksum_decode(const DataPacket *pkt, size_t payload_len);

#endif
