#include "checksum.h"

static uint32_t rotl32(uint32_t x, unsigned int r) {
  r &= 31U;
  if (r == 0U)
    return x;
  return (x << r) | (x >> (32U - r));
}

uint32_t checksum_encode(const unsigned char *payload, size_t payload_len) {
  uint32_t check = 0xA5A5A5A5u ^ (uint32_t)payload_len;

  for (size_t i = 0; i < payload_len; i++) {
    uint32_t v = (uint32_t)payload[i];
    check ^= (v << ((i & 3U) * 8U));
    check = rotl32(check, 5U);
    check ^= (check >> 16U);
  }

  return check ^ 0x9E3779B9u ^ (uint32_t)payload_len;
}

uint32_t checksum_combine(uint32_t running_checksum,
                          uint32_t chunk_checksum,
                          uint32_t seq_num,
                          uint32_t payload_len) {
  uint32_t mixed = chunk_checksum;
  mixed ^= seq_num * 0x9E3779B1u;
  mixed ^= payload_len * 0x85EBCA6Bu;
  mixed = rotl32(mixed, seq_num & 31U);
  return running_checksum ^ mixed;
}

int checksum_decode(const DataPacket *pkt, size_t payload_len) {
  uint32_t computed =
      checksum_encode((const unsigned char *)pkt->payload, payload_len);
  return computed == pkt->checksum;
}
