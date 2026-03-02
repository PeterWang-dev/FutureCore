package futurecore.writeback

import spinal.core._
import spinal.core.fiber.{Retainer, RetainerGroup}
import spinal.lib.misc.plugin.FiberPlugin

import futurecore.decode.CtrlService
import futurecore.decode.CtrlService.CtrlDef
import futurecore.riscv.Rvi
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

    val commitSrcDef = new CtrlDef(CommitSource(), CommitSource.Result)
      .setWhen(CommitSource.Memory, Rvi.Lb, Rvi.Lh, Rvi.Lw, Rvi.Lbu, Rvi.Lhu)
    cs.registerCtrlSignal(commitSrcDef)

    buildBefore.release()

    val result = SInt(32 bits)
    val memOut = Bits(32 bits)
    val commitSrc = CommitSource()

    com.io.inResult := result
    com.io.inMemory := memOut
    com.io.selSource := commitSrc
  }

  val interconnect = during build new Area {
    val ep = setup.get.ep
    val cs = setup.get.cs
    val l = logic.get

    l.result := ep.getResult()
    l.memOut := ep.getMemOut()
    l.commitSrc := cs.getCtrlSignal(l.commitSrcDef)
  }

  def getWriteback(): Bits = logic.get.com.io.outCommit
}
