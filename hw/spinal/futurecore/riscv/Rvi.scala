package futurecore.riscv

import spinal.core._

/** RV32I Base Instruction Set Dinition */
object Rvi extends InstructionSet {
  object Opcode extends BitField(6 downto 0)
  object Rd extends BitField(11 downto 7)
  object Funct3 extends BitField(14 downto 12)
  object Rs1 extends BitField(19 downto 15)
  object Rs2 extends BitField(24 downto 20)
  object Funct7 extends BitField(31 downto 25)

  // Immediate Definitions
  class Imm(r: Range*) extends BitField(r: _*)
  // I-type immediate: single contiguous range [31:20]
  object IImm extends Imm(31 downto 20)
  // S-type immediate: distributed across two ranges [11:7] and [31:25]
  object SImm extends Imm((11 downto 7), (31 downto 25))
  // B-type immediate: distributed across multiple ranges [31|7|30:25|11:8]
  object BImm extends Imm((31 downto 31), (7 downto 7), (30 downto 25), (11 downto 8))
  // U-type immediate: single contiguous range [31:12]
  object UImm extends Imm(31 downto 12)
  // J-type immediate: distributed across multiple ranges [31|19:12|20|30:21]
  object JImm extends Imm((31 downto 31), (19 downto 12), (20 downto 20), (30 downto 21))

  def TypeR(pat: MaskedLiteral) = Instruction(pat, Seq(Opcode, Rd, Rs1, Rs2, Funct3, Funct7))
  def TypeI(pat: MaskedLiteral) = Instruction(pat, Seq(Opcode, Rd, Rs1, Funct3, IImm))
  def TypeS(pat: MaskedLiteral) = Instruction(pat, Seq(Opcode, Rs1, Rs2, Funct3, SImm))
  def TypeB(pat: MaskedLiteral) = Instruction(pat, Seq(Opcode, Rs1, Rs2, Funct3, BImm))
  def TypeU(pat: MaskedLiteral) = Instruction(pat, Seq(Opcode, Rd, UImm))
  def TypeJ(pat: MaskedLiteral) = Instruction(pat, Seq(Opcode, Rd, JImm))

  // U-type: Upper Immediate
  val Lui = TypeU(M"-------------------------_-----_0110111")
  val Auipc = TypeU(M"-------------------------_-----_0010111")

  // J-type: Jump
  val Jal = TypeJ(M"-------------------------_-----_1101111")

  // I-type: Jump
  val Jalr = TypeI(M"---------------_-----_000_-----_1100111")

  // B-type: Branch
  val Beq = TypeB(M"-----------_-----_-----_000_-----_1100011")
  val Bne = TypeB(M"-----------_-----_-----_001_-----_1100011")
  val Blt = TypeB(M"-----------_-----_-----_100_-----_1100011")
  val Bge = TypeB(M"-----------_-----_-----_101_-----_1100011")
  val Bltu = TypeB(M"-----------_-----_-----_110_-----_1100011")
  val Bgeu = TypeB(M"-----------_-----_-----_111_-----_1100011")

  // I-type: Load
  val Lb = TypeI(M"-----------_-----_000_-----_0000011")
  val Lh = TypeI(M"-----------_-----_001_-----_0000011")
  val Lw = TypeI(M"-----------_-----_010_-----_0000011")
  val Lbu = TypeI(M"-----------_-----_100_-----_0000011")
  val Lhu = TypeI(M"-----------_-----_101_-----_0000011")

  // S-type: Store
  val Sb = TypeS(M"-----------_-----_-----_000_-----_0100011")
  val Sh = TypeS(M"-----------_-----_-----_001_-----_0100011")
  val Sw = TypeS(M"-----------_-----_-----_010_-----_0100011")

  // I-type: Immediate Arithmetic
  val Addi = TypeI(M"-----------_-----_000_-----_0010011")
  val Slti = TypeI(M"-----------_-----_010_-----_0010011")
  val Sltiu = TypeI(M"-----------_-----_011_-----_0010011")
  val Xori = TypeI(M"-----------_-----_100_-----_0010011")
  val Ori = TypeI(M"-----------_-----_110_-----_0010011")
  val Andi = TypeI(M"-----------_-----_111_-----_0010011")

  // I-type: Shift Immediate (funct7_shamt_rs1_funct3_rd_opcode)
  val Slli = TypeI(M"0000000_-----_-----_001_-----_0010011")
  val Srli = TypeI(M"0000000_-----_-----_101_-----_0010011")
  val Srai = TypeI(M"0100000_-----_-----_101_-----_0010011")

  // R-type: Register Arithmetic
  val Add = TypeR(M"0000000_-----_-----_000_-----_0110011")
  val Sub = TypeR(M"0100000_-----_-----_000_-----_0110011")
  val Sll = TypeR(M"0000000_-----_-----_001_-----_0110011")
  val Slt = TypeR(M"0000000_-----_-----_010_-----_0110011")
  val Sltu = TypeR(M"0000000_-----_-----_011_-----_0110011")
  val Xor = TypeR(M"0000000_-----_-----_100_-----_0110011")
  val Srl = TypeR(M"0000000_-----_-----_101_-----_0110011")
  val Sra = TypeR(M"0100000_-----_-----_101_-----_0110011")
  val Or = TypeR(M"0000000_-----_-----_110_-----_0110011")
  val And = TypeR(M"0000000_-----_-----_111_-----_0110011")

  // I-type: Memory Ordering (fm_pred_succ_rs1_000_rd_opcode)
  val Fence = TypeI(M"--------_-----_-----_000_-----_0001111")
  val FenceTso = TypeI(M"1000_0011_0011_-----_000_-----_0001111")
  val Pause = TypeI(M"0000_0001_0000_-----_000_-----_0001111")

  // I-type: Environment (funct12_rs1_000_rd_1110011)
  val Ecall = TypeI(M"000000000000_00000_000_00000_1110011")
  val Ebreak = TypeI(M"000000000001_00000_000_00000_1110011")

  // InstructionSet trait implementation
  override def ident: String = "rv32i"

  override def instructions: Seq[Instruction] = List(
    Lui,
    Auipc,
    Jal,
    Jalr,
    Beq,
    Bne,
    Blt,
    Bge,
    Bltu,
    Bgeu,
    Lb,
    Lh,
    Lw,
    Lbu,
    Lhu,
    Sb,
    Sh,
    Sw,
    Addi,
    Slti,
    Sltiu,
    Xori,
    Ori,
    Andi,
    Slli,
    Srli,
    Srai,
    Add,
    Sub,
    Sll,
    Slt,
    Sltu,
    Xor,
    Srl,
    Sra,
    Or,
    And
    // Fence,
    // FenceTso,
    // Pause,
    // Ecall,
    // Ebreak
  )
}
