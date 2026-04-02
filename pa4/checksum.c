#include "checksum.h"

unsigned char checksum_encode(const unsigned char *payload, size_t payload_len) {
  unsigned char check = 0;

  for (size_t i = 0; i < payload_len; i++) {
    check ^= payload[i];
  }

  return check;
}

int checksum_decode(const DataPacket *pkt, size_t payload_len) {
  unsigned char computed =
      checksum_encode((const unsigned char *)pkt->payload, payload_len);
  return computed == pkt->checksum;
}
