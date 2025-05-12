package futurecore.lib

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

  val ret = regFile(U(10).resized)

  io.dataRe1 := Mux(io.addrRe1 === 0, B(0), regFile(io.addrRe1.resized))
  io.dataRe2 := Mux(io.addrRe2 === 0, B(0), regFile(io.addrRe2.resized))

  when(io.writeEn && io.addrWr =/= 0) {
    regFile(io.addrWr.resized) := io.dataWr
  }
}
