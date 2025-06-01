package futurecore.lib

import futurecore.blackbox.{reg16_dpi, reg32_dpi}
import spinal.core._

case class RegFile(rv32e: Boolean = true) extends Component {
  val io = new Bundle {
    val writeEn = in Bool ()
    val addrWr = in UInt (5 bits)
    val dataWr = in Bits (32 bits)
    val addrRe1 = in UInt (5 bits)
    val dataRe1 = out Bits (32 bits)
    val addrRe2 = in UInt (5 bits)
    val dataRe2 = out Bits (32 bits)
  }

  val regFile = Mem(Bits(32 bits), if (rv32e) 16 else 32)
  regFile.setTechnology(registerFile)

  if (rv32e) {
    val regDpi = new reg16_dpi
    for (i <- 0 until 16) {
      regDpi.io.gprs(32 * (i + 1) - 1 downto 32 * i) := regFile(U(i).resized)
    }
  } else {
    val regDpi = new reg32_dpi
    for (i <- 0 until 32) {
      regDpi.io.gprs(32 * (i + 1) - 1 downto 32 * i) := regFile(U(i).resized)
    }
  }

  val ret = regFile(U(10).resized)

  io.dataRe1 := Mux(io.addrRe1 === 0, B(0), regFile(io.addrRe1.resized))
  io.dataRe2 := Mux(io.addrRe2 === 0, B(0), regFile(io.addrRe2.resized))

  when(io.writeEn && io.addrWr =/= 0) {
    regFile(io.addrWr.resized) := io.dataWr
  }
}
