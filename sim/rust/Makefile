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
	@cargo run --release -- $(ARGS) $(IMG)

run-wave:
	@cargo run --release -- --trace trace.fst $(ARGS) $(IMG)

clean:
	@cargo clean

clean-all: clean
	-rm *.fst

.PHONY: default all build run run-wave clean clean-all
