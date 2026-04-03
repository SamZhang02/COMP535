/*
 * Helper script to parse command line arguments and send a NACk to multicast
 * port.
 * */
#include "../multicast.h"
#include "../protocol.h"

#include <errno.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#define MCAST_ADDR "239.0.0.1"
#define SENDER_PORT 5000
#define RECEIVER_PORT 5000

static int parse_u32(const char *arg, uint32_t *out) {
  char *end = NULL;
  errno = 0;
  unsigned long value = strtoul(arg, &end, 10);
  if (errno != 0 || end == arg || *end != '\0' || value > UINT32_MAX) {
    return -1;
  }
  *out = (uint32_t)value;
  return 0;
}

int main(int argc, char *argv[]) {
  if (argc != 4) {
    fprintf(stderr, "Usage: %s <file_id> <chunk_start> <chunk_end>\n", argv[0]);
    return EXIT_FAILURE;
  }

  uint32_t file_id_u32 = 0;
  uint32_t chunk_start = 0;
  uint32_t chunk_end = 0;

  if (parse_u32(argv[1], &file_id_u32) != 0 || file_id_u32 > UINT16_MAX ||
      parse_u32(argv[2], &chunk_start) != 0 ||
      parse_u32(argv[3], &chunk_end) != 0) {
    fprintf(stderr, "Error: arguments must be valid unsigned integers.\n");
    return EXIT_FAILURE;
  }

  if (chunk_start > chunk_end) {
    fprintf(stderr, "Error: chunk_start must be <= chunk_end.\n");
    return EXIT_FAILURE;
  }

  NackPacket nack = {0};
  nack.header.type = PKT_TYPE_NACK;
  nack.header.payload_size =
      (uint32_t)(sizeof(NackPacket) - sizeof(PacketHeader));
  nack.file_id = (uint16_t)file_id_u32;
  nack.missing_seq_num_start = chunk_start;
  nack.missing_seq_num_end = chunk_end;

  MCast *mcast = multicast_init(MCAST_ADDR, SENDER_PORT, RECEIVER_PORT);
  multicast_send(mcast, &nack, sizeof(nack));
  multicast_destroy(mcast);

  printf("Sent NACK: file_id=%u, missing_seq_range=[%u,%u]\n",
         nack.file_id,
         nack.missing_seq_num_start,
         nack.missing_seq_num_end);
  return EXIT_SUCCESS;
}
