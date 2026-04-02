#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include "protocol.h"
#define DEFAULT_CHUNK_SIZE 1024


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
            default: // Handles unknown flags or missing arguments
                fprintf(stderr, "Usage: %s [-c chunk_size] <file1> <file2> ...\n", argv[0]);
                exit(EXIT_FAILURE);
        }
    }

    int forgotFiles = optind >= argc;
    if (forgotFiles) {
        fprintf(stderr, "Error: At least one file must be specified.\n");
        fprintf(stderr, "Usage: %s [-c chunk_size] <file1> <file2> ...\n", argv[0]);
        exit(EXIT_FAILURE);
    }

    int num_files = argc - optind; 
        printf("Total number of files to send: %d\n", num_files);


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

            fseek(fp, 0, SEEK_END);      // Jump your cursor to the very end of the file
            long file_size = ftell(fp);  // Ask the OS what byte the cursor is on
            fclose(fp);                  // Close it! We don't need the actual data yet.

            file_catalog[file_index].header.type = PKT_TYPE_METADATA;
            file_catalog[file_index].file_id = file_index;
            file_catalog[file_index].file_size = file_size;

            file_catalog[file_index].num_chunks = (file_size + chunk_size - 1) / chunk_size;

            strncpy(file_catalog[file_index].filename , filename, MAX_FILENAME_LENGTH - 1);
            file_catalog[file_index].filename[MAX_FILENAME_LENGTH - 1] = '\0'; // Guarantee null-termination
    }


    return 0;
}
