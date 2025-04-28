package futurecore

import spinal.core._

object WriteBackType extends SpinalEnum {
  val Alu, Mem, Pc = newElement()
}

case class WriteBackUnit() extends Component {
  val io = new Bundle {
    val memRes = in Bits (32 bits)
    val aluRes = in Bits (32 bits)
    val pcLink = in UInt (32 bits)
    val wbType = in(WriteBackType())
    val wbRes = out Bits (32 bits)
  }

  val result = Bits(32 bits)

  switch(io.wbType) {
    is(WriteBackType.Alu) {
      result := io.aluRes
    }
    is(WriteBackType.Mem) {
      result := io.memRes
    }
    is(WriteBackType.Pc) {
      result := io.pcLink.asBits
    }
  }

  io.wbRes := result
}
