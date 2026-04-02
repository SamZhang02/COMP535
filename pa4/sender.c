#include "checksum.h"
#include "multicast.h"
#include "protocol.h"
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define USAGE "Usage: %s [-c chunk_size] <file1> <file2> ...\n"
#define DEFAULT_CHUNK_SIZE 1024

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

int main(int argc, char *argv[]) {
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

    strncpy(
        file_catalog[file_index].filename, filename, MAX_FILENAME_LENGTH - 1);
    file_catalog[file_index].filename[MAX_FILENAME_LENGTH - 1] = '\0';
  }

  MCast *mcast = multicast_init("239.0.0.1", 5000, 5000);

  for (int i = 0; i < num_files; i++) {
    multicast_send(mcast, &file_catalog[i], sizeof(MetadataPacket));
  }

  for (int i = 0; i < num_files; i++) {
    MetadataPacket fileMetadata = file_catalog[i];
    for (uint32_t j = 0; j < fileMetadata.num_chunks; j++) {
      DataPacket *data_packet = generate_data_packet(
          file_catalog, fileMetadata.file_id, j, chunk_size);
      size_t size = sizeof(DataPacket) + data_packet->header.payload_size;

      printf("Sending data packet, file_id=%u, seq_num=%u, "
             "payload_size=%zubytes\n",
             data_packet->file_id,
             data_packet->seq_num,
             size);

      multicast_send(mcast, data_packet, size);

      free(data_packet);
    }
  }

  multicast_destroy(mcast);
  return 0;
}
