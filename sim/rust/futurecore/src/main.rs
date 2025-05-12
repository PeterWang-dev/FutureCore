use clap::Parser;
use futurecore::{
    EBREAK_RETRUN,
    mem::{Memory, init_pmem},
    verilated::VerilatedRuntime,
};
use std::{ffi::OsString, path::Path, process::ExitCode};

/// Rust based Verilator simulation for the Future Core
#[derive(Parser)]
#[clap(version, about, long_about = None)]
struct Cli {
    /// Enable trace to FST file
    #[arg(short, long, value_name = "FST FILE")]
    trace: Option<OsString>,
    /// The path to the image file to load into memory
    image: Option<OsString>,
}

fn main() -> ExitCode {
    let cli = Cli::parse();

    if let Some(image_path) = cli.image {
        let image_path = Path::new(&image_path);
        if !image_path.exists() {
            eprintln!("Error: Image file does not exist: {:?}", image_path);
            return ExitCode::FAILURE;
        }
        let image = std::fs::read(image_path).expect("Failed to read image file");
        init_pmem(Memory::with_image(&image));
    } else {
        init_pmem(Memory::new_with_default());
    }

    let mut runtime = VerilatedRuntime::new();

    if let Some(trace_path) = cli.trace {
        let trace_path = Path::new(&trace_path);
        eprintln!("Trace wave to fst file: {:?}", trace_path);
        runtime.enable_trace(trace_path);
    }

    runtime.reset(10);
    loop {
        if EBREAK_RETRUN.get().is_some() {
            break;
        }
        runtime.step(1);
    }

    if *EBREAK_RETRUN.get().unwrap() != 0 {
        return ExitCode::FAILURE;
    }

    ExitCode::SUCCESS
}
