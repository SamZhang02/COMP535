#ifndef PROTOCOL_H
#define PROTOCOL_H

#include <stdint.h>


typedef enum {
    PKT_TYPE_METADATA, // The FDT "Menu" of files
    PKT_TYPE_DATA,     // Actual file chunks
    PKT_TYPE_NACK      // Receiver asking for a missing chunk
} PacketType;

typedef struct {
    PacketType type;
    uint32_t payload_size;
} PacketHeader;

typedef struct {
    PacketHeader header; // type = PKT_TYPE_METADATA
    uint32_t file_id;
    uint32_t file_size;
} MetadataPacket ;

typedef struct {
    PacketHeader header; // type = PKT_TYPE_DATA
    uint16_t file_id;
    uint32_t seq_num; // Chunk id
    uint32_t total_chunks;
    unsigned char checksum; 
    char payload[1024]; // size depends on your -c CLI flag!
} DataPacket;

typedef struct {
    PacketHeader header; // type = PKT_TYPE_NACK
    uint16_t file_id;
    uint32_t missing_seq_num;
} NackPacket;

#endif
