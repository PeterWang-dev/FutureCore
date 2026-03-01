package futurecore.fetch

import spinal.core._
import spinal.lib.misc.plugin._

import futurecore.riscv.Rvi
import futurecore.decode.CtrlService
import futurecore.decode.CtrlService.CtrlDef
import futurecore.execute.ExecutePlugin
import futurecore.decode.DecodePlugin

class FetchPlugin extends FiberPlugin {
  val logic = during setup new Area {
    val cs = host[CtrlService]
    val dp = host[DecodePlugin]
    val ep = host[ExecutePlugin]
    val buildBefore = retains(cs.ctrlLock)

    awaitBuild()

    val pc = new ProgramCounter(U"32'h8000_0000")
    val bt = new BranchTargeter
    val im = new InstructionMemory

    val pcWriteDef = new CtrlDef(Bool(), False)
      .setWhen(True, Rvi.Jal, Rvi.Jalr)
      .setWhen(True, Rvi.Beq, Rvi.Bge, Rvi.Bgeu, Rvi.Blt, Rvi.Bltu, Rvi.Bne)
    cs.registerCtrlSignal(pcWriteDef)

    val branchBaseDef = new CtrlDef(Bool(), False)
      .setWhen(True, Rvi.Jalr)
    cs.registerCtrlSignal(branchBaseDef)

    buildBefore.release()

    bt.io.inPc := pc.io.instAddr
    bt.io.inBaseReg := dp.getRs1() // ! Blocked Here, request DecodePlugin finish
    bt.io.inOffsetImm := dp.getImm()
    bt.io.enableBase := cs.getCtrlSignal(branchBaseDef)

    pc.io.targetAddr := bt.io.outTarget
    pc.io.directWriteEnable := cs.getCtrlSignal(pcWriteDef) & ep.getBranchCond()

    im.io.instAddr := pc.io.instAddr
  }

  def getInstruction(): Bits = logic.get.im.io.inst
}
