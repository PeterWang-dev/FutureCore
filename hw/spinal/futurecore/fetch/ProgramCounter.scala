package futurecore.fetch

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

class ProgramCounter(resetVector: UInt) extends Component {
  val io = new Bundle {
    val directWriteEnable = in port Bool()
    val targetAddr = in port UInt(32 bits)
    val instAddr = out port UInt(32 bits)
  }

  val pcReg = Reg(UInt(32 bits))
  val pcNeigh = pcReg + 4

  val pcFsm = new StateMachine {
    val reset = makeInstantEntry
    val gen = new State

    reset
      .whenIsActive {
        pcReg := resetVector
        goto(gen)
      }

    gen.whenIsActive {
      pcReg := Mux(io.directWriteEnable, io.targetAddr, pcNeigh)
    }
  }

  io.instAddr := pcReg
}
