package futurecore

import futurecore.lib.{Alu, ImmGen}
import spinal.core._
import spinal.lib.master

object InstTypePat {
  def ArithLogic = M"0-10011"
  def Load = M"0000011"
  def Store = M"0100011"
  def Branch = M"1100011"
  def JumpLink = M"110-111"
  def Upper = M"0-10111"
  def CSR = M"1110011"
}

case class In() extends Bundle {
  val inst = Bits(32 bits)
  val aluRes = Bits(32 bits)
  in(inst, aluRes)
}

case class ControllSignals() extends Bundle {
  val ifu = master(IFU.Ctrl())
  val idu = master(IDU.Ctrl())
  val exu = master(EXU.Ctrl())
  val mau = master(MAU.Ctrl())
  val wbu = master(WBU.Ctrl())
}

case class Controller() extends Component {
  import ImmGen.ImmType
  import Alu.AluOp
  import MAU.AccessWidth
  import WBU.WriteBackType

  val io = new Bundle {
    val input = In()
    val ctrl = ControllSignals()
  }

  val optCode = io.input.inst(6 downto 0)
  val funct3 = io.input.inst(14 downto 12)
  val funct7 = io.input.inst(31 downto 25)
  val ctrlSignals = ControllSignals()

  // Default signals
  ctrlSignals.ifu.pcEn := True
  ctrlSignals.ifu.pcAbsSel := False
  ctrlSignals.ifu.pcRelSel := False
  ctrlSignals.idu.regWrEn := False
  ctrlSignals.idu.immType := ImmType.I // Default to I-type
  ctrlSignals.exu.pcSel := False
  ctrlSignals.exu.zeroSel := False
  ctrlSignals.exu.immSel := False
  ctrlSignals.exu.aluOpType := AluOp.ADD // Default to ADD operation
  ctrlSignals.mau.memValid := False
  ctrlSignals.mau.memWrEn := False
  ctrlSignals.mau.dataSextEn := False // Default to no sign extension to data from memory
  ctrlSignals.mau.accessType := AccessWidth.Word // Default to word access
  ctrlSignals.wbu.wbType := WriteBackType.Alu // Default to ALU result for write back

  switch(optCode) {
    is(InstTypePat.ArithLogic) {
      // Arithmetic and Logic instructions
      ctrlSignals.idu.regWrEn := True
      when(optCode(5) === True) {
        // R-type
        val aluOpCodeR = funct7(5) ## funct3(2 downto 0)
        ctrlSignals.exu.aluOpType.assignFromBits(aluOpCodeR)
      } otherwise {
        // I-Type
        ctrlSignals.idu.immType := ImmType.I
        ctrlSignals.exu.immSel := True
        when(funct3 === M"-01") {
          // shift instructions with 5-bit immediate has funct7
          val shiftOpCodeI = funct7(5) ## funct3(2 downto 0)
          ctrlSignals.exu.aluOpType.assignFromBits(shiftOpCodeI)
        } otherwise {
          // arithmetic instructions with 12-bit immediate does not have funct7
          val arithOpCodeI = U"1'b0" ## funct3(2 downto 0)
          ctrlSignals.exu.aluOpType.assignFromBits(arithOpCodeI)
        }
      }
    }
    is(InstTypePat.Load) {
      // Load instructions
      ctrlSignals.idu.regWrEn := True
      ctrlSignals.idu.immType := ImmType.I
      ctrlSignals.exu.immSel := True
      ctrlSignals.mau.memValid := True
      ctrlSignals.mau.dataSextEn := funct3(2) =/= True // Sign extension
      val accessTypeCode = funct3(1 downto 0)
      ctrlSignals.mau.accessType.assignFromBits(accessTypeCode)
      ctrlSignals.wbu.wbType := WriteBackType.Mem
    }
    is(InstTypePat.Store) {
      // Store instructions
      ctrlSignals.idu.immType := ImmType.S
      ctrlSignals.exu.immSel := True
      ctrlSignals.mau.memValid := True
      ctrlSignals.mau.memWrEn := True
      val accessTypeCode = funct3(1 downto 0)
      ctrlSignals.mau.accessType.assignFromBits(accessTypeCode)
    }
    is(InstTypePat.Branch) {
      // Branch instructions
      ctrlSignals.ifu.pcAbsSel := False
      ctrlSignals.idu.immType := ImmType.B
      switch(funct3) {
        is(M"00-") { ctrlSignals.exu.aluOpType := AluOp.EQ }
        is(M"10-") { ctrlSignals.exu.aluOpType := AluOp.SLT }
        is(M"11-") { ctrlSignals.exu.aluOpType := AluOp.SLTU }
      }
      when(funct3(0) === True) {
        ctrlSignals.mau.dataSextEn := funct3(2) === True // Sign extension
        // bne, bge, bgeu
        ctrlSignals.ifu.pcRelSel := True === ~io.input.aluRes.lsb
      } otherwise {
        // beq, blt, bltu
        ctrlSignals.ifu.pcRelSel := True === io.input.aluRes.lsb
      }
    }
    is(InstTypePat.JumpLink) {
      // Jump and Link instructions
      ctrlSignals.ifu.pcAbsSel := True
      ctrlSignals.idu.regWrEn := True
      ctrlSignals.wbu.wbType := WriteBackType.Pc
      ctrlSignals.exu.immSel := True
      when(optCode(3) === True) {
        // jal
        ctrlSignals.exu.pcSel := True
        ctrlSignals.idu.immType := ImmType.J

      } otherwise {
        // jalr
        ctrlSignals.idu.immType := ImmType.I
      }
    }
    is(InstTypePat.Upper) {
      // Upper immediate instructions
      ctrlSignals.idu.regWrEn := True
      ctrlSignals.idu.immType := ImmType.U
      ctrlSignals.exu.immSel := True

      when(optCode(5) === False) {
        // auipc
        ctrlSignals.exu.pcSel := True
      } otherwise {
        // lui
        ctrlSignals.exu.zeroSel := True
      }
    }
    is(InstTypePat.CSR) {
      when(io.input.inst(20) === True) {
        // ebreak
        ctrlSignals.ifu.pcEn := False // Disable PC update
        ctrlSignals.wbu.wbType := WriteBackType.Ebreak
      }
    }
  }

  io.ctrl := ctrlSignals
}
