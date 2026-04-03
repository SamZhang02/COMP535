#include "file_tracker.h"
#include <stdint.h>
#include <stdlib.h>

// Bitmap operations to save memory
#define SET_BIT(A, k) (A[(k / 8)] |= (1 << (k % 8)))
#define CLEAR_BIT(A, k) (A[(k / 8)] &= ~(1 << (k % 8)))
#define TEST_BIT(A, k) (A[(k / 8)] & (1 << (k % 8)))

FileTracker *filetracker_init(uint32_t file_id,
                              char *filename,
                              uint32_t total_chunks,
                              FILE *fp) {

  FileTracker *ft = calloc(1, sizeof(FileTracker));

  ft->file_id = file_id;
  ft->total_chunks = total_chunks;
  ft->chunks_received = 0;
  ft->fp = fp;
  size_t num_bytes = (total_chunks + 7) / 8;
  ft->bitmap = calloc(1, num_bytes);
  snprintf(ft->filename, sizeof(ft->filename), "%s", filename);

  return ft;
}

void mark_chunk_received(FileTracker *ft, uint32_t chunk_index) {
  if (chunk_index >= ft->total_chunks)
    return;

  if (!TEST_BIT(ft->bitmap, chunk_index)) {
    SET_BIT(ft->bitmap, chunk_index);
    ft->chunks_received++;
  }
}

int is_file_complete(FileTracker *ft) {
  return ft->chunks_received == ft->total_chunks;
}

void filetracker_destroy(FileTracker *ft) {
  if (!ft)
    return;

  if (ft->bitmap) {
    free(ft->bitmap);
  }

  if (ft->fp) {
    fclose(ft->fp);
  }

  free(ft);
}
