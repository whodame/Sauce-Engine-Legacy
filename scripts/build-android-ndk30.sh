#!/bin/sh
# Build Sauce Engine for Android using Android NDK r30 (or r21+)
# Usage:
#   export ANDROID_NDK_HOME=/path/to/android-ndk-r30
#   ./scripts/build-android-ndk30.sh [aarch64|armeabi-v7a]

ARCH="${1:-aarch64}"
API="${2:-21}"

if [ -z "$ANDROID_NDK_HOME" ] && [ -z "$ANDROID_NDK" ] && [ -z "$ANDROID_NDK_ROOT" ] && [ -z "$NDK_HOME" ]; then
	echo "Error: Set ANDROID_NDK_HOME pointing to your Android NDK r30 directory."
	exit 1
fi

./waf configure -T release --android=${ARCH},clang,${API} --togles --disable-warns &&
./waf build
