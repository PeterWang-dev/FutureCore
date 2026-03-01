package futurecore.fetch

import spinal.core._

class BranchTargeter extends Component {
  val io = new Bundle {
    val enableBase = in port Bool()
    val inBaseReg = in port Bits(32 bits)
    val inPc = in port UInt(32 bits)
    val inOffsetImm = in port SInt(32 bits)
    val outTarget = out port UInt(32 bits)
  }

  val baseU = io.enableBase ? io.inBaseReg.asUInt | io.inPc

  val targetS = baseU.asSInt + io.inOffsetImm
  val targetU = targetS.asUInt

  val AlignMask = ~U"32'h1"

  io.outTarget := io.enableBase ? (targetU & AlignMask) | targetU
}
