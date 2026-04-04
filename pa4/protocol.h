#ifndef PROTOCOL_H
#define PROTOCOL_H

#include <stdint.h>
#define MAX_FILENAME_LENGTH 256

typedef uint8_t PacketType;

#define PKT_TYPE_METADATA 0
#define PKT_TYPE_DATA 1
#define PKT_TYPE_NACK 2

const char *packet_type_to_string(PacketType type);

typedef struct __attribute__((packed)) {
  uint32_t payload_size; // Size of the payload ONLY
  PacketType type;
} PacketHeader;

typedef struct __attribute__((packed)) {
  PacketHeader header; // type = PKT_TYPE_METADATA
  uint32_t file_id;
  uint32_t file_size;
  uint32_t num_chunks;
  uint32_t chunk_size;
  char filename[MAX_FILENAME_LENGTH];
} MetadataPacket;

typedef struct __attribute__((packed)) {
  PacketHeader header; // type = PKT_TYPE_DATA
  uint16_t file_id;
  uint32_t seq_num; // Chunk id
  unsigned char checksum;

  // size depends on -c flag, malloc sizeof(DataPacket + chunk_size)
  char payload[];
} DataPacket;

// NackPacket requests a range of missing chunks, inclusive
typedef struct __attribute__((packed)) {
  PacketHeader header; // type = PKT_TYPE_NACK
  uint16_t file_id;
  uint32_t missing_seq_num_start;
  uint32_t missing_seq_num_end;
} NackPacket;

#endif
