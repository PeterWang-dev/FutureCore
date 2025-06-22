package futurecore.lib

import spinal.core._
import futurecore.blackbox.regs_dpi

case class Debugger(rv32e: Boolean) extends Component {
  val io = new Bundle {
    val npc = in UInt (32 bits)
    val inst = in Bits (32 bits)
    val gprs = in Vec (Bits(32 bits), if (rv32e) 16 else 32)
  }

  val regsDpi = new regs_dpi
  regsDpi.io.npc := io.npc
  regsDpi.io.gprs := io.gprs.asBits
}
