#include "file_tracker.h"
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>

#define STARTUP_GRACE_MS 500U
#define STARTUP_JITTER_MS 2500U

static const char *g_output_dir = RECEIVED_FILES_DIR;

// Bitmap operations to save memory
#define SET_BIT(A, k) (A[(k / 8)] |= (1 << (k % 8)))
#define CLEAR_BIT(A, k) (A[(k / 8)] &= ~(1 << (k % 8)))
#define TEST_BIT(A, k) (A[(k / 8)] & (1 << (k % 8)))

uint64_t filetracker_now_ms(void) {
  struct timespec ts;
  clock_gettime(CLOCK_MONOTONIC, &ts);
  return (uint64_t)ts.tv_sec * 1000ULL + (uint64_t)ts.tv_nsec / 1000000ULL;
}

void filetracker_set_output_dir(const char *dir) {
  if (dir && dir[0] != '\0') {
    g_output_dir = dir;
    return;
  }
  g_output_dir = RECEIVED_FILES_DIR;
}

FileTracker *filetracker_init(uint32_t file_id,
                              const char *filename,
                              uint32_t total_chunks,
                              uint32_t file_size,
                              uint32_t chunk_size,
                              uint32_t expected_file_checksum) {
  static int rand_seeded = 0;
  if (!rand_seeded) {
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    unsigned int seed = (unsigned int)time(NULL) ^ (unsigned int)getpid() ^
                        (unsigned int)ts.tv_nsec;
    srand(seed);
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
  ft->expected_file_checksum = expected_file_checksum;
  ft->computed_file_checksum = 0;
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

  snprintf(ft->filename, sizeof(ft->filename), "%s/%s", g_output_dir, base);
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
  uint64_t now_ms = filetracker_now_ms();
  ft->wait_until_ms =
      now_ms + STARTUP_GRACE_MS + (uint64_t)(rand() % (STARTUP_JITTER_MS + 1));
  ft->last_data_ms = now_ms;
  ft->next_nack_at_ms = ft->wait_until_ms;

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

  if (fflush(ft->fp) != 0)
    return 0;

  return 1;
}

const char *filetracker_tostring(const FileTracker *ft) {
  static char buf[320];
  if (!ft) {
    snprintf(buf, sizeof(buf), "FileTracker{null}");
    return buf;
  }

  snprintf(buf,
           sizeof(buf),
           "FileTracker{id=%u, file='%s', size=%u, chunks=%u, got=%u, "
           "chunk_size=%u, expected_checksum=%u, computed_checksum=%u, "
           "wait_data=%u, wait_until_ms=%llu, "
           "last_data_ms=%llu, next_nack_at_ms=%llu, fp=%p}",
           ft->file_id,
           ft->filename,
           ft->file_size,
           ft->total_chunks,
           ft->chunks_received,
           ft->chunk_size,
           ft->expected_file_checksum,
           ft->computed_file_checksum,
           (unsigned int)ft->wait_data,
           (unsigned long long)ft->wait_until_ms,
           (unsigned long long)ft->last_data_ms,
           (unsigned long long)ft->next_nack_at_ms,
           (void *)ft->fp);
  return buf;
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
