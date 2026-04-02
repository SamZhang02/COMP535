#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main(int argc, char *argv[]) {
    char *output_dir = "received_files"; // Default directory
    int opt;

    // Parse the optional -d flag
    while ((opt = getopt(argc, argv, "d:")) != -1) {
        switch (opt) {
            case 'd':
                output_dir = optarg;
                break;
            default:
                fprintf(stderr, "Usage: %s [-d output_directory]\n", argv[0]);
                exit(EXIT_FAILURE);
        }
    }

    printf("Saving files to: %s/\n", output_dir);

    return 0;
}
