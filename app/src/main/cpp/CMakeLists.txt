cmake_minimum_required(VERSION 3.22)
project(dwgjni CXX)

# Собранный скриптом scripts/build_libredwg.sh: <корень>/libredwg-prebuilt/<ABI>/
set(LIBREDWG_DIR ${CMAKE_SOURCE_DIR}/../../../../libredwg-prebuilt/${ANDROID_ABI})

add_library(redwg STATIC IMPORTED)
set_target_properties(redwg PROPERTIES IMPORTED_LOCATION ${LIBREDWG_DIR}/lib/libredwg.a)

add_library(dwgjni SHARED dwg_jni.cpp)
target_include_directories(dwgjni PRIVATE ${LIBREDWG_DIR}/include)
target_link_libraries(dwgjni redwg log m)
