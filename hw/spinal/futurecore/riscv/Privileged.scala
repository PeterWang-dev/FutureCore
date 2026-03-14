package futurecore.riscv

import spinal.core._

object Privileged extends InstructionSet {
  val Opcode = Rv32i.Opcode
  val Funct3 = Rv32i.Funct3
  val Funct7 = Rv32i.Funct7

  def TypeSystem(pat: MaskedLiteral) = Instruction(pat, Seq(Opcode, Funct3, Funct7))

  // ! WARNING: Ecall & Ebreak of Rv32i are PRIVILEGED related instructions.
  // !          Should be handled VERY CAREFULLY!
  // !          For now the detailed privilege-related ops are not supported!
  val Mret = TypeSystem(M"0011000_00010_00000_000_00000_1110011")

  override def ident: String = "priv_m"

  override def instructions: Seq[Instruction] = Seq(
    Mret
  )
}
