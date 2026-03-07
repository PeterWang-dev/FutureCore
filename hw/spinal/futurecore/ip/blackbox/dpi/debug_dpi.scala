package futurecore.ip.blackbox.dpi

import spinal.core._

class debug_dpi(gprNum: Int, gprWidth: Int) extends BlackBox {
  val generic = new Generic {
    val GPR_NUM = debug_dpi.this.gprNum
    val GPR_WIDTH = debug_dpi.this.gprWidth
  }

  val io = new Bundle {
    val clk_i = in port Bool()
    val reset_ni = in port Bool()
    val s = new Bundle {
      val pc_i = in port UInt(32 bits)
      val inst_i = in port Bits(32 bits)
      val regs_i = in port Bits(gprNum * gprWidth bits)
    }
  }

  noIoPrefix()

  mapCurrentClockDomain(io.clk_i, io.reset_ni, resetActiveLevel = LOW)

  addRTLPath("hw/verilog/debug_dpi.sv")
}
