package futurecore.debug

import spinal.core._

import futurecore.ip.blackbox.dpi.debug_dpi

class DebugProbe extends Component {
  val io = new Bundle {
    val inPc = in port UInt(32 bits)
    val inInst = in port Bits(32 bits)
    val inGprs = in port Vec(Bits(32 bits), 32)
  }

  val dbg = new debug_dpi(gprNum = 32, gprWidth = 32)

  dbg.io.s.pc_i := io.inPc
  dbg.io.s.inst_i := io.inInst
  // Reverse registers to make the unified signal little-endian to pass legally
  dbg.io.s.gprs_i := io.inGprs.reverse.reduce(_ ## _)
}
