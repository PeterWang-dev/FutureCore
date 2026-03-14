package futurecore.fetch

import spinal.core._

object BranchTargeter {
  object BranchMode extends SpinalEnum {
    val PcReletive, Displacement, Trap, TrapReturn = newElement()
  }
}

class BranchTargeter extends Component {
  import BranchTargeter._

  val io = new Bundle {
    val inSelMode = in port BranchMode()
    val inBaseReg = in port Bits(32 bits)
    val inPc = in port UInt(32 bits)
    val inTrap = in port UInt(32 bits)
    val inEpc = in port UInt(32 bits)
    val inOffsetImm = in port SInt(32 bits)
    val outTarget = out port UInt(32 bits)
  }

  val base = io.inSelMode.mux(
    BranchMode.PcReletive   -> io.inPc,
    BranchMode.Displacement -> io.inBaseReg.asUInt,
    BranchMode.Trap         -> io.inTrap,
    BranchMode.TrapReturn   -> io.inEpc
  )

  val doDisplace = io.inSelMode.mux(
    BranchMode.PcReletive   -> True,
    BranchMode.Displacement -> True,
    BranchMode.Trap         -> False,
    BranchMode.TrapReturn   -> False
  )

  val targetRaw = base
  val targetDisplacedUnaligned = (base.asSInt + io.inOffsetImm).asUInt

  val AlignMask = ~U"32'h1"
  val targetDisplacedAligned =
    (io.inSelMode === BranchMode.Displacement) ?
      (targetDisplacedUnaligned & AlignMask) |
      targetDisplacedUnaligned

  io.outTarget := doDisplace ? targetDisplacedAligned | targetRaw
}
