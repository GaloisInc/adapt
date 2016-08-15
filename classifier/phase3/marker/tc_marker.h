// tc_marker.h

#include <errno.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <string.h>
#include <netdb.h>
#include <sys/socket.h>
#include <netinet/in.h>

#define TC_MARKER_FSPEC_LEN  40

// Wrap a code fragment with Transparent Computing begin / end markers.

// Evaluate tc_code for side effects.
#define TC_MARK_VOID(tc_marker_num, tc_code) do {                  \
    char tc_fspec[TC_MARKER_FSPEC_LEN];                            \
    TC_MARK1(tc_marker_num, "begin");                              \
    tc_code;                                                       \
    TC_MARK1(tc_marker_num, "end");                                \
  } while (0)

// The target code fragment, tc_int_code, should compute
// an integer result to be returned immediately.
#define TC_MARK_RETURN(tc_marker_num, tc_int_code) do {            \
    char tc_fspec[TC_MARKER_FSPEC_LEN];                            \
    TC_MARK1(tc_marker_num, "begin");                              \
    int tc_ret = tc_int_code;                                      \
    TC_MARK1(tc_marker_num, "end");                                \
    return tc_ret;                                                 \
  } while (0)

#define TC_MARK1(tc_marker_num, tc_begin_end)                      \
    { sprintf(tc_fspec, "/tmp/adapt/tc-marker-%03d-%s.txt",        \
              tc_marker_num, tc_begin_end);                        \
      int tc_flags = O_WRONLY | O_APPEND | O_CREAT;                \
      int tc_fd = open(tc_fspec, tc_flags, 0644);                  \
      int n = write(tc_fd, ".", 1);                                \
      close(tc_fd);                                                \
      struct addrinfo hints;                                       \
      memset(&hints, 0, sizeof(hints));                            \
      hints.ai_family = AF_INET;                                   \
      hints.ai_socktype = SOCK_DGRAM;                              \
      struct addrinfo* res;                                        \
      int s = getaddrinfo("sector6.net", "4000", &hints, &res);    \
      if (s != 0) {                                                \
          perror("lookup failed");                                 \
          printf("err: %d\n", s);                                  \
      }                                                            \
      int fd = socket(AF_INET, SOCK_DGRAM, 0);                     \
      if (fd < 0) {                                                \
          perror("socket");                                        \
      }                                                            \
    }
