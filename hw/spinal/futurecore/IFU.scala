package futurecore

import futurecore.blackbox.rom_dpi
import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

object IFU {
  case class In() extends Bundle {
    val offsetRel = SInt(32 bits)
    val targetAbs = UInt(32 bits)
    in(offsetRel, targetAbs)
  }

  case class Out() extends Bundle {
    val pcCurr = UInt(32 bits)
    val pcNeigh = UInt(32 bits)
    val inst = Bits(32 bits)
    out(pcCurr, pcNeigh, inst)
  }

  case class Ctrl() extends Bundle with IMasterSlave {
    val pcAbsSel = Bool()
    val pcRelSel = Bool()
    override def asMaster(): Unit = {
      out(pcAbsSel, pcRelSel)
    }
  }
}

case class IFU() extends Component {
  import IFU._

  val io = new Bundle {
    val input = In()
    val output = Out()
    val ctrl = slave(Ctrl())
  }

  val instMem = new rom_dpi

  val pcGen = new Area {
    val pcReg = Reg(UInt(32 bits)) init (U"32'h8000_0000")
    val immTarget = io.input.targetAbs & ~U"32'b1" // clear the LSB (2 bytes aligned)
    val immOffset = io.input.offsetRel
    val pcNeigh = pcReg + 4
    val pcNext = Mux(io.ctrl.pcRelSel, U(pcReg.asSInt + immOffset), pcNeigh)

    val pcFsm = new StateMachine {
      val reset = makeInstantEntry
      val gen = new State

      reset
        .whenIsActive {
          pcReg := U"32'h8000_0000"
          goto(gen)
        }

      gen.whenIsActive {
        pcReg := Mux(io.ctrl.pcAbsSel, immTarget, pcNext)
      }
    }
  }

  instMem.io.valid := Mux(clockDomain.isResetActive, False, True)
  instMem.io.raddr := pcGen.pcReg
  io.output.inst := instMem.io.rdata

  io.output.pcCurr := pcGen.pcReg
  io.output.pcNeigh := pcGen.pcNeigh
}
