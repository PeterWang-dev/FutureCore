package futurecore.writeback

import spinal.core._
import futurecore.ip.blackbox.dpi.ebreak_dpi

class EbreakHandler extends Component {
  val io = new Bundle {
    val inEnable = in port Bool()
    val inReturnStatus = in port SInt(32 bits)
  }

  val dpi = new ebreak_dpi

  dpi.io.s.valid_i := io.inEnable
  dpi.io.s.status_i := io.inReturnStatus.asBits
}
