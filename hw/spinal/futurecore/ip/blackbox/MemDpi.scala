package futurecore.ip.blackbox

import spinal.core._

class rom_dpi extends BlackBox {
  val io = new Bundle {
    val clk = in port Bool()
    val resetn = in port Bool()
    val valid = in port Bool()
    val raddr = in port UInt(32 bits)
    val rdata = out port Bits(32 bits)
  }

  noIoPrefix()
  mapClockDomain(clock = io.clk, reset = io.resetn, resetActiveLevel = LOW)
  addRTLPath("hw/verilog/mem_dpi.sv")
}

class ram_dpi extends BlackBox {
  val io = new Bundle {
    val clk = in port Bool()
    val resetn = in port Bool()
    val valid = in port Bool()
    val raddr = in port UInt(32 bits)
    val rdata = out port Bits(32 bits)
    val wen = in port Bool()
    val waddr = in port UInt(32 bits)
    val wdata = in port Bits(32 bits)
    val wmask = in port Bits(8 bits)
  }

  noIoPrefix()
  mapClockDomain(clock = io.clk, reset = io.resetn, resetActiveLevel = LOW)
  addRTLPath("hw/verilog/mem_dpi.sv")
}
