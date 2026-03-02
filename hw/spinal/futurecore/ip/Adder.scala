package futurecore.ip

import spinal.core._

class Adder extends Component {
  val io = new Bundle {
    val inA = in port SInt(32 bits)
    val inB = in port SInt(32 bits)
    val inC = in port Bool()

    val outS = out port SInt(32 bits)
    val outC = out port Bool()

    val flagOverflow = out port Bool()
    val flagSign = out port Bool()
    val flagZero = out port Bool()
    val flagCarry = out port Bool()
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
  result := sumWithCarry.resize(32 bits).asSInt
  sign := result.sign

  io.flagCarry := carryIn ^ carryOut
  io.flagOverflow := (aSign === bSign) && (sign =/= aSign)
  io.flagSign := sign
  io.flagZero := !result.orR

  io.outS := result
  io.outC := carryOut
}
