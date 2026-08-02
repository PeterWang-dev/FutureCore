package futurecore

import futurecore.riscv.{Rv32i, Zicsr, Privileged}

object Globals {
  val SupportIsas = List(Rv32i, Zicsr, Privileged)
  val AllInstructions = SupportIsas.collect(_.instructions).flatten
}
