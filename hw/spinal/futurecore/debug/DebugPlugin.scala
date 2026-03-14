package futurecore.debug

import spinal.core._
import spinal.lib.misc.plugin._

import futurecore.fetch.FetchPlugin
import futurecore.decode.DecodePlugin
import futurecore.execute.ExecutePlugin

class DebugPlugin extends FiberPlugin {
  val setup = during setup new Area {
    val fp = host[FetchPlugin]
    val dp = host[DecodePlugin]
    val ep = host[ExecutePlugin]
  }

  val logic = during build new Area {
    val probe = new DebugProbe
  }

  val interconnect = during build new Area {
    val fp = setup.get.fp
    val dp = setup.get.dp
    val ep = setup.get.ep
    val l = logic.get

    l.probe.io.inPc := fp.getPc()
    l.probe.io.inInst := fp.getInstruction()
    l.probe.io.inGprs := dp.getDbgIntRegs()
    l.probe.io.inMstatus := ep.getDbgMstatus()
    l.probe.io.inMtvec := ep.getDbgMtvec()
    l.probe.io.inMepc := ep.getDbgMepc()
    l.probe.io.inMcause := ep.getDbgMcause()
  }
}
