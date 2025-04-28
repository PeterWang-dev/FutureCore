package futurecore

import spinal.core._
import futurecore.blackbox.rom_dpi
import spinal.lib.fsm._

case class InstFetchUnit() extends Component {
  val io = new Bundle {
    val offsetRel = in SInt (32 bits)
    val targetAbs = in UInt (32 bits)
    val pcAbsSel = in Bool ()
    val pcRelSel = in Bool ()
    val pcCurr = out UInt (32 bits)
    val pcNeigh = out UInt (32 bits)
    val inst = out Bits (32 bits)
  }

  val instMem = new rom_dpi

  val pcGen = new Area {
    val pcReg = Reg(UInt(32 bits)) init (U"32'h8000_0000")
    val immTarget = io.targetAbs & ~U"32'b1" // clear the LSB (2 bytes aligned)
    val immOffset = io.offsetRel
    val pcNeigh = pcReg + 4
    val pcNext = Mux(io.pcRelSel, U(pcReg.asSInt + immOffset), pcNeigh)

    val pcFsm = new StateMachine {
      val reset = makeInstantEntry
      val gen = new State

      reset
        .whenIsActive {
          pcReg := U"32'h8000_0000"
          goto(gen)
        }

      gen.whenIsActive {
        pcReg := Mux(io.pcAbsSel, immTarget, pcNext)
      }
    }
  }

  instMem.io.valid := Mux(clockDomain.isResetActive, False, True)
  instMem.io.raddr := pcGen.pcReg
  io.inst := instMem.io.rdata

  io.pcCurr := pcGen.pcReg
  io.pcNeigh := pcGen.pcNeigh
}
