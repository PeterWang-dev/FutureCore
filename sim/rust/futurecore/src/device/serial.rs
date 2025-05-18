use super::{DEVICE_BASE, Device, IoMap};
use std::ops::Range;

const ADDR: usize = DEVICE_BASE + 0x3f8;
const RANGE: Range<usize> = ADDR..(ADDR + 4);

#[derive(Debug, Default)]
pub struct Serial;

impl Device for Serial {
    fn register(self) -> IoMap {
        IoMap {
            name: "Serial",
            range: RANGE,
            component: Box::new(self),
        }
    }

    fn read(&self, _addr: usize) -> u32 {
        0 // ! WARNING: Should be unimplemented!() here but we need 0 to pass async read
    }

    fn write(&mut self, addr: usize, data: u32, mask: u8) {
        assert!(addr >= ADDR && addr < ADDR + 4);
        match mask {
            0x01 => print!("{}", data as u8 as char),
            _ => panic!("Error: Serial does not support the write mask: {:#x}", mask),
        }
    }
}
