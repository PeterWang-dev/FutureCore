cmake_minimum_required(VERSION 3.12)
cmake_policy(SET CMP0074 NEW)
project(verilated_cpp)
set(CMAKE_CXX_STANDARD 20)
set(TOP_MODULE FutureCore)
add_compile_options(-O3)

find_package(verilator HINTS $ENV{VERILATOR_ROOT} ${VERILATOR_ROOT})

if(NOT verilator_FOUND)
    message(FATAL_ERROR "Verilator was not found. Either install it, or set the VERILATOR_ROOT environment variable")
endif()

add_library(verilated_cpp STATIC wrapper.cpp)

set(PREFIX V${TOP_MODULE})
set(VERILATOR_OUTPUT_DIR "${CMAKE_BINARY_DIR}/${PREFIX}.dir")

file(GLOB VERILOG_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/vsrc/*.sv")

verilate(verilated_cpp TRACE_FST
    VERILATOR_ARGS -x-assign fast
    SOURCES ${VERILOG_SOURCES}
    TOP_MODULE ${TOP_MODULE}
    PREFIX ${PREFIX}
    DIRECTORY "${VERILATOR_OUTPUT_DIR}")

install(TARGETS verilated_cpp DESTINATION ".")
install(FILES wrapper.h DESTINATION ".")
install(DIRECTORY ${VERILATOR_OUTPUT_DIR} DESTINATION ".")