#!/bin/sh
set -e

# Native OpenBSD Build Script for Source Engine (Wayland-Only + Mesa/Zink + sndio)
echo "=== Configuring Source Engine for OpenBSD (Wayland-Only + Mesa/Zink) ==="

export CC=${CC:-cc}
export CXX=${CXX:-c++}
export CFLAGS="${CFLAGS} -I/usr/local/include -I/usr/X11R6/include"
export CXXFLAGS="${CXXFLAGS} -I/usr/local/include -I/usr/X11R6/include"
export LDFLAGS="${LDFLAGS} -L/usr/local/lib -L/usr/X11R6/lib -Wl,-z,wxneeded"

python3 ./waf configure -T release --wayland-only --use-mesa "$@"
python3 ./waf build -v
