package futurecore.execute

import spinal.core._
import spinal.lib.misc.plugin._

import futurecore.fetch.FetchPlugin
import futurecore.decode.{CtrlService, DecodePlugin}
import futurecore.decode.CtrlService.CtrlDef
import futurecore.riscv.{Rv32i, Zicsr, Privileged}

class ExecutePlugin extends FiberPlugin {
  import SrcSelector.{SrcUpMode, SrcDownMode}
  import IntAlu.AluOp
  import DataMemory.AccessWidth
  import CsrHandler.{CsrMode, ExceptionType}

  val setup = during setup new Area {
    val fp = host[FetchPlugin]
    val dp = host[DecodePlugin]
    val cs = host[CtrlService]
    val buildBefore = retains(cs.ctrlLock)
  }

  val logic = during build new Area {
    val cs = setup.get.cs
    val buildBefore = setup.get.buildBefore

    val src = new SrcSelector
    val alu = new IntAlu
    val dm = new DataMemory
    val csr = new CsrHandler

    val selUpDef = CtrlDef(SrcUpMode(), SrcUpMode.RegSrcA)
      .setWhen(SrcUpMode.Pc, Rv32i.Auipc, Rv32i.Jal, Rv32i.Jalr)
      .setWhen(SrcUpMode.Zero, Rv32i.Lui, Rv32i.Ebreak)
    cs.registerCtrlSignal(selUpDef)

    val selDownDef = CtrlDef(SrcDownMode(), SrcDownMode.RegSrcB)
      .setWhen(
        SrcDownMode.Imm,
        Rv32i.instructions
          .filter(_.fields.exists(_.isInstanceOf[Rv32i.Imm]))
          // BImm is not used as ALU does the comparison between Rs1 and Rs2
          .filterNot(_.fields.contains(Rv32i.BImm))
          // Imms of Jal and Jalr are not used as ALU is incrementing the PC
          .filterNot(inst => inst == Rv32i.Jal || inst == Rv32i.Jalr)
          // IImm of SYSTEM instructions are not used
          .filterNot(_ == Rv32i.Ebreak)
      )
      .setWhen(SrcDownMode.PcIncrement, Rv32i.Jal, Rv32i.Jalr)
      .setWhen(SrcDownMode.ReturnStatus, Rv32i.Ebreak)
    cs.registerCtrlSignal(selDownDef)

    val aluOpDef = CtrlDef(AluOp(), AluOp.Add)
      .setWhen(AluOp.Sub, Rv32i.Sub)
      .setWhen(AluOp.Xor, Rv32i.Xori, Rv32i.Xor)
      .setWhen(AluOp.Or, Rv32i.Ori, Rv32i.Or)
      .setWhen(AluOp.And, Rv32i.Andi, Rv32i.And)
      .setWhen(AluOp.ShiftLeftLogic, Rv32i.Slli, Rv32i.Sll)
      .setWhen(AluOp.ShiftRightLogic, Rv32i.Srli, Rv32i.Srl)
      .setWhen(AluOp.ShiftRightArith, Rv32i.Sra, Rv32i.Srai)
      .setWhen(AluOp.EqualTo, Rv32i.Beq)
      .setWhen(AluOp.NotEqual, Rv32i.Bne)
      .setWhen(AluOp.LessThan, Rv32i.Blt, Rv32i.Slti, Rv32i.Slt)
      .setWhen(AluOp.GreaterEqual, Rv32i.Bge)
      .setWhen(AluOp.LessThanUnsigned, Rv32i.Bltu, Rv32i.Sltiu, Rv32i.Sltu)
      .setWhen(AluOp.GreaterEqualUnsigned, Rv32i.Bgeu)
    cs.registerCtrlSignal(aluOpDef)

    val memAddrValidDef = CtrlDef(Bool(), False)
      .setWhen(True, Rv32i.Lb, Rv32i.Lbu, Rv32i.Lh, Rv32i.Lhu, Rv32i.Lw)
      .setWhen(True, Rv32i.Sb, Rv32i.Sh, Rv32i.Sw)
    cs.registerCtrlSignal(memAddrValidDef)

    val memAccessDef = CtrlDef(AccessWidth(), AccessWidth.Byte)
      .setWhen(AccessWidth.Half, Rv32i.Lh, Rv32i.Lhu, Rv32i.Sh)
      .setWhen(AccessWidth.Word, Rv32i.Lw, Rv32i.Sw)
    cs.registerCtrlSignal(memAccessDef)

    val readSextDef = CtrlDef(Bool(), True)
      .setWhen(False, Rv32i.Lbu, Rv32i.Lhu)
    cs.registerCtrlSignal(readSextDef)

    val memWriteDef = CtrlDef(Bool(), False)
      .setWhen(True, Rv32i.Sb, Rv32i.Sh, Rv32i.Sw)
    cs.registerCtrlSignal(memWriteDef)

    val csrEnableDef = CtrlDef(Bool(), False)
      .setWhen(True, Zicsr.instructions)
    cs.registerCtrlSignal(csrEnableDef)

    val csrModeDef = CtrlDef(CsrMode(), CsrMode.Write)
      .setWhen(CsrMode.Set, Zicsr.Csrrs)
    cs.registerCtrlSignal(csrModeDef)

    val trapEnableDef = CtrlDef(Bool(), False)
      .setWhen(True, Rv32i.Ecall)
    cs.registerCtrlSignal(trapEnableDef)

    val trapReturnDef = CtrlDef(Bool(), False)
      .setWhen(True, Privileged.Mret)
    cs.registerCtrlSignal(trapReturnDef)

    val exceptionTypeDef = CtrlDef(ExceptionType(), ExceptionType.EcallM)
    cs.registerCtrlSignal(exceptionTypeDef)

    buildBefore.release()

    val rs1 = Bits(32 bits)
    val rs2 = Bits(32 bits)
    val pc = UInt(32 bits)
    val imm = SInt(32 bits)
    val ret = SInt(32 bits)
    val selUp = SrcUpMode()
    val selDown = SrcDownMode()
    val aluOp = AluOp()
    val memAddrValid = Bool()
    val memAccessWidth = AccessWidth()
    val readSext = Bool()
    val memWrite = Bool()
    val csrEnable = Bool()
    val csrMode = CsrMode()
    val csrAddr = UInt(12 bits)
    val trapEnable = Bool()
    val trapReturn = Bool()
    val exceptionType = ExceptionType()

    src.io.inRs1 := rs1
    src.io.inRs2 := rs2
    src.io.inPc := pc
    src.io.inImm := imm
    src.io.inRet := ret
    src.io.inSelUp := selUp
    src.io.inSelDown := selDown

    alu.io.inA := src.io.outSrcUp
    alu.io.inB := src.io.outSrcDown
    alu.io.inSelOp := aluOp

    dm.io.inAddr := alu.io.outRes.asUInt
    dm.io.inValidAddr := memAddrValid
    dm.io.inSelAccessWidth := memAccessWidth
    dm.io.inEnableReadSext := readSext
    dm.io.inEnableWrite := memWrite
    dm.io.inDataWrite := rs2

    csr.io.inEnableCsr := csrEnable
    csr.io.inSelCsrMode := csrMode
    csr.io.inCsrAddr := csrAddr
    csr.io.inNewVal := rs1

    csr.io.inEnableTrap := trapEnable
    csr.io.inEnableTrapReturn := trapReturn
    csr.io.inSelException := exceptionType
    csr.io.inExceptionPc := pc
  }

  val interconnect = during build new Area {
    val fp = setup.get.fp
    val dp = setup.get.dp
    val cs = setup.get.cs
    val l = logic.get

    l.rs1 := dp.getRs1()
    l.rs2 := dp.getRs2()
    l.pc := fp.getPc()
    l.imm := dp.getImm()
    l.ret := dp.getReturnStatus()
    l.selUp := cs.getCtrlSignal(l.selUpDef)
    l.selDown := cs.getCtrlSignal(l.selDownDef)
    l.aluOp := cs.getCtrlSignal(l.aluOpDef)
    l.memAccessWidth := cs.getCtrlSignal(l.memAccessDef)
    l.readSext := cs.getCtrlSignal(l.readSextDef)
    l.memWrite := cs.getCtrlSignal(l.memWriteDef)
    l.memAddrValid := cs.getCtrlSignal(l.memAddrValidDef)
    l.csrEnable := cs.getCtrlSignal(l.csrEnableDef)
    l.csrMode := cs.getCtrlSignal(l.csrModeDef)
    l.csrAddr := dp.getCsrAddr()
    l.trapEnable := cs.getCtrlSignal(l.trapEnableDef)
    l.trapReturn := cs.getCtrlSignal(l.trapReturnDef)
    l.exceptionType := cs.getCtrlSignal(l.exceptionTypeDef)
  }

  def getAluResult(): SInt = logic.get.alu.io.outRes

  def getBranchCond(): Bool = logic.get.alu.io.outRes.lsb

  def getMemOut(): Bits = logic.get.dm.io.outDataRead

  def getCsrValue(): Bits = logic.get.csr.io.outOldVal

  def getTrapVector(): UInt = logic.get.csr.io.outTrapVector

  def getTrapReturn(): UInt = logic.get.csr.io.outReturnPc

  def getDbgMstatus(): Bits = logic.get.csr.io.dbgMstatus

  def getDbgMtvec(): Bits = logic.get.csr.io.dbgMtvec

  def getDbgMepc(): Bits = logic.get.csr.io.dbgMepc

  def getDbgMcause(): Bits = logic.get.csr.io.dbgMcause
}
