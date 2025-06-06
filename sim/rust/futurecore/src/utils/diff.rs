#[repr(C)]
pub struct NemuDiffContext {
    pub gpr: [u32; 32],
    pub pc: u32,
}