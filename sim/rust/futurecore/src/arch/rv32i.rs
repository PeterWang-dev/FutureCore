pub const RV32I_BASE_ADDR: u32 = 0x8000_0000;

pub const RV32I_DEFAULT_IMAGE: [u32; 5] = [
    0x00000297, // auipc t0,0
    0x00028823, // sb  zero,16(t0)
    0x0102c503, // lbu a0,16(t0)
    0x00100073, // ebreak (used as nemu_trap)
    0xdeadbeef, // some data
];

#[derive(Debug, Default, Clone, Copy)]
pub struct Rv32iRegs {
    gpr: [u32; 32],
    pc: u32,
}

impl Rv32iRegs {
    pub fn new() -> Self {
        Rv32iRegs::default()
    }
}

impl TryFrom<&[u32]> for Rv32iRegs {
    type Error = &'static str;

    fn try_from(value: &[u32]) -> Result<Self, Self::Error> {
        if value.len() != 32 {
            return Err("Expected 32 registers (32 GPRs)");
        }
        let mut regs = Rv32iRegs::new();
        regs.gpr.copy_from_slice(&value[0..32]);
        Ok(regs)
    }
}
