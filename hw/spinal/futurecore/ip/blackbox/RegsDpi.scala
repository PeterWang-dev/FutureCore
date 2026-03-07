package futurecore.blackbox

import spinal.core._
import spinal.lib.io.InOutVecToBits

class regs_dpi(rv32e: Boolean = true) extends BlackBox {
  val io = new Bundle {
    val clk = in Bool ()
    val resetn = in Bool ()
    val npc = in UInt (32 bits)
    val gprs = in Bits (if (rv32e) 16 * 32 bits else 32 * 32 bits)
  }
  addGeneric("NR_REGS", if (rv32e) 16 else 32)
  noIoPrefix()
  mapClockDomain(clock = io.clk, reset = io.resetn, resetActiveLevel = LOW)
  addRTLPath("hw/verilog/regs_dpi.sv")
}
