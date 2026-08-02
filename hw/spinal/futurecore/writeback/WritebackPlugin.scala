package futurecore.writeback

import spinal.core._
import spinal.core.fiber.{Retainer, RetainerGroup}
import spinal.lib.misc.plugin.FiberPlugin

import futurecore.decode.CtrlService
import futurecore.decode.CtrlService.CtrlDef
import futurecore.riscv.{Rv32i, Zicsr}
import futurecore.execute.ExecutePlugin

class WritebackPlugin extends FiberPlugin {
  import CommitSelector.CommitSource

  val setup = during setup new Area {
    val cs = host[CtrlService]
    val ep = host[ExecutePlugin]
    val buildBefore = retains(cs.ctrlLock)
  }

  val logic = during build new Area {
    val cs = setup.get.cs
    val buildBefore = setup.get.buildBefore

    val com = new CommitSelector
    val ebreak = new EbreakHandler

    val commitSrcDef = CtrlDef(CommitSource(), CommitSource.AluResults)
      .setWhen(CommitSource.Memory, Rv32i.Lb, Rv32i.Lh, Rv32i.Lw, Rv32i.Lbu, Rv32i.Lhu)
      .setWhen(CommitSource.CsrValue, Zicsr.instructions)
    cs.registerCtrlSignal(commitSrcDef)

    val isEbreakDef = CtrlDef(Bool(), False).setWhen(True, Rv32i.Ebreak)
    cs.registerCtrlSignal(isEbreakDef)

    buildBefore.release()

    val aluResult = SInt(32 bits)
    val csrValue = Bits(32 bits)
    val memOut = Bits(32 bits)
    val commitSrc = CommitSource()
    val isEbreak = Bool()

    com.io.inAluResult := aluResult
    com.io.inCsrValue := csrValue
    com.io.inMemory := memOut
    com.io.inSelSource := commitSrc

    ebreak.io.inEnable := isEbreak
    ebreak.io.inReturnStatus := aluResult
  }

  val interconnect = during build new Area {
    val ep = setup.get.ep
    val cs = setup.get.cs
    val l = logic.get

    l.aluResult := ep.getAluResult()
    l.csrValue := ep.getCsrValue()
    l.memOut := ep.getMemOut()
    l.commitSrc := cs.getCtrlSignal(l.commitSrcDef)
    l.isEbreak := cs.getCtrlSignal(l.isEbreakDef)
  }

  def getWriteback(): Bits = logic.get.com.io.outCommit
}
