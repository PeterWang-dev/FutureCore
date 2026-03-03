package futurecore.execute

import spinal.core._

import futurecore.ip.Adder

object IntAlu {
  object AluOp extends SpinalEnum {
    val Add, Sub, Xor, Or, And = newElement()
    val ShiftLeftLogic, ShiftRightLogic, ShiftRightArith = newElement()
    val EqualTo, NotEqual = newElement()
    val LessThan, GreaterEqual = newElement()
    val LessThanUnsigned, GreaterEqualUnsigned = newElement()
  }
}

class IntAlu extends Component {
  import IntAlu._

  val io = new Bundle {
    val inA = in port SInt(32 bits)
    val inB = in port SInt(32 bits)
    val selOp = in port AluOp()
    val outRes = out port SInt(32 bits)
  }

  val inA = io.inA
  val inB = io.inB

  // Use adder to implement Add, Sub and Comparisons (via Sub)
  val adder = new Adder

  adder.io.inA := inA
  adder.io.inB := Mux(io.selOp === AluOp.Add, inB, ~inB)
  adder.io.inC := (io.selOp =/= AluOp.Add)

  /*
    Consider situations when A < B:

      1.  A, B are signed int:

          -   A > 0, B > 0, A - B < 0
          -   A < 0, B < 0, A - B < 0
          -   A < 0, B > 0, A < B, A - B < 0 may underflow (leads to positive)

          Both underflow and overflow imply different carry of result and sign.
          So iff overflow flag is different with sign flag, A < B.

      2.  A, B are unsigned int:
          Iff A - B needs borrow, A < B.
   */
  val isLessThan = adder.io.flagOverflow ^ adder.io.flagSign
  val isLessThanUnsigned = !adder.io.flagCarry
  val isEqual = adder.io.flagZero

  val isGreaterEqual = !isLessThan
  val isGreaterEqualUnsigned = !isLessThanUnsigned
  val isNotEqual = !isEqual

  val adderResult = adder.io.outS

  val shiftAmount = inB(4 downto 0).asUInt // max shift amount is log2(width)
  val sllResult = (inA << shiftAmount).resized
  val srlResult = (inA.asUInt >> shiftAmount).asSInt
  val sraResult = (inA >> shiftAmount)

  val xorResult = inA ^ inB
  val orResult = inA | inB
  val andResult = inA & inB

  // Result constants for comparison operations (avoid sign extension issue)
  val one = S(1, 32 bits)
  val zero = S(0, 32 bits)

  io.outRes := io.selOp.mux(
    AluOp.Add                  -> adderResult,
    AluOp.Sub                  -> adderResult,
    AluOp.Xor                  -> xorResult,
    AluOp.Or                   -> orResult,
    AluOp.And                  -> andResult,
    AluOp.ShiftLeftLogic       -> sllResult,
    AluOp.ShiftRightLogic      -> srlResult,
    AluOp.ShiftRightArith      -> sraResult,
    AluOp.EqualTo              -> (isEqual ? one | zero),
    AluOp.NotEqual             -> (isNotEqual ? one | zero),
    AluOp.LessThan             -> (isLessThan ? one | zero),
    AluOp.GreaterEqual         -> (isGreaterEqual ? one | zero),
    AluOp.LessThanUnsigned     -> (isLessThanUnsigned ? one | zero),
    AluOp.GreaterEqualUnsigned -> (isGreaterEqualUnsigned ? one | zero)
  )
}
