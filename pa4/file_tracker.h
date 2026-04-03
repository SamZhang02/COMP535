#ifndef FILE_TRACKER_H
#define FILE_TRACKER_H

#include "protocol.h"
#include <stdint.h>
#include <stdio.h>

typedef struct {
  uint32_t file_id;
  char filename[MAX_FILENAME_LENGTH];
  uint32_t total_chunks;
  uint32_t chunks_received;
  uint8_t *bitmap; // Array of bits: 1 = received, 0 = missing
  FILE *fp;
} FileTracker;

#endif
