use crate::mem::{MEM_SIZE, PMEM};

const GUEST_BASE: u32 = 0x80000000;

fn guest_to_host(addr: u32) -> u32 {
    let aligned = addr & !0x3u32;
    if aligned < GUEST_BASE {
        panic!("Invalid address: {:#x}", addr);
    } else if aligned >= GUEST_BASE + MEM_SIZE as u32 {
        panic!("Address out of bounds: {:#x}", addr);
    }
    aligned - GUEST_BASE
}

#[unsafe(no_mangle)]
pub extern "C" fn pmem_read(raddr: u32) -> u32 {
    let addr = guest_to_host(raddr);
    PMEM.get()
        .expect("Memory not initialized")
        .read()
        .unwrap()
        .read(addr)
}

#[unsafe(no_mangle)]
pub extern "C" fn pmem_write(waddr: u32, wdata: u32, wmask: u8) {
    let addr = guest_to_host(waddr);
    let data = match wmask {
        0x01 => wdata & 0x0000ff,
        0x03 => wdata & 0x00ffff,
        0x0f => wdata & 0xffffff,
        _ => panic!("Invalid write mask: {:#x}", wmask),
    };
    PMEM.get()
        .expect("Memory not initialized")
        .write()
        .unwrap()
        .write(addr, data);
}

#[unsafe(no_mangle)]
pub extern "C" fn ebreak() {
    println!("ebreak");
    std::process::exit(0);
}
