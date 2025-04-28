#ifndef _RUNTIME_H_
#define _RUNTIME_H_
#include "VFutureCore.h"
#include <memory>
#include <verilated.h>
#include <verilated_fst_c.h>

class VerilatedRuntime {
public:
  VerilatedRuntime();
  VerilatedRuntime(int argc, char **argv);
  VerilatedContext &context();
  TOP_NAME &dut();
#if VM_TRACE_FST
  VerilatedFstC &tf();
#endif
  void step(int n);
  void reset(int n);
  void set_finish();
  bool is_finish();
  ~VerilatedRuntime();

private:
  const std::unique_ptr<VerilatedContext> contextp;
  const std::unique_ptr<TOP_NAME> dutp;
#if VM_TRACE_FST
  std::unique_ptr<VerilatedFstC> tfp;
#endif
  bool end = false;
};

#endif