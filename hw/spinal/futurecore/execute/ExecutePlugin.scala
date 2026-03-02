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

  val logic = during setup new Area {
    val fp = host[FetchPlugin]
    val dp = host[DecodePlugin]
    val cs = host[CtrlService]
    val buildBefore = retains(cs.ctrlLock)

    awaitBuild()

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

    src.io.inRs1 := dp.getRs1()
    src.io.inRs2 := dp.getRs2()
    src.io.inPc := fp.getInstruction()
    src.io.inImm := dp.getImm()
    src.io.selUp := cs.getCtrlSignal(selUpDef)
    src.io.selDown := cs.getCtrlSignal(selDownDef)

    alu.io.inA := src.io.outSrcUp
    alu.io.inB := src.io.outSrcDown
    alu.io.selOp := cs.getCtrlSignal(aluOpDef)

    dm.io.inAddr := alu.io.outRes.asUInt
    dm.io.inDataWrite := dp.getRs2()
    dm.io.selAccessWidth := cs.getCtrlSignal(memAccessDef)
    dm.io.enableReadSext := cs.getCtrlSignal(readSextDef)
    dm.io.enableWrite := cs.getCtrlSignal(memWriteDef)
    dm.io.validDataWrite := cs.getCtrlSignal(writeValidDef)
  }

  def getResult(): SInt = logic.get.alu.io.outRes

  def getBranchCond(): Bool = logic.get.alu.io.outRes.lsb

  def getMemOut(): Bits = logic.get.dm.io.outDataRead
}
