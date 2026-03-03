package futurecore.execute

import spinal.core._

object SrcSelector {
  object SrcUpMode extends SpinalEnum {
    val RegSrcA, Pc, Zero = newElement()
  }

  object SrcDownMode extends SpinalEnum {
    val RegSrcB, Imm, PcIncrement = newElement()
  }
}

class SrcSelector extends Component {
  import SrcSelector._

  val io = new Bundle {
    val inRs1 = in port Bits(32 bits)
    val inRs2 = in port Bits(32 bits)
    val inImm = in port SInt(32 bits)
    val inPc = in port UInt(32 bits)
    val selUp = in port SrcUpMode()
    val selDown = in port SrcDownMode()
    val outSrcUp = out port SInt(32 bits)
    val outSrcDown = out port SInt(32 bits)
  }

  io.outSrcUp := io.selUp.mux(
    SrcUpMode.RegSrcA -> io.inRs1.asSInt,
    SrcUpMode.Pc      -> io.inPc.asSInt,
    SrcUpMode.Zero    -> S"32'b0"
  )

  io.outSrcDown := io.selDown.mux(
    SrcDownMode.RegSrcB     -> io.inRs2.asSInt,
    SrcDownMode.Imm         -> io.inImm,
    SrcDownMode.PcIncrement -> S"32'h4"
  )
}
