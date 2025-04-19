BUILD_DIR = hw/gen

PRJ = projectname

test:
	$(call git_commit, "test RTL")
	mill -i $(PRJ).runMain $(PRJ).Simulate

formal:
	$(call git_commit, "verify RTL")
	mill -i $(PRJ).runMain $(PRJ).Verify

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	TARGET_DIR=$(BUILD_DIR) mill -i $(PRJ).runMain $(PRJ).Elaborate

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

.PHONY: test verilog help reformat checkformat clean sim

sim:
	$(call git_commit, "sim RTL")
	@echo "Write this Makefile by yourself."

-include ../Makefile
