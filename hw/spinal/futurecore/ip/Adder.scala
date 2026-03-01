package futurecore.ip

import spinal.core._

class Adder extends Component {
  val io = new Bundle {
    val inA = SInt(32 bits)
    val inB = SInt(32 bits)
    val inC = Bool()

    val outS = SInt(32 bits)
    val outC = Bool()

    val flagOverflow = Bool()
    val flagSign = Bool()
    val flagZero = Bool()
    val flagCarry = Bool()
  }

  val aUnsigned = io.inA.asUInt
  val aSign = io.inA.sign
  val bUnsigned = io.inB.asUInt
  val bSign = io.inB.sign
  val carryIn = io.inC

  val sumWithCarry = UInt(33 bits)
  val result = SInt(32 bits)
  val sign = Bool()
  val carryOut = Bool()

  sumWithCarry := (aUnsigned +^ bUnsigned) + carryIn.asUInt

  carryOut := sumWithCarry.msb
  result := sumWithCarry.resized.asSInt
  sign := result.sign

  io.flagCarry := carryIn ^ carryOut
  io.flagOverflow := (aSign === bSign) && (sign =/= aSign)
  io.flagSign := sign
  io.flagZero := !result.orR

  io.outS := result
  io.outC := carryOut
}
