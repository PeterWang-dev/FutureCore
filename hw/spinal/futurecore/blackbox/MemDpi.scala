package futurecore.blackbox

import spinal.core._

class rom_dpi extends BlackBox {
  val io = new Bundle {
    val valid = in Bool ()
    val raddr = in UInt (32 bits)
    val rdata = out Bits (32 bits)
  }

  noIoPrefix()
  addRTLPath("hw/verilog/mem_dpi.sv")
}

class ram_dpi extends BlackBox {
  val io = new Bundle {
    val valid = in Bool ()
    val raddr = in UInt (32 bits)
    val rdata = out Bits (32 bits)
    val wen = in Bool ()
    val waddr = in UInt (32 bits)
    val wdata = in Bits (32 bits)
    val wmask = in Bits (8 bits)
  }

  noIoPrefix()
  addRTLPath("hw/verilog/mem_dpi.sv")
}
