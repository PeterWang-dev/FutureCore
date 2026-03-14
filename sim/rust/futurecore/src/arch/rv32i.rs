pub const BASE_ADDR: u32 = 0x8000_0000;
pub const RESET_VECTOR: u32 = BASE_ADDR + 0x0000_0000;
pub const DEFAULT_IMAGE: [u32; 5] = [
    0x00000297, // auipc t0,0
    0x00028823, // sb  zero,16(t0)
    0x0102c503, // lbu a0,16(t0)
    0x00100073, // ebreak (used as nemu_trap)
    0xdeadbeef, // some data
];

#[derive(Debug, Clone, Copy)]
pub struct Registers {
    gpr: [u32; 32],
    pc: u32,
    mstatus: u32,
    mtvec: u32,
    mepc: u32,
    mcause: u32,
}

impl Registers {
    pub fn new() -> Self {
        Registers::default()
    }

    pub fn with_fields(
        gprs: &[u32; 32],
        pc: u32,
        mstatus: u32,
        mtvec: u32,
        mepc: u32,
        mcause: u32,
    ) -> Self {
        Registers {
            gpr: *gprs,
            pc,
            mstatus,
            mtvec,
            mepc,
            mcause,
        }
    }
    
    pub fn gpr(&self) -> &[u32; 32] {
        &self.gpr
    }

    pub fn pc(&self) -> u32 {
        self.pc
    }

    pub fn mstatus(&self) -> u32 {
        self.mstatus
    }

    pub fn mtvec(&self) -> u32 {
        self.mtvec
    }

    pub fn mepc(&self) -> u32 {
        self.mepc
    }

    pub fn mcause(&self) -> u32 {
        self.mcause
    }
}

impl Default for Registers {
    fn default() -> Self {
        Registers {
            gpr: [0; 32],
            pc: RESET_VECTOR,
            mstatus: 0x1800, // MPP=0b11 (machine mode)
            mtvec: 0,
            mepc: 0,
            mcause: 0,
        }
    }
}
