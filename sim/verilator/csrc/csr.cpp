#include "csr.h"
#include "runtime.h"
#include <cstdint>

extern VerilatedRuntime *runtime;

struct EbreakStatus {
  uint32_t thispc;
  uint32_t code;
};

EbreakStatus status;

void ebreak() {
  TOP_NAME &dutp = runtime->dut();
  runtime->set_finish();
  status.thispc = dutp.pc;
  status.code = dutp.ret;
}

int get_status() { return status.code; }