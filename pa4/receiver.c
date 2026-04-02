#include "checksum.h"
#include "multicast.h"
#include "protocol.h"

#include <stddef.h>
#include <stdio.h>
#include <unistd.h>

#define MCAST_ADDR "239.0.0.1"
#define SENDER_PORT 5000
#define RECEIVER_PORT 5000
#define RECV_BUF_SIZE 65535

static const char *packet_type_to_string(PacketType type) {
  switch (type) {
  case PKT_TYPE_METADATA:
    return "METADATA";
  case PKT_TYPE_DATA:
    return "DATA";
  case PKT_TYPE_NACK:
    return "NACK";
  default:
    return "UNKNOWN";
  }
}

static void handle_metadata_packet(const unsigned char *buf, int received) {
  if (received < (int)sizeof(MetadataPacket)) {
    printf("  metadata packet too short (%d bytes)\n", received);
    return;
  }

  const MetadataPacket *pkt = (const MetadataPacket *)buf;
  printf("  metadata: file_id=%u, file_size=%u, chunks=%u, filename=%s\n",
         pkt->file_id,
         pkt->file_size,
         pkt->num_chunks,
         pkt->filename);
}

static void handle_data_packet(const unsigned char *buf, int received) {
  if (received < (int)sizeof(DataPacket)) {
    printf("  data packet too short (%d bytes)\n", received);
    return;
  }

  const DataPacket *pkt = (const DataPacket *)buf;

  size_t header_size = offsetof(DataPacket, payload);
  size_t payload_len = received - header_size;
  //
  int checksum_ok = checksum_decode(pkt, payload_len);
  printf("  data: file_id=%u, seq=%u, checksum=%s(%u)\n",
         pkt->file_id,
         pkt->seq_num,
         checksum_ok ? "valid" : "invalid",
         pkt->checksum);

#if DEBUG
  printf("  payload: ");
  fwrite(pkt->payload, 1, payload_len, stdout);
  printf("\n");
#endif
}

static void handle_nack_packet(const unsigned char *buf, int received) {
  if (received < (int)sizeof(NackPacket)) {
    printf("  nack packet too short (%d bytes)\n", received);
    return;
  }

  const NackPacket *pkt = (const NackPacket *)buf;
  printf("  nack: file_id=%u, missing_seq=%u\n",
         pkt->file_id,
         pkt->missing_seq_num);
}

static void handle_packet(const unsigned char *buf, int received) {
  const PacketHeader *hdr = (const PacketHeader *)buf;
  switch (hdr->type) {
  case PKT_TYPE_METADATA:
    handle_metadata_packet(buf, received);
    break;
  case PKT_TYPE_DATA:
    handle_data_packet(buf, received);
    break;
  case PKT_TYPE_NACK:
    handle_nack_packet(buf, received);
    break;
  default:
    printf("  no handler for packet type=%d\n", hdr->type);
    break;
  }
}

int main(void) {
  unsigned char buf[RECV_BUF_SIZE];

  MCast *mcast = multicast_init(MCAST_ADDR, SENDER_PORT, RECEIVER_PORT);
  multicast_setup_recv(mcast);

  printf("Receiver listening on %s:%d\n", MCAST_ADDR, RECEIVER_PORT);

  while (1) {
    int received = multicast_receive(mcast, buf, sizeof(buf));
    if (received < (int)sizeof(PacketHeader)) {
      printf("Received short packet (%d bytes)\n", received);
      continue;
    }

    PacketHeader *hdr = (PacketHeader *)buf;
    printf("Received packet: type=%s (%d), bytes=%d, payload_size=%u\n",
           packet_type_to_string(hdr->type),
           hdr->type,
           received,
           hdr->payload_size);
    handle_packet(buf, received);

    fflush(stdout);
  }

  multicast_destroy(mcast);
  return 0;
}
