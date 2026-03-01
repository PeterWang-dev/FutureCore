package futurecore.fetch

import spinal.core._
import futurecore.ip.blackbox.rom_dpi

class InstructionMemory extends Component {
  val io = new Bundle {
    val instAddr = in port UInt(32 bits)
    val inst = out port Bits(32 bits)
  }

  val instMem = new rom_dpi

  instMem.io.valid := True
  instMem.io.raddr := io.instAddr
  io.inst := instMem.io.rdata
}
