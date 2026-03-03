package futurecore.execute

import spinal.core._
import spinal.lib.misc.plugin._

import futurecore.fetch.FetchPlugin
import futurecore.decode.{CtrlService, DecodePlugin}
import futurecore.decode.CtrlService.CtrlDef
import futurecore.riscv.Rvi

class ExecutePlugin extends FiberPlugin {
  import SrcSelector.{SrcUpMode, SrcDownMode}
  import IntAlu.AluOp
  import DataMemory.AccessWidth

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

    val selUpDef = new CtrlDef(SrcUpMode(), SrcUpMode.RegSrcA)
      .setWhen(SrcUpMode.Pc, Rvi.Auipc, Rvi.Jal, Rvi.Jalr)
      .setWhen(SrcUpMode.Zero, Rvi.Lui)
    cs.registerCtrlSignal(selUpDef)

    val selDownDef = new CtrlDef(SrcDownMode(), SrcDownMode.RegSrcB)
      .setWhen(
        SrcDownMode.Imm,
        Rvi.instructions
          .filter(_.fields.exists(_.isInstanceOf[Rvi.Imm]))
          // BImm are not used as ALU does the comparison between Rs1 and Rs2
          .filterNot(_.fields.contains(Rvi.BImm))
          // Imms of Jal and Jalr are not used as ALU is incrementing the PC
          .filterNot(inst => inst == Rvi.Jal || inst == Rvi.Jalr)
      )
      .setWhen(SrcDownMode.PcIncrement, Rvi.Jal, Rvi.Jalr)
    cs.registerCtrlSignal(selDownDef)

    val aluOpDef = new CtrlDef(AluOp(), AluOp.Add)
      .setWhen(AluOp.Sub, Rvi.Sub)
      .setWhen(AluOp.Xor, Rvi.Xori, Rvi.Xor)
      .setWhen(AluOp.Or, Rvi.Ori, Rvi.Or)
      .setWhen(AluOp.And, Rvi.Andi, Rvi.And)
      .setWhen(AluOp.ShiftLeftLogic, Rvi.Slli, Rvi.Sll)
      .setWhen(AluOp.ShiftRightLogic, Rvi.Srli, Rvi.Srl)
      .setWhen(AluOp.ShiftRightArith, Rvi.Sra, Rvi.Srai)
      .setWhen(AluOp.EqualTo, Rvi.Beq)
      .setWhen(AluOp.NotEqual, Rvi.Bne)
      .setWhen(AluOp.LessThan, Rvi.Blt, Rvi.Slti, Rvi.Slt)
      .setWhen(AluOp.GreaterEqual, Rvi.Bge)
      .setWhen(AluOp.LessThanUnsigned, Rvi.Bltu, Rvi.Sltiu, Rvi.Sltu)
      .setWhen(AluOp.GreaterEqualUnsigned, Rvi.Bgeu)
    cs.registerCtrlSignal(aluOpDef)

    val memAccessDef = new CtrlDef(AccessWidth(), AccessWidth.Byte)
      .setWhen(AccessWidth.Half, Rvi.Lh, Rvi.Lhu, Rvi.Sh)
      .setWhen(AccessWidth.Word, Rvi.Lw, Rvi.Sw)
    cs.registerCtrlSignal(memAccessDef)

    val readSextDef = new CtrlDef(Bool(), True)
      .setWhen(False, Rvi.Lbu, Rvi.Lhu)
    cs.registerCtrlSignal(readSextDef)

    val memWriteDef = new CtrlDef(Bool(), False)
      .setWhen(True, Rvi.Sb, Rvi.Sh, Rvi.Sw)
    cs.registerCtrlSignal(memWriteDef)

    val writeValidDef = new CtrlDef(Bool(), False)
      .setWhen(True, Rvi.Sb, Rvi.Sh, Rvi.Sw)
    cs.registerCtrlSignal(writeValidDef)

    buildBefore.release()

    val rs1 = Bits(32 bits)
    val rs2 = Bits(32 bits)
    val pc = UInt(32 bits)
    val imm = SInt(32 bits)
    val selUp = SrcUpMode()
    val selDown = SrcDownMode()
    val aluOp = AluOp()
    val memAccessWidth = AccessWidth()
    val readSext = Bool()
    val memWrite = Bool()
    val writeValid = Bool()

    src.io.inRs1 := rs1
    src.io.inRs2 := rs2
    src.io.inPc := pc
    src.io.inImm := imm
    src.io.selUp := selUp
    src.io.selDown := selDown

    alu.io.inA := src.io.outSrcUp
    alu.io.inB := src.io.outSrcDown
    alu.io.selOp := aluOp

    dm.io.inAddr := alu.io.outRes.asUInt
    dm.io.inDataWrite := rs2
    dm.io.selAccessWidth := memAccessWidth
    dm.io.enableReadSext := readSext
    dm.io.enableWrite := memWrite
    dm.io.validDataWrite := writeValid
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
    l.selUp := cs.getCtrlSignal(l.selUpDef)
    l.selDown := cs.getCtrlSignal(l.selDownDef)
    l.aluOp := cs.getCtrlSignal(l.aluOpDef)
    l.memAccessWidth := cs.getCtrlSignal(l.memAccessDef)
    l.readSext := cs.getCtrlSignal(l.readSextDef)
    l.memWrite := cs.getCtrlSignal(l.memWriteDef)
    l.writeValid := cs.getCtrlSignal(l.writeValidDef)
  }

  def getResult(): SInt = logic.get.alu.io.outRes

  def getBranchCond(): Bool = logic.get.alu.io.outRes.lsb

  def getMemOut(): Bits = logic.get.dm.io.outDataRead
}
