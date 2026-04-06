#include "checksum.h"
#include "logging.h"
#include "multicast.h"
#include "protocol.h"
#include <signal.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>

#define MCAST_ADDR "239.0.0.1"
#define SENDER_PORT 5000
#define RECEIVER_PORT 5000

#define RECV_BUF_SIZE 65535

#define USAGE "Usage: %s [-c chunk_size] <file1> <file2> ...\n"
#define DEFAULT_CHUNK_SIZE 1024

#define CYCLE_LENGTH_SECONDS 3

// ---- Helper code for data collection, not part of protocol
typedef struct {
  struct timespec send_start_time;
  int num_packets_send;
} Statistics;

Statistics *stats_init() {
  Statistics *stats = malloc(sizeof(Statistics));

  clock_gettime(CLOCK_MONOTONIC, &stats->send_start_time);
  ;
  stats->num_packets_send = 0;

  return stats;
}

static void multicast_send_with_stats(MCast *mcast,
                                      void *data,
                                      size_t size,
                                      Statistics *stats) {
  multicast_send(mcast, data, size);
  stats->num_packets_send++;
}
// --------

static volatile sig_atomic_t keep_running = 1;

static void handle_sigint(int signum) {
  (void)signum;
  keep_running = 0;
}

static uint32_t compute_file_checksum(const char *filename) {
  return checksum_file_path(filename);
}

DataPacket *generate_data_packet(MetadataPacket file_catalog[],
                                 uint16_t file_id,
                                 uint32_t seq_num,
                                 int chunk_size) {

  size_t packet_size = sizeof(DataPacket) + chunk_size;
  DataPacket *pkt = malloc(packet_size);

  if (pkt == NULL) {
    perror("malloc data packet");
    return NULL;
  }

  pkt->header.type = PKT_TYPE_DATA;
  pkt->header.payload_size = 0;
  pkt->file_id = file_id;
  pkt->seq_num = seq_num;

  FILE *fp = fopen(file_catalog[file_id].filename, "rb");
  if (!fp) {
    perror("fopen in generate_data_packet");
    free(pkt);
    return NULL;
  }

  long offset = (long)seq_num * chunk_size;
  fseek(fp, offset, SEEK_SET);

  // The last chunk might be smaller than chunk_size
  // We should zero out the payload first or track the actual bytes read.
  memset(pkt->payload, 0, chunk_size);
  size_t bytes_read = fread(pkt->payload, 1, chunk_size, fp);
  pkt->header.payload_size = (uint32_t)bytes_read;

  // need to cast to unsigned char because we use a single byte XOR checksum
  pkt->checksum =
      checksum_encode((const unsigned char *)pkt->payload, bytes_read);

  fclose(fp);
  return pkt;
}

static void handle_nack_packet(const unsigned char *buf,
                               int received,
                               MetadataPacket file_catalog[],
                               int num_files,
                               MCast *mcast,
                               int chunk_size,
                               Statistics *stats) {
  if (received < (int)sizeof(NackPacket)) {
    log_info("  nack packet too short (%d bytes)\n", received);
    return;
  }

  const NackPacket *pkt = (const NackPacket *)buf;
  log_info(" Received NACK: file_id=%u, missing_seq_range=[%u,%u]\n",
           pkt->file_id,
           pkt->missing_seq_num_start,
           pkt->missing_seq_num_end);

  uint16_t file_id = pkt->file_id;
  uint32_t missing_seq_num_start = pkt->missing_seq_num_start;
  uint32_t missing_seq_num_end = pkt->missing_seq_num_end;

  // This works under the assumption that file ids are sequential, which is true
  // in our sender protocol in main()
  if (file_id >= num_files) {
    fprintf(stderr, "Received NACK for file not in catalog, id=%u\n", file_id);
    return;
  }

  MetadataPacket file_to_resend = file_catalog[file_id];
  for (uint32_t i = missing_seq_num_start; i <= missing_seq_num_end; i++) {
    if (i >= file_to_resend.num_chunks) {
      fprintf(stderr, "File with id=%u does not have chunk=%u\n", file_id, i);
      continue;
    }

    DataPacket *data_packet =
        generate_data_packet(file_catalog, file_id, i, chunk_size);

    if (!data_packet) {
      continue;
    }

    size_t size = sizeof(DataPacket) + data_packet->header.payload_size;

    log_info("Sending data packet, file_id=%u, seq_num=%u, "
             "payload_size=%zubytes\n",
             data_packet->file_id,
             data_packet->seq_num,
             size);

    multicast_send_with_stats(mcast, data_packet, size, stats);

    free(data_packet);
  }
}

static void handle_ack_packet(const unsigned char *buf,
                              int received,
                              const Statistics *stats) {
  if (received < (int)sizeof(AckPacket)) {
    log_info("  ack packet too short (%d bytes)\n", received);
    return;
  }

  struct timespec current_time;
  clock_gettime(CLOCK_MONOTONIC, &current_time);

  double elapsed_ms =
      (current_time.tv_sec - stats->send_start_time.tv_sec) * 1000.0 +
      (current_time.tv_nsec - stats->send_start_time.tv_nsec) / 1000000.0;

  if (elapsed_ms <= 0.0) {
    elapsed_ms = 0.001; // 1 microsecond floor in case it sends too fast
  }

  double packets_per_second =
      ((double)stats->num_packets_send / elapsed_ms) * 1000.0;

  printf("Received ACK packet: num_packets_send=%d, elapsed_ms=%.2f, "
         "throughput_pps=%.2f\n",
         stats->num_packets_send,
         elapsed_ms,
         packets_per_second);
}

static void handle_packet(const unsigned char *buf,
                          int received,
                          MetadataPacket file_catalog[],
                          int num_files,
                          MCast *mcast,
                          int chunk_size,
                          Statistics *stats

) {
  const PacketHeader *hdr = (const PacketHeader *)buf;

  if (hdr->type == PKT_TYPE_NACK) {
    handle_nack_packet(
        buf, received, file_catalog, num_files, mcast, chunk_size, stats);
  }

  if (hdr->type == PKT_TYPE_ACK) {
    handle_ack_packet(buf, received, stats);
  }
}

int main(int argc, char *argv[]) {
  signal(SIGINT, handle_sigint);

  Statistics *stats = stats_init();

  int chunk_size = DEFAULT_CHUNK_SIZE;
  int opt;

  while ((opt = getopt(argc, argv, "c:")) != -1) {
    switch (opt) {
    case 'c':
      chunk_size = atoi(optarg);
      if (chunk_size <= 0) {
        fprintf(stderr, "Error: Chunk size must be a positive integer.\n");
        exit(EXIT_FAILURE);
      }
      break;
    default: // unknown flags or missing arguments
      fprintf(stderr, USAGE, argv[0]);
      exit(EXIT_FAILURE);
    }
  }

  int forgotFiles = optind >= argc;
  if (forgotFiles) {
    fprintf(stderr, "Error: At least one file must be specified.\n");
    fprintf(stderr, USAGE, argv[0]);
    exit(EXIT_FAILURE);
  }

  int num_files = argc - optind;
  printf("Total number of files to send: %d\n", num_files);

  // The array of metadata of files to send, this is sent cyclically to all
  // receivers for them to update their states
  MetadataPacket file_catalog[num_files];

  for (int i = optind; i < argc; i++) {
    int file_index = i - optind;
    char *filename = argv[i];

    printf("Preparing file: %s\n", filename);

    FILE *fp = fopen(filename, "rb");
    if (fp == NULL) {
      fprintf(stderr, "Error: Could not open file '%s'.\n", filename);
      exit(EXIT_FAILURE);
    }

    fseek(fp, 0, SEEK_END);
    long file_size = ftell(fp);
    fclose(fp);

    file_catalog[file_index].header.type = PKT_TYPE_METADATA;
    file_catalog[file_index].header.payload_size = 0;
    file_catalog[file_index].file_id = file_index;
    file_catalog[file_index].file_size = file_size;

    file_catalog[file_index].num_chunks =
        (file_size + chunk_size - 1) / chunk_size;
    file_catalog[file_index].chunk_size = (uint32_t)chunk_size;
    file_catalog[file_index].file_checksum = compute_file_checksum(filename);

    strncpy(
        file_catalog[file_index].filename, filename, MAX_FILENAME_LENGTH - 1);
    file_catalog[file_index].filename[MAX_FILENAME_LENGTH - 1] = '\0';
  }

  MCast *mcast = multicast_init(MCAST_ADDR, SENDER_PORT, RECEIVER_PORT);
  multicast_setup_recv(mcast);

  log_info("Sender listening on %s:%u\n", MCAST_ADDR, RECEIVER_PORT);

  for (int i = 0; i < num_files; i++) {
    log_info("Sending metadata packet, file_id=%u\n", file_catalog[i].file_id);
    multicast_send_with_stats(
        mcast, &file_catalog[i], sizeof(MetadataPacket), stats);
  }

  for (int i = 0; i < num_files; i++) {
    MetadataPacket fileMetadata = file_catalog[i];
    for (uint32_t j = 0; j < fileMetadata.num_chunks; j++) {
      DataPacket *data_packet = generate_data_packet(
          file_catalog, fileMetadata.file_id, j, chunk_size);
      if (!data_packet) {
        continue;
      }
      size_t size = sizeof(DataPacket) + data_packet->header.payload_size;

      log_info("Sending data packet, file_id=%u, seq_num=%u, "
               "payload_size=%zubytes\n",
               data_packet->file_id,
               data_packet->seq_num,
               size);

      multicast_send_with_stats(mcast, data_packet, size, stats);

      free(data_packet);
    }
  }

  time_t last_send_time = time(NULL);
  while (keep_running) {
    unsigned char buf[RECV_BUF_SIZE];

    // this  blocks here for at most 1 second, then
    // goes to the rest of the content
    int received_packet = multicast_check_receive(mcast);
    if (received_packet < 0) {
      if (!keep_running)
        break;
      continue;
    }

    if (received_packet) {
      int received = multicast_receive(mcast, buf, sizeof(buf));

      if (received < 0) {
        if (!keep_running)
          break;
        continue;
      }

      if (received < (int)sizeof(PacketHeader)) {
        log_info("Received short packet (%d bytes)\n", received);
        continue;
      } else {
        handle_packet(
            buf, received, file_catalog, num_files, mcast, chunk_size, stats);
      }
    }

    time_t current_time = time(NULL);
    if (current_time - last_send_time >= CYCLE_LENGTH_SECONDS) {
      log_info("Sending cyclical control packets.\n");
      for (int i = 0; i < num_files; i++) {
        multicast_send_with_stats(
            mcast, &file_catalog[i], sizeof(MetadataPacket), stats);
      }

      last_send_time = current_time;
    }

    fflush(stdout);
  }

  log_info("Sender shutting down.\n");
  multicast_destroy(mcast);
  return 0;
}
