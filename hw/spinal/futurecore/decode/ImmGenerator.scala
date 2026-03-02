package futurecore.decode

import spinal.core._

import futurecore.riscv.Rvi.{IImm, SImm, BImm, UImm, JImm}
import futurecore.misc.TypeExtensions.BitsSextExtension

object ImmGenerator {
  object ImmMode extends SpinalEnum {
    val I, S, B, U, J = newElement()
  }
}

class ImmGenerator extends Component {
  import ImmGenerator._

  val io = new Bundle {
    val inInst = in port Bits(32 bits)
    val selMode = in port ImmMode()
    val outImm = out port SInt(32 bits)
  }

  val inst = io.inInst

  val iImm = IImm.extract(inst).sext
  val sImm = SImm.extract(inst).sext
  val bImm = (BImm.extract(inst) ## U"1'b0").sext
  val uImm = (UImm.extract(inst) ## U"12'b0").sext
  val jImm = (JImm.extract(inst) ## U"1'b0").sext

  // Select immediate based on mode
  io.outImm := io.selMode.mux {
    ImmMode.I -> iImm
    ImmMode.S -> sImm
    ImmMode.B -> bImm
    ImmMode.U -> uImm
    ImmMode.J -> jImm
  }
}
