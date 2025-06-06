pub mod emu;
pub mod sim;

pub use sim::Simulator;

use crate::{dev::DeviceList, error::ExecutorError, mem::Memory};
use std::fmt::Debug;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum State {
    Stopped,
    Running,
    Aborted(u8),
    Exited,
}

impl Default for State {
    fn default() -> Self {
        State::Stopped
    }
}

pub trait Executor {
    fn init(
        self,
        memory: Memory,
        devices: DeviceList,
    ) -> Result<Box<dyn Executor>, ExecutorError>;
    fn step(&mut self, num: u32) -> Result<(), ExecutorError>;
    fn status(&self) -> State;
}
