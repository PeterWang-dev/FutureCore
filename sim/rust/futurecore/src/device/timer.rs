use super::{Device, IoMap};
use std::{ops::Range, sync::RwLock, time::Instant};

const ADDR: usize = super::DEVICE_BASE + 0x48;
const RANGE: Range<usize> = ADDR..(ADDR + 2 * size_of::<u32>());

#[derive(Debug)]
pub struct Timer {
    clock: Instant,
    regs: RwLock<[u32; 2]>,
}

impl Timer {
    fn new() -> Self {
        Timer {
            clock: Instant::now(),
            regs: RwLock::new([0; 2]),
        }
    }

    fn elapsed(&self) {
        let time_us = self.clock.elapsed().as_micros() as u64;
        let mut regs = self.regs.write().unwrap();
        regs[0] = time_us as u32;
        regs[1] = (time_us >> 32) as u32;
    }

    fn read(&self, addr: usize) -> u32 {
        assert_eq!(addr % 4, 0);
        self.elapsed();
        self.regs.read().unwrap()[(addr - ADDR) / 4]
    }
}

impl Default for Timer {
    fn default() -> Self {
        Timer::new()
    }
}

impl Device for Timer {
    fn register(self) -> IoMap {
        IoMap {
            name: "Timer",
            range: RANGE,
            component: Box::new(self),
        }
    }

    fn read(&self, addr: usize) -> u32 {
        assert!(addr >= ADDR && addr < ADDR + 8);
        return Timer::read(self, addr);
    }

    fn write(&mut self, _addr: usize, _data: u32, _mask: u8) {
        panic!("Error: Timer should not be written to!");
    }
}
