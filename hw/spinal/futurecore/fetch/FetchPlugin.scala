package futurecore.fetch

import spinal.core._
import spinal.lib.misc.plugin._
import spinal.lib.BinaryBuilder2

import futurecore.riscv.{Rv32i, Privileged}
import futurecore.decode.CtrlService
import futurecore.decode.CtrlService.CtrlDef
import futurecore.execute.ExecutePlugin
import futurecore.decode.DecodePlugin

class FetchPlugin extends FiberPlugin {
  import BranchTargeter.BranchMode

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

    val branchModeDef = CtrlDef(BranchMode(), BranchMode.PcReletive)
      .setWhen(BranchMode.Displacement, Rv32i.Jalr)
      .setWhen(BranchMode.Trap, Rv32i.Ecall)
      .setWhen(BranchMode.TrapReturn, Privileged.Mret)
    cs.registerCtrlSignal(branchModeDef)

    val isUncondDef = CtrlDef(Bool(), False)
      .setWhen(True, Rv32i.Jal, Rv32i.Jalr)
      .setWhen(True, Rv32i.Ecall, Privileged.Mret)
    cs.registerCtrlSignal(isUncondDef)

    val isCondDef = CtrlDef(Bool(), False)
      .setWhen(True, Rv32i.Beq, Rv32i.Bge, Rv32i.Bgeu, Rv32i.Blt, Rv32i.Bltu, Rv32i.Bne)
    cs.registerCtrlSignal(isCondDef)

    buildBefore.release()

    val baseRs1 = Bits(32 bits)
    val offsetImm = SInt(32 bits)
    val trapVector = UInt(32 bits)
    val epc = UInt(32 bits)
    val branchMode = BranchMode()
    val isCond = Bool()
    val isUncond = Bool()
    val branchCond = Bool()

    bt.io.inPc := pc.io.outInstAddr
    bt.io.inBaseReg := baseRs1
    bt.io.inOffsetImm := offsetImm
    bt.io.inTrap := trapVector
    bt.io.inEpc := epc
    bt.io.inSelMode := branchMode

    pc.io.inTargetAddr := bt.io.outTarget
    pc.io.inDirectWriteEnable := isUncond | (isCond & branchCond)

    im.io.inInstAddr := pc.io.outInstAddr
  }

  val interconnect = during build new Area {
    val cs = setup.get.cs
    val dp = setup.get.dp
    val ep = setup.get.ep
    val l = logic.get

    l.baseRs1 := dp.getRs1()
    l.offsetImm := dp.getImm()
    l.trapVector := ep.getTrapVector()
    l.epc := ep.getTrapReturn()
    l.branchMode := cs.getCtrlSignal(l.branchModeDef)
    l.isCond := cs.getCtrlSignal(l.isCondDef)
    l.isUncond := cs.getCtrlSignal(l.isUncondDef)
    l.branchCond := ep.getBranchCond()
  }

  def getPc(): UInt = logic.get.pc.io.outInstAddr

  def getInstruction(): Bits = logic.get.im.io.outInst
}
