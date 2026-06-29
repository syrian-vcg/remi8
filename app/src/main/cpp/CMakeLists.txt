cmake_minimum_required(VERSION 3.22.1)
project("remi8_engine")

add_library(
    remi8_engine
    SHARED
    remiscript_engine.cpp
)

find_library(log-lib log)

target_link_libraries(
    remi8_engine
    ${log-lib}
)

target_compile_options(remi8_engine PRIVATE
    -std=c++17
    -O2
    -Wall
)
