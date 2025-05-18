use crate::{
    device::{DEVICE_RANGE, mmio_read, mmio_write},
    mem::{MEM_SIZE, PMEM},
};
use std::sync::atomic::{AtomicBool, AtomicU32, Ordering};

const GUEST_BASE: u32 = 0x80000000;

fn guest_to_host(addr: u32) -> usize {
    if addr < GUEST_BASE {
        panic!("Invalid address: {:#x}", addr);
    } else if addr >= GUEST_BASE + MEM_SIZE as u32 {
        panic!("Address out of bounds: {:#x}", addr);
    }
    (addr - GUEST_BASE) as usize
}

#[unsafe(no_mangle)]
pub extern "C" fn pmem_read(raddr: u32) -> u32 {
    if DEVICE_RANGE.contains(&(raddr as usize)) {
        return mmio_read(raddr as usize);
    }
    let offset = guest_to_host(raddr);
    PMEM.get()
        .expect("Memory not initialized")
        .read()
        .unwrap()
        .read(offset)
}

#[unsafe(no_mangle)]
pub extern "C" fn pmem_write(waddr: u32, wdata: u32, wmask: u8) {
    if DEVICE_RANGE.contains(&(waddr as usize)) {
        mmio_write(waddr as usize, wdata, wmask);
        return;
    }
    let addr = guest_to_host(waddr);
    PMEM.get()
        .expect("Memory not initialized")
        .write()
        .unwrap()
        .write(addr, wdata, wmask);
}

pub static IS_EBREAK: AtomicBool = AtomicBool::new(false);
pub static EBREAK_RETRUN: AtomicU32 = AtomicU32::new(u32::MAX);

#[unsafe(no_mangle)]
pub extern "C" fn ebreak(status: u32) {
    IS_EBREAK.store(true, Ordering::SeqCst);
    EBREAK_RETRUN.store(status, Ordering::SeqCst);
}
