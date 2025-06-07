mod arch;
mod core;
mod dev;
mod error;
mod mem;
mod utils;

use crate::{
    core::{Executor, Simulator, sim::VerilatedRuntime},
    dev::{DeviceList, Serial, Timer},
    error::ExecutorError,
    mem::Memory,
};
use std::{error::Error, fs, path::Path, process::ExitCode};

pub fn init_sim(
    fst_file: Option<&Path>,
    image_file: Option<&Path>,
) -> Result<Box<dyn Executor>, Box<dyn Error>> {
    let mut runtime = VerilatedRuntime::new();
    if let Some(fst_path) = fst_file {
        runtime.enable_trace(fst_path);
    }

    let memory = match image_file {
        Some(path) => {
            let image = fs::read(path)?;
            Memory::with_image(&image)
        }
        None => Memory::with_default_image(),
    };

    let devices = DeviceList::new()
        .register(Serial::new())
        .register(Timer::new());

    let simulator = Simulator::new(runtime).init(memory, devices)?;

    Ok(simulator)
}

pub fn run(mut executor: Box<dyn Executor>) -> Result<(), ExitCode> {
    executor
        .step(u64::MAX)
        .map_err(|ExecutorError::Aborted(code)| {
            eprintln!("error: Executor aborted with code {}", code);
            ExitCode::from(code)
        })
}
