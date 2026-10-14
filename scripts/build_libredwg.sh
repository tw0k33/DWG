#!/usr/bin/env bash
# Собирает LibreDWG как статическую библиотеку под Android ABI.
# Нужны: Android NDK (ANDROID_NDK_HOME), curl, make, стандартные autotools-зависимости.
set -euo pipefail

: "${ANDROID_NDK_HOME:?Задайте ANDROID_NDK_HOME}"
VER=0.13.3
API=24
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TOOLCHAIN="$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"   # на macOS: darwin-x86_64

mkdir -p "$ROOT/.build" && cd "$ROOT/.build"
[ -f libredwg-$VER.tar.xz ] || curl -LO https://ftp.gnu.org/gnu/libredwg/libredwg-$VER.tar.xz

build() {
  local abi=$1 triple=$2
  rm -rf "src-$abi" && mkdir "src-$abi"
  tar xf libredwg-$VER.tar.xz -C "src-$abi" --strip-components=1
  ( cd "src-$abi"
    export CC="$TOOLCHAIN/bin/${triple}${API}-clang"
    export AR="$TOOLCHAIN/bin/llvm-ar" RANLIB="$TOOLCHAIN/bin/llvm-ranlib"
    ./configure --host="$triple" --prefix="$ROOT/libredwg-prebuilt/$abi" \
      --disable-shared --enable-static --disable-bindings --disable-docs --disable-werror
    make -C src -j"$(nproc)" libredwg.la
    mkdir -p "$ROOT/libredwg-prebuilt/$abi/lib" "$ROOT/libredwg-prebuilt/$abi/include"
    cp src/.libs/libredwg.a "$ROOT/libredwg-prebuilt/$abi/lib/"
    cp include/dwg.h include/dwg_api.h "$ROOT/libredwg-prebuilt/$abi/include/"
    # dwg.h ссылается на сгенерированные заголовки
    cp src/dwg.h "$ROOT/libredwg-prebuilt/$abi/include/dwg.h" 2>/dev/null || true
    cp src/config.h "$ROOT/libredwg-prebuilt/$abi/include/" 2>/dev/null || true
  )
}

build arm64-v8a aarch64-linux-android
build x86_64    x86_64-linux-android
echo "Готово: $ROOT/libredwg-prebuilt"
