#!/usr/bin/env bash
set -e

# ---------------------------------------------------------
# ENV
# ---------------------------------------------------------

# Bare-metal AArch64 toolchain. Override with KP_TOOLCHAIN=<dir> where
# <dir>/bin/aarch64-none-elf-gcc exists, or place it in PATH.
if [[ -n "${KP_TOOLCHAIN}" ]]; then
    export PATH="${KP_TOOLCHAIN}/bin:${PATH}"
fi
export TARGET_COMPILE=aarch64-none-elf-

# Android NDK root. Override with ANDROID_NDK=<path>.
if [[ -z "${ANDROID_NDK}" ]] && [[ -n "${ANDROID_NDK_HOME}" ]]; then
    export ANDROID_NDK="${ANDROID_NDK_HOME}"
fi

# ---------------------------------------------------------
# TIMESTAMP
# ---------------------------------------------------------

BUILD_START_EPOCH=$(date +%s)
BUILD_DATE=$(date +%Y%m%d)
BUILD_TIME=$(date +%H%M%S)

echo "Build started at: $(date '+%Y-%m-%d %H:%M:%S')"

# ---------------------------------------------------------
# VERSION PARSING
# ---------------------------------------------------------

VERSION_FILE=version
REVISION_FILE=revision

MAJOR=$(grep -E '#define[[:space:]]+MAJOR'   "$VERSION_FILE" | awk '{print $3}')
MINOR=$(grep -E '#define[[:space:]]+MINOR'   "$VERSION_FILE" | awk '{print $3}')
PATCH=$(grep -E '#define[[:space:]]+PATCH'   "$VERSION_FILE" | awk '{print $3}')
REVISION=$(grep -E '#define[[:space:]]+REVISION' "$REVISION_FILE" | awk '{print $3}')

if [[ "$REVISION" != "0" ]]; then
    VERSION="${MAJOR}.${MINOR}.${PATCH}-${REVISION}"
else
    VERSION="${MAJOR}.${MINOR}.${PATCH}"
fi

ZIP_NAME="KPatch-Next-${VERSION}-${BUILD_DATE}-${BUILD_TIME}.zip"

echo "Building version: $VERSION"
echo "Output zip: $ZIP_NAME"

# ---------------------------------------------------------
# CLEAN
# ---------------------------------------------------------

echo "Cleaning kernel..."
( cd kernel && make clean )

echo "Cleaning user..."
( cd user && make clean )

echo "Cleaning tools..."
( cd tools && make clean )

# ---------------------------------------------------------
# KPIMG
# ---------------------------------------------------------

echo "Building kernel..."
( cd kernel && make )

# ---------------------------------------------------------
# KPATCH (user)
# ---------------------------------------------------------

echo "Building kpatch (Android)..."
(
  cd user
  mkdir -p build/android
  cd build/android

  cmake -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
        -DCMAKE_BUILD_TYPE=Release \
        -DANDROID_PLATFORM=android-33 \
        -DANDROID_ABI=arm64-v8a ../..

  cmake --build .
)

# ---------------------------------------------------------
# KPTOOLS
# ---------------------------------------------------------

echo "Building kptools (Android)..."
(
  cd tools
  mkdir -p build/android
  cd build/android

  cmake -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK/build/cmake/android.toolchain.cmake" \
        -DCMAKE_BUILD_TYPE=Release \
        -DANDROID_PLATFORM=android-33 \
        -DANDROID_ABI=arm64-v8a ../..

  cmake --build .
)

# ---------------------------------------------------------
# PACK BINS
# ---------------------------------------------------------

echo "Packing binaries..."
(
  rm -rf build
  mkdir -p build

  cp kernel/kpimg build/
  cp user/build/android/kpatch build/
  cp tools/build/android/kptools build/

  cd build
  zip -r "../$ZIP_NAME" .
)

# ---------------------------------------------------------
# CLEAN BINS
# ---------------------------------------------------------

echo "Cleaning bins..."
( rm -rf build )

# ---------------------------------------------------------
# DONE
# ---------------------------------------------------------

BUILD_END_EPOCH=$(date +%s)
BUILD_DURATION=$((BUILD_END_EPOCH - BUILD_START_EPOCH))

echo "Build finished at: $(date '+%Y-%m-%d %H:%M:%S')"
echo "Build duration: ${BUILD_DURATION}s"
echo "Done!"

