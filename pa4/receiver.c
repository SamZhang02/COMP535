#include "checksum.h"
#include "dlist.h"
#include "file_tracker.h"
#include "logging.h"
#include "multicast.h"
#include "protocol.h"

#include <errno.h>
#include <signal.h>
#include <stddef.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

#define MCAST_ADDR "239.0.0.1"
#define SENDER_PORT 5000
#define RECEIVER_PORT 5000
#define RECV_BUF_SIZE 65535
#define NACK_IDLE_GAP_MS 200ULL
#define NACK_FILE_COOLDOWN_MS 500ULL
#define USAGE "Usage: %s [-d output_dir]\n"

static volatile sig_atomic_t keep_running = 1;

static void handle_sigint(int signum) {
  (void)signum;
  keep_running = 0;
}

static void
send_missing_chunk_nacks(FileTracker *tracker, MCast *mcast, uint64_t now_ms) {
  if (!tracker || !mcast) {
    return;
  }

  filetracker_maybe_expire_wait(tracker);
  if (filetracker_is_waiting(tracker) || is_file_complete(tracker)) {
    return;
  }
  if (now_ms < tracker->last_data_ms + NACK_IDLE_GAP_MS) {
    return;
  }
  if (now_ms < tracker->next_nack_at_ms) {
    return;
  }

  // Build NACK packets by ranges, iterate through total chunks and create 1
  // NACk packet per range
  uint32_t range_start = 0;
  int in_missing_range = 0;
  int sent_any_nack = 0;

  for (uint32_t seq = 0; seq < tracker->total_chunks; seq++) {
    int received = filetracker_is_chunk_received(tracker, seq);

    if (!received && !in_missing_range) {
      range_start = seq;
      in_missing_range = 1;
      continue;
    }

    // Encountered a received chunk, end range here and make nack packet
    if (received && in_missing_range) {
      NackPacket nack = {0};
      nack.header.type = PKT_TYPE_NACK;
      nack.header.payload_size =
          (uint32_t)(sizeof(NackPacket) - sizeof(PacketHeader));
      nack.file_id = (uint16_t)tracker->file_id;
      nack.missing_seq_num_start = range_start;
      nack.missing_seq_num_end = seq - 1;

      log_info("Sending NACK: file_id=%u, missing_seq_range=[%u,%u]\n",
               nack.file_id,
               nack.missing_seq_num_start,
               nack.missing_seq_num_end);
      multicast_send(mcast, &nack, sizeof(NackPacket));
      sent_any_nack = 1;

      in_missing_range = 0;
    }
  }

  // Last packet
  if (in_missing_range) {
    NackPacket nack = {0};
    nack.header.type = PKT_TYPE_NACK;
    nack.header.payload_size =
        (uint32_t)(sizeof(NackPacket) - sizeof(PacketHeader));
    nack.file_id = (uint16_t)tracker->file_id;
    nack.missing_seq_num_start = range_start;
    nack.missing_seq_num_end = tracker->total_chunks - 1;

    log_info("Sending NACK: file_id=%u, missing_seq_range=[%u,%u]\n",
             nack.file_id,
             nack.missing_seq_num_start,
             nack.missing_seq_num_end);
    multicast_send(mcast, &nack, sizeof(NackPacket));
    sent_any_nack = 1;
  }

  if (sent_any_nack) {
    tracker->next_nack_at_ms = now_ms + NACK_FILE_COOLDOWN_MS;
  }
}

static void handle_metadata_packet(const unsigned char *buf,
                                   int received,
                                   DList *filetrackers,
                                   MCast *mcast) {
  if (received < (int)sizeof(MetadataPacket)) {
    printf("  metadata packet too short (%d bytes)\n", received);
    return;
  }

  const MetadataPacket *pkt = (const MetadataPacket *)buf;
  printf("  metadata: file_id=%u, file_size=%u, chunks=%u, chunk_size=%u, "
         "file_checksum=%u, filename=%s\n",
         pkt->file_id,
         pkt->file_size,
         pkt->num_chunks,
         pkt->chunk_size,
         pkt->file_checksum,
         pkt->filename);

  // Check file trackers, if does not exist create it
  uint32_t file_id = pkt->file_id;
  int alreadyTracking =
      dlist_contains(filetrackers, &file_id, filetracker_cmp_by_id);

  if (alreadyTracking) {
    DListNode *tracker_node =
        dlist_find(filetrackers, &file_id, filetracker_cmp_by_id);
    if (tracker_node) {
      FileTracker *tracker = (FileTracker *)tracker_node->data;
      send_missing_chunk_nacks(tracker, mcast, filetracker_now_ms());
    }
    return;
  }

  log_info("Creating tracker for file id=%u, name=%s\n",
           pkt->file_id,
           pkt->filename);

  FileTracker *filetracker = filetracker_init(pkt->file_id,
                                              pkt->filename,
                                              pkt->num_chunks,
                                              pkt->file_size,
                                              pkt->chunk_size,
                                              pkt->file_checksum);
  if (!filetracker) {
    log_info("Failed to create tracker for file id=%u\n", pkt->file_id);
    return;
  }

  if (!dlist_push_front(filetrackers, (void *)filetracker)) {
    log_info("Failed to insert tracker for file id=%u\n", pkt->file_id);
    filetracker_destroy(filetracker);
    return;
  }

  log_debug(
      "Tracker created for file id=%u, name=%s, waiting data until %llu\n",
      pkt->file_id,
      pkt->filename,
      filetracker->wait_until_ms);
  send_missing_chunk_nacks(filetracker, mcast, filetracker_now_ms());
}

static void handle_data_packet(const unsigned char *buf,
                               int received,
                               DList *filetrackers) {
  if (received < (int)sizeof(DataPacket)) {
    printf("  data packet too short (%d bytes)\n", received);
    return;
  }

  const DataPacket *pkt = (const DataPacket *)buf;
  size_t header_size = offsetof(DataPacket, payload);
  if (received < (int)header_size) {
    log_info("  malformed data packet: shorter than header (%d bytes)\n",
             received);
    return;
  }
  size_t payload_len = (size_t)received - header_size;
  if ((uint32_t)payload_len != pkt->header.payload_size) {
    log_info(
        "  malformed data packet: payload mismatch (got=%zu expected=%u)\n",
        payload_len,
        pkt->header.payload_size);
    return;
  }

  int checksum_ok = checksum_decode(pkt, payload_len);

  printf("  data: file_id=%u, seq=%u, checksum=%s(%u)\n",
         pkt->file_id,
         pkt->seq_num,
         checksum_ok ? "VALID" : "INVALID",
         pkt->checksum);

  uint32_t lookup_file_id = pkt->file_id;
  DListNode *tracker_node =
      dlist_find(filetrackers, &lookup_file_id, filetracker_cmp_by_id);
  log_debug("tracker lookup: target_file_id=%u %s\n",
            lookup_file_id,
            dlist_tostring(filetrackers));
  if (!tracker_node) {
    log_debug("Dropping data packet: no tracker for file_id=%u\n",
              pkt->file_id);
    return;
  }

  FileTracker *tracker = (FileTracker *)tracker_node->data;
  log_debug("tracker found: %s\n", filetracker_tostring(tracker));
  filetracker_maybe_expire_wait(tracker);

  if (!checksum_ok) {
    log_info("Dropping data packet: Invalid checksum.\n");

    return;
  }

  if (pkt->seq_num >= tracker->total_chunks) {
    log_info("Dropping data packet: seq=%u out of range for file_id=%u\n",
             pkt->seq_num,
             pkt->file_id);
    return;
  }

  if (filetracker_is_chunk_received(tracker, pkt->seq_num)) {
    return;
  }

  if (!filetracker_write_chunk(
          tracker, pkt->seq_num, pkt->payload, pkt->header.payload_size)) {
    log_info("Failed to write chunk for file_id=%u seq=%u\n",
             pkt->file_id,
             pkt->seq_num);
    return;
  }

  uint32_t chunk_checksum =
      checksum_encode((const unsigned char *)pkt->payload,
                      pkt->header.payload_size);
  tracker->computed_file_checksum =
      checksum_combine(tracker->computed_file_checksum,
                       chunk_checksum,
                       pkt->seq_num,
                       pkt->header.payload_size);
  mark_chunk_received(tracker, pkt->seq_num);
  tracker->last_data_ms = filetracker_now_ms();

  if (is_file_complete(tracker)) {
    if (tracker->computed_file_checksum != tracker->expected_file_checksum) {
      // If file level validation wrong, re request file

      uint32_t file_id = tracker->file_id;
      uint32_t total_chunks = tracker->total_chunks;
      uint32_t file_size = tracker->file_size;
      uint32_t chunk_size = tracker->chunk_size;
      uint32_t expected_checksum = tracker->expected_file_checksum;
      char filename[MAX_FILENAME_LENGTH];
      strncpy(filename, tracker->filename, sizeof(filename) - 1);
      filename[sizeof(filename) - 1] = '\0';

      log_info("File checksum mismatch: file_id=%u name=%s expected=%u "
               "computed=%u. Reinitializing tracker.\n",
               file_id,
               filename,
               expected_checksum,
               tracker->computed_file_checksum);

      filetracker_destroy(tracker);

      FileTracker *replacement = filetracker_init(file_id,
                                                  filename,
                                                  total_chunks,
                                                  file_size,
                                                  chunk_size,
                                                  expected_checksum);
      if (!replacement) {
        log_info("Failed to reinitialize tracker after checksum mismatch, "
                 "removing tracker for file_id=%u\n",
                 file_id);
        tracker_node->data = NULL;
        (void)dlist_remove_node(filetrackers, tracker_node);
        return;
      }

      tracker_node->data = replacement;
      return;
    }

    // Ensure on-disk content is durable and visible before declaring complete.
    if (fflush(tracker->fp) != 0) {
      log_info("Failed to flush completed file: file_id=%u name=%s\n",
               tracker->file_id,
               tracker->filename);
      return;
    }
    if (fsync(fileno(tracker->fp)) != 0) {
      log_info("Failed to fsync completed file: file_id=%u name=%s\n",
               tracker->file_id,
               tracker->filename);
      return;
    }

    log_info("File complete: file_id=%u name=%s\n",
             tracker->file_id,
             tracker->filename);
  }
}

static void handle_packet(const unsigned char *buf,
                          int received,
                          DList *filetrackers,
                          MCast *mcast) {
  const PacketHeader *hdr = (const PacketHeader *)buf;
  switch (hdr->type) {
  case PKT_TYPE_METADATA:
    handle_metadata_packet(buf, received, filetrackers, mcast);
    break;
  case PKT_TYPE_DATA:
    handle_data_packet(buf, received, filetrackers);
    break;
  default:
    printf("  no handler for packet type=%d\n", hdr->type);
    break;
  }
}

static void filetracker_free_void(void *data) {
  filetracker_destroy((FileTracker *)data);
}

int main(int argc, char *argv[]) {
  signal(SIGINT, handle_sigint);

  const char *output_dir = RECEIVED_FILES_DIR;
  int opt;
  while ((opt = getopt(argc, argv, "d:")) != -1) {
    switch (opt) {
    case 'd':
      output_dir = optarg;
      break;
    default:
      fprintf(stderr, USAGE, argv[0]);
      return 1;
    }
  }

  if (optind < argc) {
    fprintf(stderr, USAGE, argv[0]);
    return 1;
  }

  unsigned char buf[RECV_BUF_SIZE];
  filetracker_set_output_dir(output_dir);

  if (mkdir(output_dir, 0755) != 0 && errno != EEXIST) {
    fprintf(stderr, "mkdir %s failed: %s\n", output_dir, strerror(errno));
    return 1;
  }

  DList *filetrackers = dlist_create(filetracker_free_void);

  MCast *mcast = multicast_init(MCAST_ADDR, SENDER_PORT, RECEIVER_PORT);
  multicast_setup_recv(mcast);

  log_info("Receiver listening on %s:%d\n", MCAST_ADDR, RECEIVER_PORT);
  log_info("Saving received files to: %s\n", output_dir);

  while (keep_running) {
    int ready = multicast_check_receive(mcast);

    if (ready < 0) {
      if (!keep_running)
        break;
      continue;
    }

    if (ready == 0) {
      continue;
    }

    int received = multicast_receive(mcast, buf, sizeof(buf));
    if (received < 0) {
      if (!keep_running)
        break;
      continue;
    }

    if (received < (int)sizeof(PacketHeader)) {
      log_info("Received short packet (%d bytes)\n", received);
      continue;
    }

    PacketHeader *hdr = (PacketHeader *)buf;
    log_info("Received packet: type=%s (%d), bytes=%d, payload_size=%u\n",
             packet_type_to_string(hdr->type),
             hdr->type,
             received,
             hdr->payload_size);

    handle_packet(buf, received, filetrackers, mcast);

    fflush(stdout);
  }

  log_info("Receiver shutting down.\n");
  dlist_destroy(filetrackers);
  multicast_destroy(mcast);
  return 0;
}
