use crate::{
    arch::rv32i::{self, RESET_VECTOR},
    error::DiffError,
    mem::Memory,
};
use dlopen2::wrapper::{Container, WrapperApi};
use std::{
    cell::{Cell, OnceCell},
    ffi::c_void,
    path::Path,
    sync::atomic::{AtomicBool, Ordering},
};

const DIFF_PORT: i32 = 1234;

thread_local! {
    static CONTAINER: OnceCell<Container<Api>> = OnceCell::new();
    // Sticky MMIO read request: set by async `pmem_read()` on device range,
    // consumed and cleared by `test()`. Delayed by one `test()` invocation
    // via `SKIP_READ` because the combinational read precedes the commit.
    static DO_SKIP_REF_READ: Cell<bool> = Cell::new(false);
    // Sticky MMIO write request: set by `pmem_write()` on device range at the
    // posedge, consumed and cleared by `test()`. Applied to the current
    // `test()` because the posedge write coincides with the commit.
    static DO_SKIP_REF_WRITE: Cell<bool> = Cell::new(false);
}

#[derive(WrapperApi)]
struct Api {
    difftest_memcpy: unsafe extern "C" fn(addr: u32, buf: *const c_void, n: usize, direction: bool),
    difftest_regcpy: unsafe extern "C" fn(dut: *const c_void, direction: bool),
    difftest_exec: unsafe extern "C" fn(n: u64),
    difftest_raise_intr: unsafe extern "C" fn(no: u64),
    difftest_init: unsafe extern "C" fn(port: i32),
}

#[repr(C)]
#[derive(Debug, Clone, Copy)]
pub struct Context {
    gpr: [u32; 32],
    csr: [u32; 4096],
    pc: u32,
}

impl Context {
    fn diff(&self, other: &Self) -> Vec<(&'static str, u32, u32)> {
        const GPR_NAMES: [&str; 32] = [
            "x0", "x1", "x2", "x3", "x4", "x5", "x6", "x7", "x8", "x9", "x10", "x11", "x12", "x13",
            "x14", "x15", "x16", "x17", "x18", "x19", "x20", "x21", "x22", "x23", "x24", "x25",
            "x26", "x27", "x28", "x29", "x30", "x31",
        ];
        const CSR_NAMES: [(usize, &str); 4] = [
            (0x300, "mstatus"),
            (0x305, "mtvec"),
            (0x341, "mepc"),
            (0x342, "mcause"),
        ];

        let mut diffs = Vec::new();
        for i in 0..32 {
            if self.gpr[i] != other.gpr[i] {
                diffs.push((GPR_NAMES[i], self.gpr[i], other.gpr[i]));
            }
        }
        if self.pc != other.pc {
            diffs.push(("pc", self.pc, other.pc));
        }
        for (addr, name) in CSR_NAMES {
            if self.csr[addr] != other.csr[addr] {
                diffs.push((name, self.csr[addr], other.csr[addr]));
            }
        }
        diffs
    }
}

impl Default for Context {
    fn default() -> Self {
        let mut csr = [0; 4096];
        csr[0x300] = 0x1800; // mstatus with MPP=0b11 (machine mode)
        Context {
            gpr: [0; 32],
            csr,
            pc: RESET_VECTOR,
        }
    }
}

impl From<rv32i::Registers> for Context {
    fn from(regs: rv32i::Registers) -> Self {
        let mut ctx = Context::default();
        ctx.gpr = *regs.gpr();
        ctx.pc = regs.pc();
        // Copy CSR values to correct addresses
        ctx.csr[0x300] = regs.mstatus();
        ctx.csr[0x305] = regs.mtvec();
        ctx.csr[0x341] = regs.mepc();
        ctx.csr[0x342] = regs.mcause();
        ctx
    }
}

enum Direction {
    /// Direction from reference to DUT
    ToDut,
    /// Direction from DUT to reference
    ToRef,
}

impl Into<bool> for Direction {
    fn into(self) -> bool {
        match self {
            Direction::ToDut => false,
            Direction::ToRef => true,
        }
    }
}

pub fn load(diff_so: &Path) -> Result<(), DiffError> {
    let cont: Container<Api> =
        unsafe { Container::load(diff_so).map_err(DiffError::CanNotLoadObject)? };
    if let Err(_) = CONTAINER.with(|ori| ori.set(cont)) {
        panic!("CONTAINER already initialized");
    };
    Ok(())
}

pub fn init(init_mem: Memory, init_ctx: impl Into<Context>) {
    CONTAINER.with(|cont| {
        let cont = cont.get().expect("CONTAINER not initialized");
        unsafe {
            cont.difftest_init(DIFF_PORT);
            cont.difftest_memcpy(
                RESET_VECTOR,
                init_mem.image().as_ptr() as *const c_void,
                init_mem.size(),
                Direction::ToRef.into(),
            );
            cont.difftest_regcpy(
                &init_ctx.into() as *const Context as *const c_void,
                Direction::ToRef.into(),
            );
        }
    });
}

pub fn request_skip_ref_read() {
    DO_SKIP_REF_READ.with(|flag| flag.set(true));
}

pub fn request_skip_ref_write() {
    DO_SKIP_REF_WRITE.with(|flag| flag.set(true));
}

pub fn test(dut_ctx: impl Into<Context>) {
    let dut_ctx = dut_ctx.into();
    let mut ref_ctx = dut_ctx.clone();
    // One-test delay for async MMIO reads: a read observed during the previous
    // `test()` window corresponds to the instruction committing in this window.
    static SKIP_READ: AtomicBool = AtomicBool::new(false);

    CONTAINER.with(|cont| {
        let cont = cont.get().expect("CONTAINER not initialized");

        let do_skip_read = DO_SKIP_REF_READ.with(|flag| flag.replace(false));
        let do_skip_write = DO_SKIP_REF_WRITE.with(|flag| flag.replace(false));
        let skip_prev_read = SKIP_READ.load(Ordering::Acquire);

        let skip_current = skip_prev_read || do_skip_write;
        SKIP_READ.store(do_skip_read, Ordering::Release);

        if skip_current {
            unsafe {
                cont.difftest_regcpy(
                    &dut_ctx as *const Context as *const c_void,
                    Direction::ToRef.into(),
                );
            }
            return;
        }

        unsafe {
            cont.difftest_exec(1);
            cont.difftest_regcpy(
                &mut ref_ctx as *mut Context as *mut c_void,
                Direction::ToDut.into(),
            );
        }

        let diffs = dut_ctx.diff(&ref_ctx);
        if !diffs.is_empty() {
            let mut msg = String::from("DUT registers do not match reference registers:\n");
            for (name, dut_val, ref_val) in &diffs {
                msg.push_str(&format!(
                    "  {}: ref=0x{:08x}, dut=0x{:08x}\n",
                    name, ref_val, dut_val
                ));
            }
            panic!("{}", msg);
        }
    });
}
