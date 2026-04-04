#ifndef CHECKSUM_H
#define CHECKSUM_H

#include "protocol.h"
#include <stddef.h>
#include <stdint.h>

uint32_t checksum_encode(const unsigned char *payload, size_t payload_len);
uint32_t checksum_combine(uint32_t running_checksum,
                          uint32_t chunk_checksum,
                          uint32_t seq_num,
                          uint32_t payload_len);
int checksum_decode(const DataPacket *pkt, size_t payload_len);

#endif
