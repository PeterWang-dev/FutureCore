package futurecore.riscv

import spinal.core._

object Privileged extends InstructionSet {
  def TypeSystem(pat: MaskedLiteral) = Instruction(pat, Seq())

  // ! WARNING: Ecall & Ebreak of Rv32i are PRIVILEGED related instructions.
  // !          Should be handled VERY CAREFULLY!
  // !          For now the detailed privilege-related ops are not supported!
  val Mret = TypeSystem(M"0111000_00001_00000_000_00000_1110011")

  override def ident: String = "priv_m"

  override def instructions: Seq[Instruction] = Seq(
    Mret
  )
}
