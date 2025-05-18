package futurecore

import spinal.core._
import spinal.core.sim._

object Config {
  def spinal = SpinalConfig(
    targetDirectory = sys.env.getOrElse("TARGET_DIR", "hw/gen"),
    defaultConfigForClockDomains = ClockDomainConfig(
      resetKind = SYNC,
      resetActiveLevel = LOW
    ),
    oneFilePerComponent = false,
    onlyStdLogicVectorAtTopLevelIo = false
  )

  def sim = SimConfig.withConfig(spinal).withFstWave
}
