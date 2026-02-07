package futurecore.ip

import spinal.core._
import spinal.lib.CountOne

object Alu {
  object AluOp extends SpinalEnum {
    val ADD, SLL, SLT, SLTU, XOR, SRL, OR, AND, SUB, EQ, SRA = newElement()
    defaultEncoding = SpinalEnumEncoding("RISC-V ALU")(
      // func7(5) ## func3(2,0)
      ADD -> 0x0,
      SUB -> 0x8,
      SLL -> 0x1,
      SLT -> 0x2,
      SLTU -> 0x3,
      XOR -> 0x4,
      SRL -> 0x5,
      SRA -> 0xd,
      OR -> 0x6,
      AND -> 0x7,
      EQ -> 0x9
    )
  }
}

case class Alu(dataWidth: Int = 32) extends Component {
  import Alu.AluOp

  val io = new Bundle {
    val inA = in Bits (dataWidth bits)
    val inB = in Bits (dataWidth bits)
    val op = in(AluOp())
    val outRes = out Bits (dataWidth bits)
    val carry = out Bool ()
    val overflow = out Bool ()
    val zero = out Bool ()
  }

  val aBits = io.inA
  val bBits = io.inB
  val result = Bits(dataWidth bits)
  val carry = False
  val overflow = False

  switch(io.op) {
    is(AluOp.ADD, AluOp.SUB) {
      val isSub = io.op.asBits(3)
      val a = aBits.asUInt
      val b = bBits.asUInt
      val c = Bool()
      val r = UInt(dataWidth bits)

      (c, r) := a +^ Mux(isSub, ~b, b) + isSub.asUInt

      val signA = a.msb
      val signB = Mux(isSub, ~b.msb, b.msb)
      val signRes = r.msb

      result := r.asBits
      carry := Mux(isSub, a < b, c)
      overflow := (~(signA ^ signB)) & (signRes ^ signA)
    }
    is(AluOp.SLL) {
      result := aBits |<< bBits(log2Up(dataWidth) - 1 downto 0).asUInt
    }
    is(AluOp.SLT) {
      result := (aBits.asSInt < bBits.asSInt).asBits(dataWidth bits)
    }
    is(AluOp.SLTU) {
      result := (aBits.asUInt < bBits.asUInt).asBits(dataWidth bits)
    }
    is(AluOp.XOR) {
      result := aBits ^ bBits
    }
    is(AluOp.SRL, AluOp.SRA) {
      val shiftAmount = bBits(log2Up(dataWidth) - 1 downto 0).asUInt
      val logicShift = aBits.asUInt >> shiftAmount
      val arithmeticShift = aBits.asSInt >> shiftAmount
      result := Mux(io.op.asBits(3), B(arithmeticShift), B(logicShift))
    }
    is(AluOp.OR) {
      result := aBits | bBits
    }
    is(AluOp.AND) {
      result := aBits & bBits
    }
    is(AluOp.EQ) {
      result := (aBits === bBits).asBits(dataWidth bits)
    }
  }

  io.outRes := result
  io.carry := carry
  io.overflow := overflow
  io.zero := ~result.orR
}
