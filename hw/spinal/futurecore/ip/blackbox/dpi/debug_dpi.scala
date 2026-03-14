package futurecore.ip.blackbox.dpi

import spinal.core._

class debug_dpi(gprNum: Int, gprWidth: Int) extends BlackBox {
  val generic = new Generic {
    val GPR_NUM = debug_dpi.this.gprNum
    val GPR_WIDTH = debug_dpi.this.gprWidth
  }

  val io = new Bundle {
    val clk_i = in port Bool()
    val rst_ni = in port Bool()
    val s = new Bundle {
      val pc_i = in port UInt(32 bits)
      val inst_i = in port Bits(32 bits)
      val gprs_i = in port Bits(gprNum * gprWidth bits)
      val mstatus_i = in port Bits(32 bits)
      val mtvec_i = in port Bits(32 bits)
      val mepc_i = in port Bits(32 bits)
      val mcause_i = in port Bits(32 bits)
    }
  }

  noIoPrefix()

  mapCurrentClockDomain(io.clk_i, io.rst_ni, resetActiveLevel = LOW)

  addRTLPath("hw/verilog/debug_dpi.sv")
}
