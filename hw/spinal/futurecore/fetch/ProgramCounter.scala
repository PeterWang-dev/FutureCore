package futurecore.fetch

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

class ProgramCounter(resetVector: BigInt) extends Component {
  val io = new Bundle {
    val inDirectWriteEnable = in port Bool()
    val inTargetAddr = in port UInt(32 bits)
    val outInstAddr = out port UInt(32 bits)
  }

  val pcReg = Reg(UInt(32 bits))
  val pcNeigh = pcReg + 4

  val pcFsm = new StateMachine {
    val reset = makeInstantEntry
    val gen = new State

    reset
      .whenIsActive {
        pcReg := U(resetVector, 32 bits)
        goto(gen)
      }

    gen.whenIsActive {
      pcReg := Mux(io.inDirectWriteEnable, io.inTargetAddr, pcNeigh)
    }
  }

  io.outInstAddr := pcReg
}
