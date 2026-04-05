#include "checksum.h"
#include <stdio.h>

#define FNV1A_32_OFFSET_BASIS 2166136261u
#define FNV1A_32_PRIME 16777619u

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

int checksum_decode(const DataPacket *pkt, size_t payload_len) {
  uint32_t computed =
      checksum_encode((const unsigned char *)pkt->payload, payload_len);
  return computed == pkt->checksum;
}

uint32_t file_checksum_update(uint32_t running_checksum,
                              const unsigned char *payload,
                              size_t payload_len) {
  uint32_t hash = running_checksum;
  for (size_t i = 0; i < payload_len; i++) {
    hash ^= (uint32_t)payload[i];
    hash *= FNV1A_32_PRIME;
  }
  return hash;
}

uint32_t checksum_file_path(const char *path) {
  if (!path) {
    return 0;
  }

  FILE *fp = fopen(path, "rb");
  if (!fp) {
    return 0;
  }

  unsigned char buf[4096];
  uint32_t hash = FNV1A_32_OFFSET_BASIS;
  size_t bytes_read = 0;
  while ((bytes_read = fread(buf, 1, sizeof(buf), fp)) > 0) {
    hash = file_checksum_update(hash, buf, bytes_read);
  }

  if (ferror(fp)) {
    fclose(fp);
    return 0;
  }

  fclose(fp);
  return hash;
}
