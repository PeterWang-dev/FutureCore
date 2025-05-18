use std::{
    sync::{OnceLock, RwLock},
    vec,
};

pub static PMEM: OnceLock<RwLock<Memory>> = OnceLock::new();

pub const MEM_SIZE: usize = 0x8000000;

const DEFAULT_IMAGE: [u32; 5] = [
    0x00000297, // auipc t0,0
    0x00028823, // sb  zero,16(t0)
    0x0102c503, // lbu a0,16(t0)
    0x00100073, // ebreak (used as nemu_trap)
    0xdeadbeef, // some data
];

#[derive(Debug)]
pub struct Memory {
    inner: Box<[u8]>,
}

impl Memory {
    pub fn new() -> Self {
        Memory::default()
    }

    pub fn with_image(image: &[u8]) -> Self {
        let mut memory = Memory::new();
        let image_len = image.len();
        if image_len > MEM_SIZE {
            panic!("Image size exceeds memory size");
        }
        memory.inner[0..image_len].copy_from_slice(image);
        memory
    }

    pub fn new_with_default() -> Self {
        Self::with_image(
            &DEFAULT_IMAGE
                .iter()
                .flat_map(|&x| x.to_le_bytes())
                .collect::<Vec<u8>>(),
        )
    }

    pub fn read(&self, addr: usize) -> u32 {
        u32::from_le_bytes(
            self.inner[addr as usize..addr as usize + 4]
                .try_into()
                .expect("Failed to read memory"),
        )
    }

    pub fn write(&mut self, offset: usize, data: u32, mask: u8) {
        let data_le_bytes = data.to_le_bytes();
        match mask {
            0x01 => self.inner[offset] = data_le_bytes[0],
            0x03 => self.inner[offset..offset + 2].copy_from_slice(&data_le_bytes[0..2]),
            0x0f => self.inner[offset..offset + 4].copy_from_slice(&data_le_bytes),
            _ => panic!("Error: Invalid memory write mask: {:#x}", mask),
        }
    }
}

impl Default for Memory {
    fn default() -> Self {
        Memory {
            inner: vec![0; MEM_SIZE].into_boxed_slice(),
        }
    }
}

pub fn init_pmem(memory: Memory) {
    PMEM.set(memory.into()).expect("Memory already set");
}
