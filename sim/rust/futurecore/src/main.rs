use futurecore::{
    mem::{Memory, init_pmem},
    verilated::VerilatedRuntime,
};
use std::{path::Path, process::ExitCode};

fn main() -> ExitCode {
    init_pmem(Memory::new_with_default());
    
    let mut runtime = VerilatedRuntime::new();

    let fst_file = Path::new("trace.fst");
    runtime.enable_trace(fst_file);

    runtime.reset(10);

    loop {
        if runtime.is_finished() {
            break;
        }
        runtime.step(1);
    }

    ExitCode::SUCCESS
}
