package futurecore.fetch

import spinal.core._
import spinal.lib.misc.plugin._
import spinal.lib.BinaryBuilder2

import futurecore.riscv.Rvi
import futurecore.decode.CtrlService
import futurecore.decode.CtrlService.CtrlDef
import futurecore.execute.ExecutePlugin
import futurecore.decode.DecodePlugin

class FetchPlugin extends FiberPlugin {
  val setup = during setup new Area {
    val cs = host[CtrlService]
    val dp = host[DecodePlugin]
    val ep = host[ExecutePlugin]
    val buildBefore = retains(cs.ctrlLock)
  }

  val logic = during build new Area {
    val cs = setup.get.cs
    val buildBefore = setup.get.buildBefore

    val pc = new ProgramCounter("80000000".asHex)
    val bt = new BranchTargeter
    val im = new InstructionMemory

    val pcWriteDef = new CtrlDef(Bool(), False)
      .setWhen(True, Rvi.Jal, Rvi.Jalr)
      .setWhen(True, Rvi.Beq, Rvi.Bge, Rvi.Bgeu, Rvi.Blt, Rvi.Bltu, Rvi.Bne)
    cs.registerCtrlSignal(pcWriteDef)

    val branchBaseDef = new CtrlDef(Bool(), False)
      .setWhen(True, Rvi.Jalr)
    cs.registerCtrlSignal(branchBaseDef)

    buildBefore.release()

    val rs1 = Bits(32 bits)
    val imm = SInt(32 bits)
    val branchBase = Bool()
    val pcWrite = Bool()
    val branchCond = Bool()

    bt.io.inPc := pc.io.instAddr
    bt.io.inBaseReg := rs1
    bt.io.inOffsetImm := imm
    bt.io.enableBase := branchBase

    pc.io.targetAddr := bt.io.outTarget
    pc.io.directWriteEnable := pcWrite & branchCond

    im.io.instAddr := pc.io.instAddr
  }

  val interconnect = during build new Area {
    val cs = setup.get.cs
    val dp = setup.get.dp
    val ep = setup.get.ep
    val l = logic.get

    l.rs1 := dp.getRs1()
    l.imm := dp.getImm()
    l.branchBase := cs.getCtrlSignal(l.branchBaseDef)
    l.pcWrite := cs.getCtrlSignal(l.pcWriteDef)
    l.branchCond := ep.getBranchCond()
  }

  def getPc(): UInt = logic.get.pc.io.instAddr

  def getInstruction(): Bits = logic.get.im.io.inst
}
