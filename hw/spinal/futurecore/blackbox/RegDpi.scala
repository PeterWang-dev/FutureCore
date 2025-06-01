package futurecore.blackbox

import spinal.core._
import spinal.lib.io.InOutVecToBits

class reg16_dpi extends BlackBox {
  val io = new Bundle {
    val clk = in Bool ()
    val resetn = in Bool ()
    val gprs = in Bits (16 * 32 bits)
  }

  noIoPrefix()
  mapClockDomain(clock = io.clk, reset = io.resetn, resetActiveLevel = LOW)
  addRTLPath("hw/verilog/reg_dpi.sv")
}

class reg32_dpi extends BlackBox {
  val io = new Bundle {
    val clk = in Bool ()
    val resetn = in Bool ()
    val gprs = in Bits(32 * 32 bits)
  }

  noIoPrefix()
  mapClockDomain(clock = io.clk, reset = io.resetn, resetActiveLevel = LOW)
  addRTLPath("hw/verilog/reg_dpi.sv")
}
