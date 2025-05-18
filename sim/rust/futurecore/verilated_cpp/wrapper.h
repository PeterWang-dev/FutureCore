#ifndef VERILATED_WRAPPER_H
#define VERILATED_WRAPPER_H

#include "VFutureCore.h"
#include "verilated.h"
#include "verilated_fst_c.h"
#include <cstdint>
#include <memory>

std::unique_ptr<VerilatedContext> make_context();
std::unique_ptr<VerilatedFstC> make_fst_c();
std::unique_ptr<VFutureCore> make_dut();
void trace(VFutureCore &dut, VerilatedFstC *trace, int32_t level, int32_t options);

uint8_t &resetn(VFutureCore &dut);
uint8_t &clk(VFutureCore &dut);

#endif