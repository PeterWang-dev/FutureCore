
#include "csr.h"
#include "runtime.h"
#include <cstdio>

#define MAX_SIM_CYCLES 0xffffffff

VerilatedRuntime *runtime = nullptr;

void init_mem(const char *img_path);

int main(int argc, char **argv) {
  init_mem(argv[1]);
  runtime = new VerilatedRuntime();

  printf("Resetting...\n");
  runtime->reset(10);
  printf("Reset done.\n");

  unsigned int counter = 0;
  while (!runtime->is_finish()) {
    if (counter >= MAX_SIM_CYCLES) {
      printf("Simulation reached max cycles (%d)\n", MAX_SIM_CYCLES);
      delete runtime;
      return -1;
    }

    runtime->step(1);
    counter++;
  }

  delete runtime;
  return get_status();
}
