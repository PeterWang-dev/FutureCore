#ifndef _MEMORY_H_
#define _MEMORY_H_

void init_mem(const char *img_path);

#ifdef __cplusplus
extern "C" {
#endif
int pmem_read(int raddr);
void pmem_write(int waddr, int wdata, char wmask);
#ifdef __cplusplus
}
#endif

#endif