// Compatibility header for OpenBSD/FreeBSD
#ifndef _BSD_COMPAT_MALLOC_H_
#define _BSD_COMPAT_MALLOC_H_

#include <stdlib.h>

#if defined(__FreeBSD__) || defined(PLATFORM_FBSD)
#include <malloc_np.h>
#endif

#endif // _BSD_COMPAT_MALLOC_H_
