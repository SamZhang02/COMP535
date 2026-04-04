#ifndef FILE_TRACKER_H
#define FILE_TRACKER_H

#include "protocol.h"
#include <stdint.h>
#include <stdio.h>

#define RECEIVED_FILES_DIR "received_files"

typedef struct {
  uint32_t file_id;
  char filename[MAX_FILENAME_LENGTH];
  uint32_t file_size;
  uint32_t total_chunks;
  uint32_t chunk_size;
  uint32_t chunks_received;
  uint8_t *bitmap; // Array of bits: 1 = received, 0 = missing
  FILE *fp;

  // We want to backoff first when we get new file metadata to avoid nack
  // implosion with other receivers
  uint8_t wait_data;
  uint64_t wait_until_ms;
  uint64_t last_data_ms;
  uint64_t next_nack_at_ms;
} FileTracker;

FileTracker *filetracker_init(uint32_t file_id,
                              const char *filename,
                              uint32_t total_chunks,
                              uint32_t file_size,
                              uint32_t chunk_size);
void mark_chunk_received(FileTracker *ft, uint32_t chunk_index);
int is_file_complete(FileTracker *ft);
void filetracker_destroy(FileTracker *ft);
int filetracker_cmp_by_id(const void *lhs, const void *rhs);
int filetracker_is_chunk_received(const FileTracker *ft, uint32_t chunk_index);
int filetracker_write_chunk(FileTracker *ft,
                            uint32_t chunk_index,
                            const char *payload,
                            uint32_t payload_len);
const char *filetracker_tostring(const FileTracker *ft);
uint64_t filetracker_now_ms(void);
int filetracker_is_waiting(const FileTracker *ft);
void filetracker_maybe_expire_wait(FileTracker *ft);

#endif
