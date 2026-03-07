use crate::error::DeviceError;

use super::{DEVICE_BASE, Device};
use std::{ops::Range, sync::RwLock, time::Instant};

const NAME: &'static str = "Timer";
const ADDR: u32 = DEVICE_BASE + 0x48;
const RANGE: Range<u32> = ADDR..(ADDR + 2 * size_of::<u32>() as u32);

#[derive(Debug)]
pub struct Timer {
    clock: Instant,
    regs: RwLock<[u32; 2]>,
}

impl Timer {
    pub fn new() -> Self {
        Timer::default()
    }

    fn elapsed(&self) {
        let time_us = self.clock.elapsed().as_micros() as u64;
        let mut regs = self
            .regs
            .write()
            .expect("Failed to acquire write lock twice on Timer regs");
        regs[0] = time_us as u32;
        regs[1] = (time_us >> 32) as u32;
    }

    fn read(&self, addr: u32) -> u32 {
        assert_eq!(addr % 4, 0);
        self.elapsed();
        let regs = self
            .regs
            .read()
            .expect("Failed to acquire read lock twice on Timer regs");
        regs[((addr - ADDR) / 4) as usize]
    }
}

impl Default for Timer {
    fn default() -> Self {
        Timer {
            clock: Instant::now(),
            regs: RwLock::new([0; 2]),
        }
    }
}

impl Device for Timer {
    fn ident(&self) -> &'static str {
        NAME
    }

    fn in_range(&self, addr: u32) -> bool {
        RANGE.contains(&addr)
    }

    fn read(&self, addr: u32) -> Result<u32, DeviceError> {
        assert!(addr >= ADDR && addr < ADDR + 8);
        return Ok(Timer::read(self, addr));
    }
}
