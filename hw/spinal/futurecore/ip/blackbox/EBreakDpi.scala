package futurecore.ip.blackbox

import spinal.core._

class ebreak_dpi extends BlackBox {
  val io = new Bundle {
    val clk = in Bool ()
    val resetn = in Bool ()
    val valid = in Bool ()
    val status = in Bits (32 bits)
  }
  noIoPrefix()
  mapClockDomain(clock = io.clk, reset = io.resetn, resetActiveLevel = LOW)
  addRTLPath("hw/verilog/ebreak_dpi.sv")
}
