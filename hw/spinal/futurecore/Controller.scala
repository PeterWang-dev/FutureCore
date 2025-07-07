package futurecore

import futurecore.lib.{Alu, ImmGen}
import spinal.core._
import spinal.lib.master

object Controller {
  object OptcodePat {
    final val Load = M"00000"
    final val Store = M"01000"
    final val Branch = M"11000"
    final val JumpLink = M"110-1"
    final val Op = M"0-100"
    final val Upper = M"0-101"
    final val System = M"11100"
  }
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
  import Controller._
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

  switch(optCode(6 downto 2)) {
    is(OptcodePat.Op) {
      // Arithmetic and Logic instructions
      val hasImm = optCode(5) === False
      val isShift = funct3 === M"-01"
      // arithmetic instructions with 12-bit immediate does not have funct7
      val aluOp = Mux(
        hasImm && !isShift,
        U"1'b0" ## funct3(2 downto 0),
        funct7(5) ## funct3(2 downto 0)
      )

      ctrlSignals.idu.regWrEn := True
      ctrlSignals.exu.aluOpType.assignFromBits(aluOp)
      when(hasImm) {
        ctrlSignals.idu.immType := ImmType.I
        ctrlSignals.exu.immSel := True
      }
    }
    is(OptcodePat.Load) {
      // Load instructions
      val accessTypeCode = funct3(1 downto 0)
      ctrlSignals.idu.regWrEn := True
      ctrlSignals.idu.immType := ImmType.I
      ctrlSignals.exu.immSel := True
      ctrlSignals.mau.memValid := True
      ctrlSignals.mau.dataSextEn := funct3(2) =/= True // Sign extension
      ctrlSignals.mau.accessType.assignFromBits(accessTypeCode)
      ctrlSignals.wbu.wbType := WriteBackType.Mem
    }
    is(OptcodePat.Store) {
      // Store instructions
      val accessTypeCode = funct3(1 downto 0)
      ctrlSignals.idu.immType := ImmType.S
      ctrlSignals.exu.immSel := True
      ctrlSignals.mau.memValid := True
      ctrlSignals.mau.memWrEn := True
      ctrlSignals.mau.accessType.assignFromBits(accessTypeCode)
    }
    is(OptcodePat.Branch) {
      // Branch instructions
      ctrlSignals.ifu.pcAbsSel := False
      ctrlSignals.idu.immType := ImmType.B
      switch(funct3) {
        is(M"00-") { ctrlSignals.exu.aluOpType := AluOp.EQ }
        is(M"10-") { ctrlSignals.exu.aluOpType := AluOp.SLT }
        is(M"11-") { ctrlSignals.exu.aluOpType := AluOp.SLTU }
      }
      ctrlSignals.ifu.pcRelSel := Mux(
        funct3(0) === True,
        ~io.input.aluRes.lsb, // bne, bge, bgeu
        io.input.aluRes.lsb // beq, blt, bltu
      )
    }
    is(OptcodePat.JumpLink) {
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
    is(OptcodePat.Upper) {
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
    is(OptcodePat.System) {
      when(io.input.inst(20) === True) {
        // ebreak
        ctrlSignals.ifu.pcEn := False // Disable PC update
        ctrlSignals.wbu.wbType := WriteBackType.Ebreak
      }
    }
  }

  io.ctrl := ctrlSignals
}
