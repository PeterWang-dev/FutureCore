package futurecore.lib

import spinal.core._
import futurecore.Config

object ImmGen {
  object ImmType extends SpinalEnum {
    val I, S, B, U, J = newElement()
  }
}

case class ImmGen() extends Component {
  import ImmGen.ImmType

  val io = new Bundle {
    val inst = in Bits (32 bits)
    val immType = in(ImmGen.ImmType())
    val imm = out SInt (32 bits)
  }

  switch(io.immType) {
    is(ImmType.I) {
      io.imm := io.inst(31 downto 20).asSInt.resize(32 bits)
    }
    is(ImmType.S) {
      io.imm := (io.inst(31 downto 25) ## io.inst(11 downto 7)).asSInt.resize(32 bits)
    }
    is(ImmType.B) {
      io.imm := (io.inst(31) ## io.inst(7) ## io.inst(30 downto 25) ## io.inst(11 downto 8) ## U"1'b0").asSInt
        .resize(32 bits)
    }
    is(ImmType.U) {
      io.imm := (io.inst(31 downto 12) ## B"12'b0").asSInt.resize(32 bits)
    }
    is(ImmType.J) {
      // ! J-type immediate is UInt actually
      io.imm := (io.inst(31) ## io.inst(19 downto 12) ## io.inst(20) ## io.inst(30 downto 21) ## B"1'b0").asSInt
        .resize(32 bits)
    }
  }
}
