package futurecore.blackbox

import spinal.core._

class ebreak_dpi extends BlackBox {
  val io = new Bundle {
    val valid = in Bool ()
    val status = out Bits (32 bits)
  }
  noIoPrefix()
  addRTLPath("hw/verilog/ebreak_dpi.sv")
}
