# Basic Info (Required)
TOPNAME = FutureCore
NXDC_FILES = constr/top.nxdc

# Outputs
BUILD_DIR = $(CURDIR)/build
OBJ_DIR = $(BUILD_DIR)/obj_dir
SIM_BIN = $(BUILD_DIR)/$(TOPNAME)-sim
BIN = $(BUILD_DIR)/$(TOPNAME)
BIN_WAVE = $(BUILD_DIR)/$(TOPNAME)-wave
ARGS ?=
IMG ?=

$(shell mkdir -p $(BUILD_DIR))

# Verilator configs
VERILATOR = verilator
# Generate C++ in executable form
VERILATOR_FLAGS += -cc --exe
# Generate makefile dependencies (not shown as complicates the Makefile)
VERILATOR_FLAGS += -MMD
# Optimize
VERILATOR_FLAGS += -x-assign fast
# Warn abount lint issues; may not want this on less solid designs
# VERILATOR_FLAGS += -Wall
# Make waveforms (Simulation only)
$(BIN_WAVE): VERILATOR_FLAGS += --trace-fst
# Check SystemVerilog assertions
# VERILATOR_FLAGS += --assert
# Generate coverage analysis (Simulation only)
$(BIN_WAVE): VERILATOR_FLAGS += --coverage
# Run Verilator in debug mode
# VERILATOR_FLAGS += --debug
# Add this trace in gdb
# VERILATOR_FLAGS += --gdb
# Add this trace to get a backtrace in gdb
# VERILATOR_FLAGS += --gdbbt
# Specifies the name of the Make object directory
VERILATOR_FLAGS += --Mdir $(OBJ_DIR)
# Set toplevel module
VERILATOR_FLAGS += --top-module $(TOPNAME)

.DEFAULT_GOAL = default
default: build

# Project source
VSRCS = $(shell find $(realpath ./vsrc) -name "*.v" -or -name "*.sv") # ./vsrc is a symlink pointing at generated verilog files
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
# NVBOARD_CSRCS += $(CSRCS) $(shell find $(abspath ./csrc) -name "main.*") $(SRC_AUTO_BIND)

# C compiler argument
INC_PATH += $(abspath ./csrc/include)
INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="V$(TOPNAME)"
VERILATOR_FLAGS += $(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS))

# include $(NVBOARD_HOME)/scripts/nvboard.mk

# Constraint file
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

# $(NVBOARD_BIN): $(VSRCS) $(RES_SRC) $(NVBOARD_CSRCS) $(NVBOARD_ARCHIVE)
# 	$(VERILATOR) $(VERILATOR_FLAGS) --build -o $(abspath $@) $(VSRCS) $(NVBOARD_CSRCS) $(NVBOARD_ARCHIVE)

# $(NVBOARD_BIN_WAVE): $(VSRCS) $(RES_SRC) $(NVBOARD_CSRCS) $(NVBOARD_ARCHIVE)
# 	$(VERILATOR) $(VERILATOR_FLAGS) --build -o $(abspath $@) $(VSRCS) $(NVBOARD_CSRCS) $(NVBOARD_ARCHIVE)

$(BIN): $(VSRCS) $(CSRCS)
	$(VERILATOR) $(VERILATOR_FLAGS) --build -o $(abspath $@) $(VSRCS) $(CSRCS)

$(BIN_WAVE): $(VSRCS) $(CSRCS)
	$(VERILATOR) $(VERILATOR_FLAGS) --build -o $(abspath $@) $(VSRCS) $(CSRCS)

all: default

build: $(BIN)

build-wave: $(BIN_WAVE)

run: $(BIN)
	@$^ $(ARGS) $(IMG)

run-wave: $(BIN_WAVE)
	@$^ $(ARGS) $(IMG)

clean:
	-rm -rf $(BUILD_DIR)

clean-all:
	-rm -rf $(BUILD_DIR) logs *.log *.dmp *.vpd coverage.dat core

.PHONY: default all build build-wave run run-wave clean clean-all
