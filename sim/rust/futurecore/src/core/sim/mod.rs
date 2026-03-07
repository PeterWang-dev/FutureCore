mod dpic;
mod verilated;

pub use verilated::VerilatedRuntime;

use super::{Executor, State};
use crate::{
    arch::rv32i::Registers,
    dev::DeviceList,
    error::ExecutorError,
    mem::Memory,
    utils::diff::{init as diff_init, test as diff_test},
};
use dpic::{DpiVar, init as dpic_init};

#[derive(Debug, Default)]
pub struct Simulator {
    status: DpiVar<State>,
    registers: DpiVar<Registers>,
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
        self.runtime.step(1);
        self.check_status()
    }
}

impl Executor for Simulator {
    fn init(self, memory: Memory, devices: DeviceList) -> Result<Box<dyn Executor>, ExecutorError> {
        let mut executor = Self::new(self.runtime);

        executor.memory = DpiVar::new(memory.into());
        executor.devices = DpiVar::new(devices.into());

        dpic_init(
            &executor.status,
            &executor.registers,
            &executor.memory,
            &executor.devices,
        );

        let diff_ctx: Registers = executor.registers.borrow().clone();
        let diff_mem: Memory = executor.memory.borrow().clone();
        diff_init(diff_mem, diff_ctx);

        executor.runtime.reset(10);

        Ok(Box::new(executor))
    }

    fn step(&mut self, num_cycles: u64) -> Result<(), ExecutorError> {
        let mut cycle_executed = 0;
        loop {
            if num_cycles != u64::MAX && cycle_executed >= num_cycles {
                break;
            }
            if !self.step_once()? {
                break;
            }
            cycle_executed += 1;
            if cycle_executed > 1 {
                diff_test(self.registers.borrow().clone());
            };
        }

        Ok(())
    }

    fn status(&self) -> State {
        self.status.borrow().clone()
    }
}
