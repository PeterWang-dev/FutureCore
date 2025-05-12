#include "wrapper.h"

std::unique_ptr<VerilatedContext> make_context() {
  auto contextp = std::make_unique<VerilatedContext>();
  return contextp;
}

std::unique_ptr<VFutureCore> make_dut() {
  auto dutp = std::make_unique<VFutureCore>();
  return dutp;
}

std::unique_ptr<VerilatedFstC> make_fst_c() {
  auto tfp = std::make_unique<VerilatedFstC>();
  return tfp;
}

uint8_t &resetn(VFutureCore &dut) { return dut.resetn; }

uint8_t &clk(VFutureCore &dut) { return dut.clk; }