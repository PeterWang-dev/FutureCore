PRJ = futurecore

GEN_DIR = $(abspath hw/gen)
SIM_DIR = $(abspath sim/verilator)
ARGS ?=
IMG ?=

sim:
	$(call git_commit, "sim RTL")
	make -C $(SIM_DIR) run ARGS="$(ARGS)" IMG=$(IMG)

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(GEN_DIR)
	TARGET_DIR=$(GEN_DIR) mill -i $(PRJ).runMain $(PRJ).Elaborate
	-rm $(SIM_DIR)/vsrc
	ln -s $(GEN_DIR) $(SIM_DIR)/vsrc

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
	-rm -rf $(BUILD_DIR)

clean-verilog:
	-rm $(SIM_DIR)/vsrc
	-rm $(GEN_DIR)/*

.PHONY: test verilog help reformat checkformat clean sim


-include ../Makefile
