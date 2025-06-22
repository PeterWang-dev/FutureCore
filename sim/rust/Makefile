ARGS ?=
IMG ?=

# Clear any existing variables
CC=gcc
CXX=g++
CFLAGS=
CXXFLAGS=
LDFLAGS=

.DEFAULT_GOAL = default
default: build

all: build run

build:
	@cargo build --release

run:
	@cargo run --release -- --diff $(NEMU_HOME)/tools/spike-diff/build/riscv32-spike-so $(ARGS) $(IMG)

run-wave:
	@cargo run --release -- --diff $(NEMU_HOME)/tools/spike-diff/build/riscv32-spike-so --trace trace.fst $(ARGS) $(IMG)

clean:
	@cargo clean

clean-all: clean
	-rm *.fst

.PHONY: default all build run run-wave clean clean-all
