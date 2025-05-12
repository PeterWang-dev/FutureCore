#ifndef __CSR_H__
#define __CSR_H__

#ifdef __cplusplus
extern "C" {
#endif
void ebreak(int status);
#ifdef __cplusplus
}
#endif

int get_status();

#endif
