package futurecore.decode

import spinal.core._
import spinal.core.fiber.Retainer
import spinal.lib.logic.{DecodingSpec, Masked}
import spinal.lib.misc.plugin.FiberPlugin

import scala.collection.mutable.Map

import futurecore.riscv.Rvi
import futurecore.fetch.FetchPlugin
import futurecore.writeback.WritebackPlugin

class DecodePlugin extends FiberPlugin with CtrlService {
  import CtrlService.CtrlDef
  import ImmGenerator.ImmMode

  val logic = during build new Area {
    val fp = host[FetchPlugin]
    val wp = host[WritebackPlugin]

    awaitBuild()

    val immGen = new ImmGenerator
    val regfile = new IntRegfile

    val immSelDef = new CtrlDef(ImmMode(), ImmMode.I())
      .setWhen(ImmMode.I, Rvi.instructions.filter(_.fields.contains(Rvi.IImm)))
      .setWhen(ImmMode.S, Rvi.instructions.filter(_.fields.contains(Rvi.SImm)))
      .setWhen(ImmMode.B, Rvi.instructions.filter(_.fields.contains(Rvi.BImm)))
      .setWhen(ImmMode.U, Rvi.instructions.filter(_.fields.contains(Rvi.UImm)))
      .setWhen(ImmMode.J, Rvi.instructions.filter(_.fields.contains(Rvi.JImm)))
    registerCtrlSignal(immSelDef)

    val rfWriteEnableDef = new CtrlDef(Bool(), False)
      .setWhen(
        True,
        Rvi.instructions.filter(_.fields.contains(Rvi.Rd))
      )
    registerCtrlSignal(rfWriteEnableDef)

    ctrlLock.await()

    val ctrlArea = new Area {
      val instruction = Bits(32 bits)

      val map = ctrlSignals.map { ctrlDef =>
        val spec = ctrlDef.toDecodingSpec
        val signal = spec.build(instruction, Rvi.instructions.map(_.toMasked))
        ctrlDef -> signal
      }.toMap
    }

    // ! Deadlock occurs here !!!
    val inst = fp.getInstruction() // ! Blocked here, request FetchPlugin finish

    // === 内部互联 ===
    ctrlArea.instruction := inst

    immGen.io.inInst := inst
    immGen.io.selMode := getCtrlSignal(immSelDef)

    regfile.io.inAddrReadA := Rvi.Rs1.extract(inst).asUInt
    regfile.io.inAddrReadB := Rvi.Rs2.extract(inst).asUInt
    regfile.io.inAddrWrite := Rvi.Rd.extract(inst).asUInt
    regfile.io.inDataWrite := wp.getWriteback()
    regfile.io.enableWrite := getCtrlSignal(rfWriteEnableDef)
  }

  override def getCtrlSignal[T <: BaseType](key: CtrlDef[T]): T = {
    logic.get.ctrlArea.map.get(key) match {
      case None    => ??? // ! Never reaches here theoretically
      case Some(s) => s.asInstanceOf[T] // ! Never throws here theoretically
    }
  }

  def getRs1(): Bits = logic.get.regfile.io.outDataReadA

  def getRs2(): Bits = logic.get.regfile.io.outDataReadB

  def getImm(): SInt = logic.get.immGen.io.outImm
}
