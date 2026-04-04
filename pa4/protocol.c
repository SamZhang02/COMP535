#include "protocol.h"

const char *packet_type_to_string(PacketType type) {
  switch (type) {
  case PKT_TYPE_METADATA:
    return "METADATA";
  case PKT_TYPE_DATA:
    return "DATA";
  case PKT_TYPE_NACK:
    return "NACK";
  default:
    return "UNKNOWN";
  }
}
