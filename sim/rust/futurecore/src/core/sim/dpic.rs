use super::State;
use crate::{
    arch::rv32i::Rv32iRegs,
    dev::{DEVICE_RANGE, DeviceList},
    error::DeviceError,
    mem::Memory,
};
use std::{
    cell::{Ref, RefCell, RefMut},
    ops::Deref,
    slice,
    sync::{Arc, OnceLock},
};

pub type DpiVar<T> = Arc<UnsafeDpiVar<T>>;
type OnceDpiVar<T> = OnceLock<DpiVar<T>>;

static STATUS: OnceDpiVar<State> = OnceDpiVar::new();
static REGISTERS: OnceDpiVar<Rv32iRegs> = OnceDpiVar::new();
static MEMORY: OnceDpiVar<Memory> = OnceDpiVar::new();
static DEVICES: OnceDpiVar<DeviceList> = OnceDpiVar::new();

#[derive(Debug, Default)]
pub struct UnsafeDpiVar<T> {
    inner: RefCell<T>,
}

unsafe impl<T> Send for UnsafeDpiVar<T> {}
unsafe impl<T> Sync for UnsafeDpiVar<T> {}

impl<T> From<T> for UnsafeDpiVar<T> {
    fn from(value: T) -> Self {
        UnsafeDpiVar {
            inner: RefCell::new(value),
        }
    }
}

impl<T> Deref for UnsafeDpiVar<T> {
    type Target = RefCell<T>;

    fn deref(&self) -> &Self::Target {
        &self.inner
    }
}

fn get_memory() -> Ref<'static, Memory> {
    MEMORY.get().expect("DPI-MEMORY not initialized").borrow()
}

fn get_memory_mut() -> RefMut<'static, Memory> {
    MEMORY
        .get()
        .expect("DPI-MEMORY not initialized")
        .borrow_mut()
}

fn get_devices() -> Ref<'static, DeviceList> {
    DEVICES.get().expect("DPI-DEVICES not initialized").borrow()
}

#[unsafe(no_mangle)]
pub extern "C" fn pmem_read(raddr: u32) -> u32 {
    if DEVICE_RANGE.contains(&raddr) {
        let devices = get_devices();

        match devices.read(raddr) {
            Ok(data) => data,
            Err(DeviceError::ReadNotSupported(_d, _a)) => {
                // Commented out to avoid unnecessary warnings
                // eprint!(
                //     "warning: pmem_read: {} does not support read at address {:#x}",
                //     _d, _a
                // );
                // eprintln!("Returning 0x0 as default value.");
                // ! Note: At hardware side, read is asynchronous, so this function is always been evaluated.
                // !       Returning 0x0 to prevent unexpected panic!
                0
            }
            Err(e) => {
                panic!("error: pmem_read: {}", e);
            }
        }
    } else {
        let memory = get_memory();
        match memory.read(raddr) {
            Ok(data) => data,
            Err(e) => panic!("error: pmem_read: {}", e),
        }
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn pmem_write(waddr: u32, wdata: u32, wmask: u8) {
    if DEVICE_RANGE.contains(&waddr) {
        let devices = get_devices();
        if let Err(e) = devices.write(waddr, wdata, wmask) {
            panic!("error: pmem_write: {}", e);
        }
    } else {
        let mut memory = get_memory_mut();
        if let Err(e) = memory.write(waddr, wdata, wmask) {
            panic!("error: pmem_write: {}", e);
        }
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn ebreak(status: u32) {
    let mut dpi_status = STATUS
        .get()
        .expect("DPI-STATUS not initialized")
        .borrow_mut();

    *dpi_status = if status == 0 {
        State::Exited
    } else {
        State::Aborted(status as u8)
    };
}

#[unsafe(no_mangle)]
pub extern "C" fn get_regs(gpr: *const u32) {
    let gpr_slice = unsafe { slice::from_raw_parts(gpr, 32) };
    let mut regs = REGISTERS
        .get()
        .expect("DPI-REGISTERS not initialized")
        .borrow_mut();
    *regs = Rv32iRegs::try_from(gpr_slice).expect("Failed to convert raw registers to Rv32iRegs");
}

pub fn init_dpic(
    status: &DpiVar<State>,
    registers: &DpiVar<Rv32iRegs>,
    memory: &DpiVar<Memory>,
    devices: &DpiVar<DeviceList>,
) {
    STATUS
        .set(Arc::clone(status))
        .expect("DPI-STATUS already initialized");
    REGISTERS
        .set(Arc::clone(registers))
        .expect("DPI-REGISTERS already initialized");
    MEMORY
        .set(Arc::clone(memory))
        .expect("DPI-MEMORY already initialized");
    DEVICES
        .set(Arc::clone(devices))
        .expect("DPI-DEVICES already initialized");
}
