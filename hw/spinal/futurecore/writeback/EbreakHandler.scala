package futurecore.writeback

import spinal.core._
import futurecore.ip.blackbox.dpi.ebreak_dpi

class EbreakHandler extends Component {
  val io = new Bundle {
    val enable = in port Bool()
    val inReturnStatus = in port SInt(32 bits)
  }

  val dpi = new ebreak_dpi

  dpi.io.s.valid_i := io.enable
  dpi.io.s.status_i := io.inReturnStatus.asBits
}
