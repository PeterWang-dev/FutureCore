package futurecore.decode

import spinal.core._

class IntRegfile extends Component {
  val io = new Bundle {
    val inAddrReadA = in port UInt(5 bits)
    val outDataReadA = out port Bits(32 bits)

    val inAddrReadB = in port UInt(5 bits)
    val outDataReadB = out port Bits(32 bits)

    val inEnableWrite = in port Bool()
    val inAddrWrite = in port UInt(5 bits)
    val inDataWrite = in port Bits(32 bits)

    val outSpecialRet = out port Bits(32 bits)
    val dbgIntRegisters = out port Vec(Bits(32 bits), 32)
  }

  val regfile = Mem(Bits(32 bits), 32)
  regfile.setTechnology(registerFile)

  io.outDataReadA := Mux(io.inAddrReadA === 0, B(0), regfile(io.inAddrReadA.resized))
  io.outDataReadB := Mux(io.inAddrReadB === 0, B(0), regfile(io.inAddrReadB.resized))

  when(io.inEnableWrite && io.inAddrWrite =/= 0) {
    regfile(io.inAddrWrite.resized) := io.inDataWrite
  }

  // Return value is conventionally stored in x10 (a0)
  val returnIndex = U(10, 5 bits)
  io.outSpecialRet := regfile(returnIndex.resized)

  io.dbgIntRegisters.zipWithIndex.foreach { case (b, i) => b := regfile(U(i).resized) }
}
