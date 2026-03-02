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

  val logic = during setup new Area {
    val cs = host[CtrlService]
    val ep = host[ExecutePlugin]
    val buildBefore = retains(cs.ctrlLock)

    awaitBuild()

    val com = new CommitSelector

    val commitSrcDef = new CtrlDef(CommitSource(), CommitSource.Result)
      .setWhen(CommitSource.Memory, Rvi.Lb, Rvi.Lh, Rvi.Lw, Rvi.Lbu, Rvi.Lhu)
    cs.registerCtrlSignal(commitSrcDef)

    buildBefore.release()

    com.io.inResult := ep.getResult()
    com.io.inMemory := ep.getMemOut()
    com.io.selSource := cs.getCtrlSignal(commitSrcDef)
  }

  def getWriteback(): Bits = logic.get.com.io.outCommit
}
