package futurecore.writeback

import spinal.core._
import spinal.core.fiber.{Retainer, RetainerGroup}
import spinal.lib.misc.plugin.FiberPlugin

import futurecore.decode.CtrlService
import futurecore.decode.CtrlService.CtrlDef
import futurecore.riscv.Rv32i
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

    val commitSrcDef = CtrlDef(CommitSource(), CommitSource.Result)
      .setWhen(CommitSource.Memory, Rv32i.Lb, Rv32i.Lh, Rv32i.Lw, Rv32i.Lbu, Rv32i.Lhu)
    cs.registerCtrlSignal(commitSrcDef)

    val isEbreakDef = CtrlDef(Bool(), False).setWhen(True, Rv32i.Ebreak)
    cs.registerCtrlSignal(isEbreakDef)

    buildBefore.release()

    val result = SInt(32 bits)
    val memOut = Bits(32 bits)
    val commitSrc = CommitSource()
    val isEbreak = Bool()

    com.io.inResult := result
    com.io.inMemory := memOut
    com.io.inSelSource := commitSrc

    ebreak.io.inEnable := isEbreak
    ebreak.io.inReturnStatus := result
  }

  val interconnect = during build new Area {
    val ep = setup.get.ep
    val cs = setup.get.cs
    val l = logic.get

    l.result := ep.getResult()
    l.memOut := ep.getMemOut()
    l.commitSrc := cs.getCtrlSignal(l.commitSrcDef)
    l.isEbreak := cs.getCtrlSignal(l.isEbreakDef)
  }

  def getWriteback(): Bits = logic.get.com.io.outCommit
}
