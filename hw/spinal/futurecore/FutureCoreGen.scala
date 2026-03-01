package futurecore

import spinal.core._
import spinal.lib.misc.plugin.PluginHost

import futurecore.fetch.FetchPlugin
import futurecore.decode.DecodePlugin
import futurecore.execute.ExecutePlugin
import futurecore.writeback.WritebackPlugin

class FutureCorePlugin extends Component {
  val host = new PluginHost()

  host.asHostOf(
    new FetchPlugin,
    new DecodePlugin,
    new ExecutePlugin,
    new WritebackPlugin
  )
}

object ElaborateExperimental extends App {
  Config.spinal.generateSystemVerilog(new FutureCorePlugin).mergeRTLSource("blackbox")
}
