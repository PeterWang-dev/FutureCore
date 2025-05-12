#include "csr.h"
#include "runtime.h"
#include <cstdint>

extern VerilatedRuntime *runtime;

struct EbreakStatus {
  // uint32_t thispc;
  int code;
};

EbreakStatus ebreak_status;

void ebreak(int status) {
  TOP_NAME &dutp = runtime->dut();
  runtime->set_finish();
  // status.thispc = dutp.pc;
  ebreak_status.code = status;
}

int get_status() { return ebreak_status.code; }