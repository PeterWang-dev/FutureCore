use crate::{
    arch::rv32i::{BASE_ADDR as GUEST_BASE, DEFAULT_IMAGE},
    error::MemoryError,
};

const MEM_SIZE: usize = 0x8000000;

#[derive(Debug, Clone)]
pub struct Memory {
    inner: Box<[u8]>,
}

impl Memory {
    pub fn new() -> Self {
        Memory::default()
    }

    pub fn with_image(image: &[u8]) -> Self {
        let mut memory = Memory::default();
        let image_len = image.len();
        if image_len > MEM_SIZE {
            panic!("Image size exceeds memory size");
        }
        memory.inner[0..image_len].copy_from_slice(image);
        memory
    }

    pub fn with_default_image() -> Self {
        Self::with_image(
            &DEFAULT_IMAGE
                .iter()
                .flat_map(|&x| x.to_le_bytes())
                .collect::<Vec<u8>>(),
        )
    }

    pub fn image(&self) -> &[u8] {
        &self.inner
    }

    pub fn size(&self) -> usize {
        self.inner.len()
    }

    fn convert_addr(addr: u32) -> Result<usize, MemoryError> {
        const MAX_ADDR: u32 = GUEST_BASE + MEM_SIZE as u32;
        match addr {
            0..GUEST_BASE | MAX_ADDR..=std::u32::MAX => Err(MemoryError::AddressOutOfBound(addr)),
            GUEST_BASE..MAX_ADDR => Ok((addr - GUEST_BASE) as usize),
        }
    }

    pub fn read(&self, addr: u32) -> Result<u32, MemoryError> {
        let offset = Self::convert_addr(addr)?;
        Ok(u32::from_le_bytes(
            self.inner[offset..offset + 4]
                .try_into()
                .expect("Failed to read memory"),
        ))
    }

    pub fn write(&mut self, addr: u32, data: u32, mask: u8) -> Result<(), MemoryError> {
        let offset = Self::convert_addr(addr)?;
        let data_le_bytes = data.to_le_bytes();
        match mask {
            0x01 => self.inner[offset] = data_le_bytes[0],
            0x03 => self.inner[offset..offset + 2].copy_from_slice(&data_le_bytes[0..2]),
            0x0f => self.inner[offset..offset + 4].copy_from_slice(&data_le_bytes),
            _ => return Err(MemoryError::IllegalMask(mask, addr)),
        }
        Ok(())
    }
}

impl Default for Memory {
    fn default() -> Self {
        Memory {
            inner: vec![0; MEM_SIZE].into_boxed_slice(),
        }
    }
}
