package futurecore.riscv

import spinal.core._

object Zicsr extends InstructionSet {
  val Opcode = Rv32i.Opcode
  val Rd = Rv32i.Rd
  val Func3 = Rv32i.Funct3
  val Rs1 = Rv32i.Rs1

  object Csr extends BitField(31 downto 20)
  object MicroImm extends Rv32i.Imm(19 downto 15)

  def TypeR(pat: MaskedLiteral) = Instruction(pat, Seq(Opcode, Rd, Func3, Rs1, Csr))
  def TypeI(pat: MaskedLiteral) = Instruction(pat, Seq(Opcode, Rd, MicroImm, Csr))

  val Csrrw = TypeR(M"--------_-----_-----_001_-----_0001111")
  val Csrrs = TypeR(M"--------_-----_-----_010_-----_0001111")
  val Csrrc = TypeR(M"--------_-----_-----_011_-----_0001111")
  val Csrrwi = TypeI(M"--------_-----_-----_101_-----_0001111")
  val Csrrsi = TypeI(M"--------_-----_-----_110_-----_0001111")
  val Csrrci = TypeI(M"--------_-----_-----_111_-----_0001111")

  override def ident: String = "zicsr"

  override def instructions: Seq[Instruction] = Seq(
    Csrrw,
    Csrrs
    // CsrRc,
    // CsrRwI,
    // CsrRsI,
    // CsrRcI
  )

}
