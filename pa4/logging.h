#ifndef LOGGING_H
#define LOGGING_H

#include <stdio.h>
#include <time.h>

#define log_info(...)                                                          \
  do {                                                                         \
    time_t now = time(NULL);                                                   \
    struct tm *t = localtime(&now);                                            \
    printf("[%02d:%02d:%02d] ", t->tm_hour, t->tm_min, t->tm_sec);             \
    printf(__VA_ARGS__);                                                       \
  } while (0)

#endif
