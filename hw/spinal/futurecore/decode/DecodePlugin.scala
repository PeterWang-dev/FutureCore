package futurecore.decode

import spinal.core._
import spinal.core.fiber.Retainer
import spinal.lib.logic.{DecodingSpec, Masked}
import spinal.lib.misc.plugin.FiberPlugin

import scala.collection.mutable.Map

import futurecore.Globals.AllInstructions
import futurecore.riscv.{Rv32i, Zicsr, Privileged}
import futurecore.fetch.FetchPlugin
import futurecore.writeback.WritebackPlugin

class DecodePlugin extends FiberPlugin with CtrlService {
  import CtrlService.CtrlDef
  import ImmGenerator.ImmMode

  val setup = during setup new Area {
    val fp = host[FetchPlugin]
    val wp = host[WritebackPlugin]
  }

  val logic = during build new Area {
    val immGen = new ImmGenerator
    val regfile = new IntRegfile

    val immSelDef = CtrlDef(ImmMode(), ImmMode.I)
      .setWhen(ImmMode.S, Rv32i.instructions.filter(_.fields.contains(Rv32i.SImm)))
      .setWhen(ImmMode.B, Rv32i.instructions.filter(_.fields.contains(Rv32i.BImm)))
      .setWhen(ImmMode.U, Rv32i.instructions.filter(_.fields.contains(Rv32i.UImm)))
      .setWhen(ImmMode.J, Rv32i.instructions.filter(_.fields.contains(Rv32i.JImm)))
    registerCtrlSignal(immSelDef)

    val rfWriteEnableDef = CtrlDef(Bool(), False)
      .setWhen(
        True,
        Rv32i.instructions
          .filter(_.fields.contains(Rv32i.Rd))
          .filterNot(_ == Rv32i.Ebreak)
      )
    registerCtrlSignal(rfWriteEnableDef)

    ctrlLock.await()

    val ctrlArea = new Area {
      val inst = Bits(32 bits)

      val map = ctrlSignals.map { ctrlDef =>
        val spec = ctrlDef.toDecodingSpec
        val signal = spec.build(inst, AllInstructions.map(_.toMasked))
        ctrlDef -> signal
      }.toMap
    }

    val inst = Bits(32 bits)
    val writebackData = Bits(32 bits)
    val immSel = ImmMode()
    val rfWriteEnable = Bool()

    ctrlArea.inst := inst

    immGen.io.inInst := inst
    immGen.io.inSelMode := immSel

    regfile.io.inAddrReadA := Rv32i.Rs1.extract(inst).asUInt
    regfile.io.inAddrReadB := Rv32i.Rs2.extract(inst).asUInt
    regfile.io.inAddrWrite := Rv32i.Rd.extract(inst).asUInt
    regfile.io.inDataWrite := writebackData
    regfile.io.inEnableWrite := rfWriteEnable

    val csrAddr = Zicsr.Csr.extract(inst).asUInt
  }

  val interconnect = during build new Area {
    val fp = setup.get.fp
    val wp = setup.get.wp
    val l = logic.get

    l.inst := fp.getInstruction()
    l.writebackData := wp.getWriteback()
    l.immSel := getCtrlSignal(l.immSelDef)
    l.rfWriteEnable := getCtrlSignal(l.rfWriteEnableDef)
  }

  override def getCtrlSignal[T <: BaseType](key: CtrlDef[T, _]): T = {
    logic.get.ctrlArea.map.get(key) match {
      case None    => ??? // ! Never reaches here theoretically
      case Some(s) => s.asInstanceOf[T] // ! Never throws here theoretically
    }
  }

  def getRs1(): Bits = logic.get.regfile.io.outDataReadA

  def getRs2(): Bits = logic.get.regfile.io.outDataReadB

  def getImm(): SInt = logic.get.immGen.io.outImm

  def getCsrAddr(): UInt = logic.get.csrAddr

  def getReturnStatus(): SInt = logic.get.regfile.io.outSpecialRet.asSInt

  def getDbgRegfile(): Vec[Bits] = logic.get.regfile.io.outDbgRegisters
}
