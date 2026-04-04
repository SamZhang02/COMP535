#include "file_tracker.h"
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

#define STARTUP_GRACE_MS 100U
#define STARTUP_JITTER_MS 150U

// Bitmap operations to save memory
#define SET_BIT(A, k) (A[(k / 8)] |= (1 << (k % 8)))
#define CLEAR_BIT(A, k) (A[(k / 8)] &= ~(1 << (k % 8)))
#define TEST_BIT(A, k) (A[(k / 8)] & (1 << (k % 8)))

uint64_t filetracker_now_ms(void) {
  struct timespec ts;
  clock_gettime(CLOCK_MONOTONIC, &ts);
  return (uint64_t)ts.tv_sec * 1000ULL + (uint64_t)ts.tv_nsec / 1000000ULL;
}

FileTracker *filetracker_init(uint32_t file_id,
                              const char *filename,
                              uint32_t total_chunks,
                              uint32_t file_size,
                              uint32_t chunk_size) {
  static int rand_seeded = 0;
  if (!rand_seeded) {
    srand((unsigned int)time(NULL));
    rand_seeded = 1;
  }

  FileTracker *ft = calloc(1, sizeof(FileTracker));
  if (!ft)
    return NULL;

  ft->file_id = file_id;
  ft->file_size = file_size;
  ft->total_chunks = total_chunks;
  ft->chunk_size = chunk_size;
  ft->chunks_received = 0;
  size_t num_bytes = (total_chunks + 7) / 8;
  ft->bitmap = NULL;
  if (num_bytes > 0) {
    ft->bitmap = calloc(1, num_bytes);
    if (!ft->bitmap) {
      free(ft);
      return NULL;
    }
  }
  const char *base = filename;
  const char *slash = NULL;
  for (const char *p = filename; *p; ++p) {
    if (*p == '/')
      slash = p;
  }
  if (slash && *(slash + 1) != '\0')
    base = slash + 1;

  snprintf(
      ft->filename, sizeof(ft->filename), "%s/%s", RECEIVED_FILES_DIR, base);
  ft->fp = fopen(ft->filename, "wb+");
  if (!ft->fp) {
    free(ft->bitmap);
    free(ft);
    return NULL;
  }
  if (ftruncate(fileno(ft->fp), (off_t)file_size) != 0) {
    fclose(ft->fp);
    free(ft->bitmap);
    free(ft);
    return NULL;
  }

  ft->wait_data = 1;
  ft->wait_until_ms = filetracker_now_ms() + STARTUP_GRACE_MS +
                      (uint64_t)(rand() % (STARTUP_JITTER_MS + 1));

  return ft;
}

void mark_chunk_received(FileTracker *ft, uint32_t chunk_index) {
  if (!ft || chunk_index >= ft->total_chunks || !ft->bitmap)
    return;

  if (!TEST_BIT(ft->bitmap, chunk_index)) {
    SET_BIT(ft->bitmap, chunk_index);
    ft->chunks_received++;
  }
}

int filetracker_is_chunk_received(const FileTracker *ft, uint32_t chunk_index) {
  if (!ft || !ft->bitmap || chunk_index >= ft->total_chunks)
    return 0;
  return TEST_BIT(ft->bitmap, chunk_index) != 0;
}

int filetracker_write_chunk(FileTracker *ft,
                            uint32_t chunk_index,
                            const char *payload,
                            uint32_t payload_len) {
  if (!ft || !ft->fp || !payload)
    return 0;
  if (chunk_index >= ft->total_chunks)
    return 0;
  if (payload_len > ft->chunk_size)
    return 0;

  uint64_t offset = (uint64_t)chunk_index * (uint64_t)ft->chunk_size;
  if (offset + payload_len > ft->file_size)
    return 0;

  if (fseek(ft->fp, (long)offset, SEEK_SET) != 0)
    return 0;

  if (payload_len > 0 && fwrite(payload, 1, payload_len, ft->fp) != payload_len)
    return 0;

  return 1;
}

int is_file_complete(FileTracker *ft) {
  return ft->chunks_received == ft->total_chunks;
}

int filetracker_is_waiting(const FileTracker *ft) {
  if (!ft)
    return 0;
  return ft->wait_data != 0;
}

void filetracker_maybe_expire_wait(FileTracker *ft) {
  if (!ft || !ft->wait_data)
    return;

  if (filetracker_now_ms() >= ft->wait_until_ms)
    ft->wait_data = 0;
}

void filetracker_on_first_data(FileTracker *ft) {
  if (!ft)
    return;
  ft->wait_data = 0;
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

int filetracker_cmp_by_id(const void *lhs, const void *rhs) {
  const FileTracker *ft = (const FileTracker *)lhs;
  uint32_t key_file_id = *(const uint32_t *)rhs;

  if (ft->file_id == key_file_id)
    return 0;
  return (ft->file_id < key_file_id) ? -1 : 1;
}
