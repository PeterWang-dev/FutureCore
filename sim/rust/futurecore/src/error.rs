use std::io;
use thiserror::Error;

#[derive(Debug, Error)]
pub enum ExecutorError {
    #[error("Executor exited abnormally with code: {0}")]
    Aborted(u8),
}

#[derive(Debug, Error)]
pub enum MemoryError {
    #[error("Can not initialize memory with the given image: {0}")]
    InitError(io::Error),
    #[error("Address out of bound: {0:#x}")]
    AddressOutOfBound(u32),
    #[error("Illegal memory write mask: {0:#x} for address: {1:#x}")]
    IllegalMask(u8, u32),
}

#[derive(Debug, Error)]
pub enum DeviceError {
    #[error("Unsupported device: {0}")]
    UnsupportedDevice(String),
    #[error("Address out of bound: {0:#x}")]
    AddressOutOfBound(u32),
    #[error("Address not mapped to any device: {0:#x}")]
    AddressNotMapped(u32),
    #[error("Read not supported for device: {0} at address: {1:#x}")]
    ReadNotSupported(&'static str, u32),
    #[error("Write not supported for device: {0} at address: {1:#x}")]
    WriteNotSupported(&'static str, u32),
    #[error("Illegal operation: {0} on device: {1} at address: {2:#x}")]
    IllegalOperation(&'static str, &'static str, u32),
}
