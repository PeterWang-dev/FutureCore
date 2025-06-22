package futurecore.lib

import spinal.core._

case class RegFile(rv32e: Boolean = true, hasDebugSignals: Boolean = true) extends Component {
  val io = new Bundle {
    val writeEn = in Bool ()
    val addrWr = in UInt (5 bits)
    val dataWr = in Bits (32 bits)
    val addrRe1 = in UInt (5 bits)
    val dataRe1 = out Bits (32 bits)
    val addrRe2 = in UInt (5 bits)
    val dataRe2 = out Bits (32 bits)
    val ret = out Bits (32 bits)
    val gprsDbg = ifGen(hasDebugSignals)(out Vec (Bits(32 bits), if (rv32e) 16 else 32))
  }

  val gprs = Mem(Bits(32 bits), if (rv32e) 16 else 32)
  gprs.setTechnology(registerFile)

  io.dataRe1 := Mux(io.addrRe1 === 0, B(0), gprs(io.addrRe1.resized))
  io.dataRe2 := Mux(io.addrRe2 === 0, B(0), gprs(io.addrRe2.resized))
  io.ret := gprs(U(10))

  when(io.writeEn && io.addrWr =/= 0) {
    gprs(io.addrWr.resized) := io.dataWr
  }

  if (hasDebugSignals) {
    for (i <- 0 until (if (rv32e) 16 else 32)) {
      io.gprsDbg(i) := gprs(U(i).resized)
    }
  }
}
