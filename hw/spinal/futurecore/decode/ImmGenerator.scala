package futurecore.decode

import spinal.core._

import futurecore.riscv.Rv32i.{IImm, SImm, BImm, UImm, JImm}
import futurecore.riscv.Zicsr.{MicroImm => CsrMicroImm}
import futurecore.misc.TypeExtensions.{BitsSextExtension, BitsZeroExtension}

object ImmGenerator {
  object ImmMode extends SpinalEnum {
    val I, S, B, U, J, CsrU = newElement()
  }
}

class ImmGenerator extends Component {
  import ImmGenerator._

  val io = new Bundle {
    val inInst = in port Bits(32 bits)
    val inSelMode = in port ImmMode()
    val outImm = out port SInt(32 bits)
  }

  val inst = io.inInst

  val iImm = IImm.extract(inst).sext
  val sImm = SImm.extract(inst).sext
  val bImm = (BImm.extract(inst) ## U"1'b0").sext
  val uImm = (UImm.extract(inst) ## U"12'b0").sext
  val jImm = (JImm.extract(inst) ## U"1'b0").sext

  val zicsrMicroImm = (CsrMicroImm.extract(inst)).zext

  // Select immediate based on mode
  io.outImm := io.inSelMode.mux(
    ImmMode.I -> iImm,
    ImmMode.S -> sImm,
    ImmMode.B -> bImm,
    ImmMode.U -> uImm,
    ImmMode.J -> jImm,
    // Although uImm is UInt and zero-extended,to make type system happy,
    // still needs to convert to SInt. This will not cause any problem as
    // binary representation keep untouched.
    ImmMode.CsrU -> zicsrMicroImm.asSInt
  )
}
