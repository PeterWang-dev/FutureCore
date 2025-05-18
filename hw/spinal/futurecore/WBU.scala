package futurecore

import futurecore.blackbox.ebreak_dpi
import spinal.core._
import spinal.lib._

object WBU {
  object WriteBackType extends SpinalEnum {
    val Alu, Mem, Pc, Ebreak = newElement()
  }

  case class In() extends Bundle {
    val memRes = Bits(32 bits)
    val aluRes = Bits(32 bits)
    val pcLink = UInt(32 bits)
    val retStatus = Bits(32 bits)
    in(memRes, aluRes, pcLink, retStatus)
  }

  case class Out() extends Bundle {
    val wbRes = Bits(32 bits)
    out(wbRes)
  }

  case class Ctrl() extends Bundle with IMasterSlave {
    val wbType = WriteBackType()
    override def asMaster(): Unit = {
      out(wbType)
    }
  }
}

case class WBU() extends Component {
  import WBU._

  val io = new Bundle {
    val input = In()
    val ctrl = slave(Ctrl())
    val output = Out()
  }

  val ebreak = new ebreak_dpi
  val result = Bits(32 bits)

  ebreak.io.valid := io.ctrl.wbType === WriteBackType.Ebreak
  ebreak.io.status := io.input.retStatus

  switch(io.ctrl.wbType) {
    is(WriteBackType.Alu) {
      result := io.input.aluRes
    }
    is(WriteBackType.Mem) {
      result := io.input.memRes
    }
    is(WriteBackType.Pc) {
      result := io.input.pcLink.asBits
    }
    is(WriteBackType.Ebreak) {
      result := io.input.retStatus
    }
  }

  io.output.wbRes := result
}
