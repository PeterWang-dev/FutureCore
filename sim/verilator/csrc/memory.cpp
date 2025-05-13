#include "memory.h"
#include "macro.h"
#include "runtime.h"
#include <cassert>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>

extern VerilatedRuntime *runtime;

static uint8_t pmem[CONFIG_MSIZE] PG_ALIGN = {};

static const uint32_t default_img[] = {
    0x00000297, // auipc t0,0
    0x00028823, // sb  zero,16(t0)
    0x0102c503, // lbu a0,16(t0)
    0x00100073, // ebreak (used as nemu_trap)
    0xdeadbeef, // some data
};

static uint8_t *guest_to_host(int paddr) {
  return pmem + (paddr - CONFIG_MBASE);
}

static inline bool in_pmem(int addr) {
  return addr - CONFIG_MBASE < CONFIG_MSIZE;
}

static inline void out_of_bound(int addr) {
  printf("address = " FMT_PADDR " is out of bound of pmem [" FMT_PADDR
         ", " FMT_PADDR "]\n",
         addr, PMEM_LEFT, PMEM_RIGHT);
}

void init_mem(const char *img_path) {

  if (img_path == nullptr) {
    // Initialize pmem with default image
    memcpy(pmem, default_img, sizeof(default_img));
    return;
  }

  FILE *fp = fopen(img_path, "rb");
  if (fp == nullptr) {
    fprintf(stderr, "Failed to open image file: %s\n", img_path);
    exit(EXIT_FAILURE);
  }

  fseek(fp, 0, SEEK_END);
  size_t size = ftell(fp);
  printf("Reading image: %s, size: %ld\n", img_path, size);

  fseek(fp, 0, SEEK_SET);
  size_t read_size = fread(guest_to_host(RESET_VECTOR), size, 1, fp);
  if (read_size != 1) {
    fprintf(stderr, "Failed to read image file: %s\n", img_path);
    fclose(fp);
    exit(EXIT_FAILURE);
  }

  fclose(fp);
  return;
}

int pmem_read(int raddr) {
  uint32_t addr = raddr;
  IFDEF(CONFIG_MTRACE,
        log_write("[mtrace] read %d bytes in " FMT_PADDR "\n", 32, addr));

  if (unlikely(!in_pmem(addr))) {
    out_of_bound(addr);
    return 0;
  }

  IFDEF(CONFIG_DEVICE, return mmio_read(addr, len));

  int ret = *(uint32_t *)(guest_to_host(addr));
  // print fetched data
  // printf("pmem_read: addr = " FMT_PADDR ", data = " FMT_WORD "\n", addr,
  //        ret);

  return ret;
}

void pmem_write(int waddr, int wdata, char wmask) {
  uint32_t addr = waddr;
  IFDEF(CONFIG_MTRACE, log_write("[mtrace] write %d bytes in " FMT_PADDR
                                 " with " FMT_WORD "\n",
                                 len, addr, data));

  if (unlikely(!in_pmem(addr))) {
    out_of_bound(addr);
    return;
  }

  IFDEF(CONFIG_DEVICE, mmio_write(addr, len, data); return);

  switch (wmask) {
  case 0x1:
    *(uint8_t *)(guest_to_host(addr)) = wdata;
    return;
  case 0x3:
    *(uint16_t *)(guest_to_host(addr)) = wdata;
    return;
  case 0xf:
    *(uint32_t *)(guest_to_host(addr)) = wdata;
    return;
  default:
    assert(0);
  }
}
