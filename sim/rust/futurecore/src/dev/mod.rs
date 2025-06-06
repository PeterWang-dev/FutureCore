mod serial;
mod timer;

pub use serial::Serial;
pub use timer::Timer;

use crate::error::DeviceError;
use std::{cell::RefCell, fmt::Debug, ops::Range};

type DeviceType<T> = RefCell<Box<T>>;

const DEVICE_BASE: u32 = 0xa0000000;
const DEVICE_RANGE: Range<u32> = DEVICE_BASE..(DEVICE_BASE + 0x1000);

#[derive(Debug, Default)]
pub struct DeviceList {
    devices: Vec<DeviceType<dyn Device>>,
}

impl DeviceList {
    pub fn new() -> Self {
        DeviceList::default()
    }

    pub fn register<T: Device + 'static>(mut self, device: T) -> Self {
        self.devices.push(RefCell::new(Box::new(device)));
        self
    }

    fn check_range(&self, addr: u32) -> Result<(), DeviceError> {
        if !DEVICE_RANGE.contains(&addr) {
            return Err(DeviceError::AddressOutOfBound(addr));
        }
        Ok(())
    }

    pub fn read(&self, addr: u32) -> Result<u32, DeviceError> {
        self.check_range(addr)?;

        for device in self.devices.iter().map(|d| d.borrow()) {
            if device.in_range(addr) {
                return device.read(addr);
            }
        }

        Err(DeviceError::AddressNotMapped(addr))
    }

    pub fn write(&self, addr: u32, data: u32, mask: u8) -> Result<(), DeviceError> {
        self.check_range(addr)?;

        for device in self.devices.iter() {
            if device.borrow().in_range(addr) {
                return device.borrow_mut().write(addr, data, mask);
            }
        }

        Err(DeviceError::AddressNotMapped(addr))
    }
}

pub trait Device: Debug + Send + Sync {
    fn ident(&self) -> &'static str;
    fn in_range(&self, addr: u32) -> bool;
    fn read(&self, addr: u32) -> Result<u32, DeviceError> {
        Err(DeviceError::ReadNotSupported(self.ident(), addr))
    }
    fn write(&mut self, addr: u32, _data: u32, _mask: u8) -> Result<(), DeviceError> {
        Err(DeviceError::WriteNotSupported(self.ident(), addr))
    }
}
