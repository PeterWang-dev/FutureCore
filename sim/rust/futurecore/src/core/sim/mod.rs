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

    #[inline]
    fn check_status(&mut self) -> Result<bool, ExecutorError> {
        let mut status = self.status.borrow_mut();
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

    #[inline]
    fn step_once(&mut self) -> Result<bool, ExecutorError> {
        if self.check_status()? {
            self.runtime.step(1);
            Ok(true)
        } else {
            Ok(false)
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

    fn step(&mut self, num_cycles: u64) -> Result<(), ExecutorError> {
        let mut cycle_remaining = num_cycles;
        loop {
            if self.step_once()? && cycle_remaining != 0 {
                if num_cycles == u64::MAX {
                    continue; // Infinite loop for maximum cycles
                }

                cycle_remaining -= 1;
                continue;
            }
            break;
        }

        Ok(())
    }

    fn status(&self) -> State {
        self.status.borrow().clone()
    }
}
