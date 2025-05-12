package futurecore

import futurecore.lib.Alu
import spinal.core._
import spinal.lib._

object EXU {
  case class In() extends Bundle {
    val rs1 = Bits(32 bits)
    val rs2 = Bits(32 bits)
    val pc = UInt(32 bits)
    val imm = SInt(32 bits)
    in(rs1, rs2, pc, imm)
  }

  case class Out() extends Bundle {
    val aluRes = Bits(32 bits)
    out(aluRes)
  }

  case class Ctrl() extends Bundle with IMasterSlave {
    val pcSel = Bool()
    val zeroSel = Bool()
    val immSel = Bool()
    val aluOpType = Alu.AluOp()
    override def asMaster(): Unit = {
      out(pcSel, zeroSel, immSel, aluOpType)
    }
  }
}

case class EXU() extends Component {
  import EXU._

  val io = new Bundle {
    val input = In()
    val ctrl = slave(Ctrl())
    val output = Out()
  }

  val alu = Alu()

  alu.io.inA := Mux(io.ctrl.zeroSel, B(0), Mux(io.ctrl.pcSel, io.input.pc.asBits, io.input.rs1))
  alu.io.inB := Mux(io.ctrl.immSel, io.input.imm.asBits, io.input.rs2)
  alu.io.op := io.ctrl.aluOpType
  io.output.aluRes := alu.io.outRes
}
