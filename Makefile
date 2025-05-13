PRJ = futurecore

GEN_DIR = $(abspath hw/gen)
SIM_DIR = $(abspath sim/rust)
ARGS ?=
IMG ?=

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(GEN_DIR)
	TARGET_DIR=$(GEN_DIR) mill -i $(PRJ).runMain $(PRJ).Elaborate

sim:
	$(call git_commit, "sim RTL")
	make -C $(SIM_DIR) run ARGS="$(ARGS)" IMG=$(IMG)

test:
	$(call git_commit, "test RTL")
	mill -i __.test

formal:
	$(call git_commit, "verify RTL")
	mill -i $(PRJ).runMain $(PRJ).Verify

help:
	mill -i $(PRJ).runMain Elaborate --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

bsp:
	mill -i mill.bsp.BSP/install

idea:
	mill -i mill.idea.GenIdea/idea

clean:
	@make -C $(SIM_DIR) clean
	-rm $(GEN_DIR)/*

clean-verilog:
	-rm $(GEN_DIR)/*

clean-all: clean
	@make -C $(SIM_DIR) clean-all
	-rm -r out simWorkspace .bsp .bloop .metals .idea

.PHONY: sim test verilog help reformat checkformat clean clean-verilog clean-all

-include ../Makefile
