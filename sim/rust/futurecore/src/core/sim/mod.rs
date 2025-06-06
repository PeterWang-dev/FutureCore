mod dpic;
mod verilated;

pub use verilated::VerilatedRuntime;

use super::{Executor, State};
use crate::{arch::rv32i::Rv32iRegs, dev::DeviceList, error::ExecutorError, mem::Memory};
use dpic::{DpiVar, init_dpic};

#[derive(Debug, Default)]
pub struct Simulator {
    status: DpiVar<State>,
    registers: DpiVar<Rv32iRegs>,
    memory: DpiVar<Memory>,
    devices: DpiVar<DeviceList>,
    runtime: VerilatedRuntime,
}

impl Simulator {
    pub fn new(runtime: VerilatedRuntime) -> Self {
        Simulator {
            runtime,
            ..Default::default()
        }
    }

    fn check_status(&mut self) -> Result<bool, ExecutorError> {
        let mut status = self.status.lock().expect("Failed to lock status");
        match *status {
            State::Stopped => {
                *status = State::Running;
                Ok(true)
            }
            State::Running => Ok(true),
            State::Aborted(code) => Err(ExecutorError::Aborted(code)),
            State::Exited => Ok(false),
        }
    }
}

impl Executor for Simulator {
    fn init(self, memory: Memory, devices: DeviceList) -> Result<Box<dyn Executor>, ExecutorError> {
        let mut executor = Self::new(self.runtime);

        executor.memory = DpiVar::new(memory.into());
        executor.devices = DpiVar::new(devices.into());

        init_dpic(
            &executor.status,
            &executor.registers,
            &executor.memory,
            &executor.devices,
        );
        executor.runtime.reset(10);

        Ok(Box::new(executor))
    }

    fn step(&mut self, num_cycles: u32) -> Result<(), ExecutorError> {
        for _ in 0..num_cycles {
            if !self.check_status()? {
                break;
            }
            self.runtime.step(1);
        }

        Ok(())
    }

    fn status(&self) -> State {
        self.status.lock().expect("Failed to lock status").clone()
    }
}
