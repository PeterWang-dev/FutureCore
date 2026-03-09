package futurecore.fetch

import spinal.core._

object BranchTargeter {
  object BranchMode extends SpinalEnum {
    val PcReletive, Displacement = newElement()
  }
}

class BranchTargeter extends Component {
  import BranchTargeter._

  val io = new Bundle {
    val inSelMode = in port BranchMode()
    val inBaseReg = in port Bits(32 bits)
    val inPc = in port UInt(32 bits)
    val inOffsetImm = in port SInt(32 bits)
    val outTarget = out port UInt(32 bits)
  }

  val base = io.inSelMode.mux(
    BranchMode.PcReletive   -> io.inPc,
    BranchMode.Displacement -> io.inBaseReg.asUInt
  )

  val targetUnaligned = (base.asSInt + io.inOffsetImm).asUInt

  val AlignMask = ~U"32'h1"
  val targetAligned =
    (io.inSelMode === BranchMode.Displacement) ?
      (targetUnaligned & AlignMask) |
      targetUnaligned

  io.outTarget := targetAligned
}
