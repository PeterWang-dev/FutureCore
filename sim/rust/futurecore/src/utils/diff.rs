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
};

const DIFF_PORT: i32 = 1234;

thread_local! {
    static CONTAINER: OnceCell<Container<Api>> = OnceCell::new();
    static DO_SKIP_REF: Cell<bool> = Cell::new(false);
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
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Context {
    gpr: [u32; 32],
    csr: [u32; 4096],
    pc: u32,
}

impl Context {
    pub fn new() -> Self {
        Context::default()
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

pub fn init(
    diff_so: &Path,
    init_mem: Memory,
    init_ctx: impl Into<Context>,
) -> Result<(), DiffError> {
    let cont: Container<Api> =
        unsafe { Container::load(diff_so).map_err(DiffError::CanNotLoadObject)? };
    if let Err(_) = CONTAINER.with(|ori| ori.set(cont)) {
        panic!("CONTAINER already initialized");
    };

    CONTAINER.with(|cont| {
        let cont = cont.get().expect("CONTAINER not initialized");
        unsafe {
            cont.difftest_init(DIFF_PORT);
            cont.difftest_memcpy(
                0,
                init_mem.image().as_ptr() as *const c_void,
                init_mem.size(),
                Direction::ToRef.into(),
            );
            cont.difftest_regcpy(
                &init_ctx.into() as *const Context as *const c_void,
                Direction::ToDut.into(),
            );
        }
    });

    Ok(())
}

pub fn skip_ref() {
    DO_SKIP_REF.with(|do_skip_ref| {
        do_skip_ref.replace(true);
    });
}

pub fn test(dut_ctx: impl Into<Context>) {
    let dut_ctx = dut_ctx.into();
    let mut ref_ctx = Context::new();

    CONTAINER.with(|cont| {
        let cont = cont.get().expect("CONTAINER not initialized");

        DO_SKIP_REF.with(|do_skip_ref| {
            if do_skip_ref.get() {
                let skipped_ctx = dut_ctx.into();
                unsafe {
                    cont.difftest_regcpy(
                        &skipped_ctx as *const Context as *const c_void,
                        Direction::ToRef.into(),
                    );
                }
                do_skip_ref.replace(false);
            }
        });

        unsafe {
            cont.difftest_exec(1);
            cont.difftest_regcpy(
                &mut ref_ctx as *mut Context as *mut c_void,
                Direction::ToRef.into(),
            );
        };
    });

    if ref_ctx != dut_ctx {
        panic!(
            "Reference registers {:?} do not match DUT registers {:?}",
            ref_ctx, dut_ctx
        );
    }
}
