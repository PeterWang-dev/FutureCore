package futurecore.ip.blackbox.dpi

import spinal.core._

class ebreak_dpi extends BlackBox {
  val io = new Bundle {
    val clk_i = in port Bool()
    val rst_ni = in port Bool()
    val s = new Bundle {
      val valid_i = in port Bool()
      val status_i = in port Bits(32 bits)
    }
  }
  noIoPrefix()
  mapClockDomain(clock = io.clk_i, reset = io.rst_ni, resetActiveLevel = LOW)
  addRTLPath("hw/verilog/ebreak_dpi.sv")
}
