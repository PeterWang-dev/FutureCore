package futurecore.ip.blackbox

import spinal.core._

class rom_dpi extends BlackBox {
  val io = new Bundle {
    val clk = in Bool ()
    val resetn = in Bool ()
    val valid = in Bool ()
    val raddr = in UInt (32 bits)
    val rdata = out Bits (32 bits)
  }

  noIoPrefix()
  mapClockDomain(clock = io.clk, reset = io.resetn, resetActiveLevel = LOW)
  addRTLPath("hw/verilog/mem_dpi.sv")
}

class ram_dpi extends BlackBox {
  val io = new Bundle {
    val clk = in Bool ()
    val resetn = in Bool ()
    val valid = in Bool ()
    val raddr = in UInt (32 bits)
    val rdata = out Bits (32 bits)
    val wen = in Bool ()
    val waddr = in UInt (32 bits)
    val wdata = in Bits (32 bits)
    val wmask = in Bits (8 bits)
  }

  noIoPrefix()
  mapClockDomain(clock = io.clk, reset = io.resetn, resetActiveLevel = LOW)
  addRTLPath("hw/verilog/mem_dpi.sv")
}
