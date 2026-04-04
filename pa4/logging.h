#ifndef LOGGING_H
#define LOGGING_H

#include <stdio.h>
#include <time.h>

#define log_with_level(level, ...)                                             \
  do {                                                                         \
    time_t now = time(NULL);                                                   \
    struct tm *t = localtime(&now);                                            \
    printf("[%02d:%02d:%02d] [%s] ", t->tm_hour, t->tm_min, t->tm_sec, level); \
    printf(__VA_ARGS__);                                                       \
  } while (0)

#define log_info(...)                                                          \
  do {                                                                         \
    log_with_level("INFO", __VA_ARGS__);                                       \
  } while (0)

#ifdef DEBUG
#define log_debug(...)                                                         \
  do {                                                                         \
    log_with_level("DEBUG", __VA_ARGS__);                                      \
  } while (0)
#else
#define log_debug(...)                                                         \
  do {                                                                         \
  } while (0)
#endif

#endif
