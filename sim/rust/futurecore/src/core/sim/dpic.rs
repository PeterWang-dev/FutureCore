use super::State;
use crate::{
    arch::rv32i::Registers,
    dev::{DEVICE_RANGE, DeviceList},
    error::DeviceError,
    mem::Memory,
    utils::diff::{request_skip_ref_read, request_skip_ref_write},
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
static REGISTERS: OnceDpiVar<Registers> = OnceDpiVar::new();
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
        // Async combinational read: the pmem_read is scheduled before the
        // instruction commits, so the skip must be delayed to the next test().
        request_skip_ref_read();
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
        // Synchronous posedge write: coincides with the commit, so the skip
        // applies to the current test().
        request_skip_ref_write();
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
#[deprecated(note = "regs_dpi at hw becomes a legacy")]
pub extern "C" fn get_regs(gpr: *const u64) {
    let gpr = unsafe {
        slice::from_raw_parts(gpr, 33)
            .iter()
            .map(|n| *n as u32)
            .collect::<Vec<u32>>()
    };

    let mut gpr_array = [0u32; 32];
    gpr_array.copy_from_slice(&gpr[0..32]);
    let pc = gpr[32];

    let regs = Registers::with_fields(&gpr_array, pc, 0x1800, 0, 0, 0);

    let mut dpi_regs = REGISTERS
        .get()
        .expect("DPI-REGISTERS not initialized")
        .borrow_mut();
    *dpi_regs = regs;
}

#[unsafe(no_mangle)]
pub extern "C" fn send_state(
    pc: *const u32,
    _inst: *const u32,
    gprs: *const u32,
    mstatus: *const u32,
    mtvec: *const u32,
    mepc: *const u32,
    mcause: *const u32,
) {
    let gprs = unsafe { slice::from_raw_parts(gprs, 32) }
        .as_array::<32>()
        .expect("size of gprs passed in is not 32");
    let pc = unsafe { *pc };
    let mstatus = unsafe { *mstatus };
    let mtvec = unsafe { *mtvec };
    let mepc = unsafe { *mepc };
    let mcause = unsafe { *mcause };

    let regs = Registers::with_fields(gprs, pc, mstatus, mtvec, mepc, mcause);

    let mut dpi_regs = REGISTERS
        .get()
        .expect("DPI-REGISTERS not initialized")
        .borrow_mut();
    *dpi_regs = regs;
}

pub fn init(
    status: &DpiVar<State>,
    registers: &DpiVar<Registers>,
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
