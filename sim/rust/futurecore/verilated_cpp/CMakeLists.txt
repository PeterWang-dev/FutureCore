cmake_minimum_required(VERSION 3.12)
cmake_policy(SET CMP0074 NEW)

project(verilated_cpp)

find_package(verilator HINTS $ENV{VERILATOR_ROOT} ${VERILATOR_ROOT})
if(NOT verilator_FOUND)
    message(FATAL_ERROR "Verilator was not found. Either install it, or set the VERILATOR_ROOT environment variable")
endif()

set(TOP_MODULE FutureCore)
set(PREFIX V${TOP_MODULE})
set(VERILATOR_OUTPUT_DIR "${CMAKE_BINARY_DIR}/${PREFIX}.dir")

add_library(verilated_cpp STATIC wrapper.cpp)

set_target_properties(verilated_cpp PROPERTIES
    CXX_STANDARD 20
    CXX_STANDARD_REQUIRED YES
    CXX_EXTENSIONS OFF
)

file(GLOB VERILOG_SOURCES "${CMAKE_CURRENT_SOURCE_DIR}/vsrc/*.sv")

verilate(verilated_cpp
    SOURCES ${VERILOG_SOURCES}
    PREFIX ${PREFIX}
    TOP_MODULE ${TOP_MODULE}
    TRACE_FST
    OPT_FAST
    DIRECTORY "${VERILATOR_OUTPUT_DIR}"
)

install(TARGETS verilated_cpp DESTINATION ".")
install(FILES wrapper.h DESTINATION ".")
install(DIRECTORY ${VERILATOR_OUTPUT_DIR} DESTINATION ".")