package futurecore.blackbox

import spinal.core._

class csr_dpi extends BlackBox {
  val io = new Bundle {
    val valid = in Bool ()
  }
  noIoPrefix()
  addRTLPath("hw/verilog/csr_dpi.sv")
}
