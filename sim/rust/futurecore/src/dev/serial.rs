use super::{DEVICE_BASE, Device};
use crate::error::DeviceError;
use std::{
    io::{Write, stdout},
    ops::Range,
};

const NAME: &'static str = "Serial";
const ADDR: u32 = DEVICE_BASE + 0x3f8;
const RANGE: Range<u32> = ADDR..(ADDR + 4);

#[derive(Debug, Default)]
pub struct Serial;

impl Serial {
    pub fn new() -> Self {
        Serial::default()
    }
}

impl Device for Serial {
    fn ident(&self) -> &'static str {
        NAME
    }

    fn in_range(&self, addr: u32) -> bool {
        RANGE.contains(&addr)
    }

    fn write(&mut self, addr: u32, data: u32, mask: u8) -> Result<(), DeviceError> {
        assert!(addr >= ADDR && addr < ADDR + 4);
        match mask {
            0x01 => {
                print!("{}", data as u8 as char);
                stdout().flush().expect("Failed to flush stdout");
                Ok(())
            }
            _ => Err(DeviceError::IllegalOperation(
                "Serial does not support multi-byte write",
                NAME,
                addr,
            )),
        }
    }
}
