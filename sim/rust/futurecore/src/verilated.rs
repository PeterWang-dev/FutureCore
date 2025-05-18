use cxx::UniquePtr;
use std::{ffi::CString, fs, mem::ManuallyDrop, path::Path};

use ffi::*;

#[cxx::bridge]
mod ffi {
    unsafe extern "C++" {
        include!("wrapper.h");
        include!("VFutureCore.h");
        include!("verilated.h");
        include!("verilated_fst_c.h");

        type VerilatedContext;
        fn make_context() -> UniquePtr<VerilatedContext>;
        unsafe fn commandArgs(
            self: Pin<&mut VerilatedContext>,
            argc: i32,
            argv: *mut *const c_char,
        );
        fn traceEverOn(self: Pin<&mut VerilatedContext>, flag: bool);
        fn time(self: &VerilatedContext) -> u64;
        fn timeInc(self: Pin<&mut VerilatedContext>, add: u64);
        fn gotFinish(self: &VerilatedContext) -> bool;

        type VFutureCore;
        fn make_dut() -> UniquePtr<VFutureCore>;
        fn eval(self: Pin<&mut VFutureCore>);
        #[cxx_name = "final"]
        fn final_(self: Pin<&mut VFutureCore>);
        unsafe fn trace(
            dut: Pin<&mut VFutureCore>,
            tfp: *mut VerilatedFstC,
            levels: i32,
            options: i32,
        );

        fn resetn(dut: Pin<&mut VFutureCore>) -> &mut u8;
        fn clk(dut: Pin<&mut VFutureCore>) -> &mut u8;

        type VerilatedFstC;
        fn make_fst_c() -> UniquePtr<VerilatedFstC>;
        unsafe fn open(self: Pin<&mut VerilatedFstC>, name: *const c_char);
        fn close(self: Pin<&mut VerilatedFstC>);
        fn flush(self: Pin<&mut VerilatedFstC>);
        fn dump(self: Pin<&mut VerilatedFstC>, time: u64);
    }
}

pub struct VerilatedRuntime {
    contextp: UniquePtr<VerilatedContext>,
    dutp: ManuallyDrop<UniquePtr<VFutureCore>>,
    tfp: Option<UniquePtr<VerilatedFstC>>,
}

impl VerilatedRuntime {
    pub fn new() -> Self {
        VerilatedRuntime {
            contextp: make_context(),
            dutp: ManuallyDrop::new(make_dut()),
            tfp: None,
        }
    }

    pub fn enable_trace(&mut self, filepath: &Path) {
        if filepath.exists() {
            fs::remove_file(filepath).expect("Failed to remove existing fst file");
        }

        if let Some(parent) = filepath.parent() {
            // An empty parent path (e.g., for "file.txt") means the file is intended
            // for the current directory, which doesn't need explicit creation here.
            if parent != Path::new("") {
                fs::create_dir_all(parent)
                    .expect("Failed to create parent directory for trace file");
            }
        }

        let filepath = CString::new(filepath.display().to_string())
            .expect("Failed to convert path to CString");

        self.tfp = Some(make_fst_c());

        let tfp = self.tfp.as_mut().unwrap();
        VerilatedContext::traceEverOn(self.contextp.pin_mut(), true);
        unsafe {
            ffi::trace(self.dutp.pin_mut(), tfp.as_mut_ptr(), 99, 0);
            VerilatedFstC::open(tfp.pin_mut(), filepath.as_ptr());
        }
    }

    pub fn reset(&mut self, num_cycles: u64) {
        *ffi::resetn(self.dutp.pin_mut()) = 0;
        self.step(num_cycles);
        *ffi::resetn(self.dutp.pin_mut()) = 1;
    }

    fn may_dump_trace(&mut self) {
        if let Some(tfp) = self.tfp.as_mut() {
            VerilatedFstC::dump(tfp.pin_mut(), self.contextp.pin_mut().time());
            VerilatedFstC::flush(tfp.pin_mut());
        }
    }

    pub fn step(&mut self, num_cycles: u64) {
        for _ in 0..num_cycles {
            self.contextp.pin_mut().timeInc(1);
            *ffi::clk(self.dutp.pin_mut()) = 1;
            self.dutp.pin_mut().eval();
            self.may_dump_trace();

            self.contextp.pin_mut().timeInc(1);
            *ffi::clk(self.dutp.pin_mut()) = 0;
            self.dutp.pin_mut().eval();
            self.may_dump_trace();
        }
    }

    #[deprecated = "Use `EBREAK_RETRUN` to check if the simulation is finished"]
    pub fn is_finished(&self) -> bool {
        self.contextp.gotFinish()
    }
}

impl Drop for VerilatedRuntime {
    fn drop(&mut self) {
        if let Some(tfp) = self.tfp.as_mut() {
            VerilatedFstC::close(tfp.pin_mut());
        }
        self.tfp = None; // Drop the trace file pointer
        self.dutp.pin_mut().final_();
        unsafe {
            ManuallyDrop::drop(&mut self.dutp); // DUT pointer must be dropped before context
        }
    }
}
