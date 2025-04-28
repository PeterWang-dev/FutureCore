package futurecore

import spinal.core._
import futurecore.lib.{AluOp, Alu}

case class ExecuteUnit() extends Component {
  val io = new Bundle {
    // input signals
    val rs1 = in Bits (32 bits)
    val rs2 = in Bits (32 bits)
    val pc = in UInt (32 bits)
    val imm = in SInt (32 bits)
    // control signals
    val pcSel = in Bool ()
    val zeroSel = in Bool ()
    val immSel = in Bool ()
    val aluOpType = in(AluOp())
    // output signals
    val aluRes = out Bits (32 bits)
  }

  val alu = Alu()

  alu.io.inA := Mux(io.zeroSel, B(0), Mux(io.pcSel, io.pc.asBits, io.rs1))
  alu.io.inB := Mux(io.immSel, io.imm.asBits, io.rs2)
  alu.io.op := io.aluOpType
  io.aluRes := alu.io.outRes
}
