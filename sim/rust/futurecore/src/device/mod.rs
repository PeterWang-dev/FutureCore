mod serial;
mod timer;

use std::{
    fmt::Debug,
    ops::Range,
    sync::{OnceLock, RwLock},
};

pub static DEVICES: OnceLock<DeviceList> = OnceLock::new();
pub const DEVICE_RANGE: Range<usize> = DEVICE_BASE..(DEVICE_BASE + 0x1000);

const DEVICE_BASE: usize = 0xa0000000;

pub type DeviceList = Vec<RwLock<IoMap>>;

#[derive(Debug)]
pub struct IoMap {
    name: &'static str,
    range: Range<usize>,
    component: Box<dyn Device>,
}

trait Device: Debug + Send + Sync {
    fn register(self) -> IoMap;
    fn read(&self, addr: usize) -> u32;
    fn write(&mut self, addr: usize, data: u32, mask: u8);
}

pub fn init_device() {
    let mut devices = Vec::new();
    // ! Warning: No check for overlap of device memory space!
    devices.push(timer::Timer::default().register().into());
    devices.push(serial::Serial::default().register().into());
    DEVICES.set(devices).expect("Failed to initialize devices");
}

pub fn mmio_read(addr: usize) -> u32 {
    assert!(addr >= DEVICE_BASE && addr < DEVICE_BASE + 0x1000);
    let devices = DEVICES.get().unwrap();
    for device in devices.iter().map(|d| d.read().unwrap()) {
        if device.range.contains(&addr) {
            return device.component.read(addr);
        }
    }
    panic!("Invalid MMIO read at address: {:#x}", addr);
}

pub fn mmio_write(addr: usize, data: u32, mask: u8) {
    assert!(addr >= DEVICE_BASE && addr < DEVICE_BASE + 0x1000);
    let devices = DEVICES.get().unwrap();
    for device in devices.iter() {
        if device.read().unwrap().range.contains(&addr) {
            device.write().unwrap().component.write(addr, data, mask);
            return;
        }
    }
    panic!("Invalid MMIO write at address: {:#x}", addr);
}
