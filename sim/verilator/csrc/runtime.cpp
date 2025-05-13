#include "runtime.h"
#include "VFutureCore.h"
#include "macro.h"
#include <memory>
#include <verilated.h>
#include <verilated_fst_c.h>

VerilatedRuntime::VerilatedRuntime()
    : contextp{new VerilatedContext},
      dutp{new TOP_NAME{contextp.get(), STR(TOP_NAME)}}
#if VM_TRACE_FST
      ,
      tfp{new VerilatedFstC}
#endif
{
  contextp->debug(0);
  contextp->randReset(2);
#if VM_TRACE_FST
  contextp->traceEverOn(true);

  dutp.get()->trace(tfp.get(), 99);
  Verilated::mkdir("logs");
  tfp->open("logs/vlt_dump.fst");
#endif
}

VerilatedRuntime::VerilatedRuntime(int argc, char **argv)
    : contextp{new VerilatedContext},
      dutp{new TOP_NAME{contextp.get(), STR(TOP_NAME)}}
#if VM_TRACE_FST
      ,
      tfp{new VerilatedFstC}
#endif
{
  contextp->debug(0);
  contextp->randReset(2);
#if VM_TRACE_FST
  contextp->traceEverOn(true);
  contextp->commandArgs(argc, argv);

  dutp.get()->trace(tfp.get(), 99);
  Verilated::mkdir("logs");
  tfp->open("logs/vlt_dump.fst");
#endif
};

VerilatedContext &VerilatedRuntime::context() { return *contextp; }

TOP_NAME &VerilatedRuntime::dut() { return *dutp; }

#if VM_TRACE_FST
VerilatedFstC &VerilatedRuntime::tf() {
  assert(tfp != NULL);
  return *tfp;
}
#endif

void VerilatedRuntime::step(int n) {
  while (n--) {
    contextp->timeInc(1);
    dutp->clk = 1;
    dutp->eval();
#if VM_TRACE_FST
    tfp->dump(contextp->time());
#endif

    contextp->timeInc(1);
    dutp->clk = 0;
    dutp->eval();
#if VM_TRACE_FST
    tfp->dump(contextp->time());
#endif
  }
}

void VerilatedRuntime::reset(int n) {
  dutp->resetn = 0;
  step(n);
  dutp->resetn = 1;
}

void VerilatedRuntime::set_finish() { end = true; }
bool VerilatedRuntime::is_finish() { return end || contextp->gotFinish(); }

VerilatedRuntime::~VerilatedRuntime() {
  dutp.get()->final();
#if VM_TRACE_FST
  tfp->close();
#endif
#if VM_COVERAGE
  Verilated::mkdir("logs");
  contextp->coveragep()->write("logs/coverage.dat");
#endif
}