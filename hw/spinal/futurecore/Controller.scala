package futurecore

import futurecore.{AccessWidth, WriteBackType}
import futurecore.lib.{AluOp, ImmType}
import futurecore.blackbox.csr_dpi
import spinal.core._

object InstTypePat {
  def ArithLogic = M"0-10011"
  def Load = M"0000011"
  def Store = M"0100011"
  def Branch = M"1100011"
  def JumpLink = M"110-111"
  def Upper = M"0-10111"
  def CSR = M"1110011"
}

case class ControllSignals() extends Bundle {
  // InstFetchUnit
  val pcAbsSel = Bool()
  val pcRelSel = Bool()
  // InstDecodeUnit
  val regWrEn = Bool()
  val immType = ImmType()
  // ExecuteUnit
  val pcSel = Bool()
  val zeroSel = Bool()
  val immSel = Bool()
  val aluOpType = AluOp()
  // MemoryAccessUnit
  val memValid = Bool()
  val memWrEn = Bool()
  val dataSextEn = Bool()
  val accessType = AccessWidth()
  // WriteBackUnit
  val wbType = WriteBackType()
}

case class Controller() extends Component {
  val io = new Bundle {
    val inst = in Bits (32 bits)
    val aluRes = in Bits (32 bits)
    val ctrl = out(ControllSignals())
  }

  val optCode = io.inst(6 downto 0)
  val funct3 = io.inst(14 downto 12)
  val funct7 = io.inst(31 downto 25)
  val ctrlSignals = ControllSignals()

  val csr_dpi = new csr_dpi;
  csr_dpi.io.valid := False

  // Default signals
  ctrlSignals.pcAbsSel := False
  ctrlSignals.pcRelSel := False
  ctrlSignals.regWrEn := False
  ctrlSignals.immType := ImmType.I // Default to I-type
  ctrlSignals.pcSel := False
  ctrlSignals.zeroSel := False
  ctrlSignals.immSel := False
  ctrlSignals.aluOpType := AluOp.ADD // Default to ADD operation
  ctrlSignals.memValid := False
  ctrlSignals.memWrEn := False
  ctrlSignals.dataSextEn := False // Default to no sign extension to data from memory
  ctrlSignals.accessType := AccessWidth.Word // Default to word access
  ctrlSignals.wbType := WriteBackType.Alu // Default to ALU result for write back

  switch(optCode) {
    is(InstTypePat.ArithLogic) {
      // Arithmetic and Logic instructions
      val aluOpCode = funct7(5) ## funct3(2 downto 0)
      ctrlSignals.aluOpType.assignFromBits(aluOpCode)
      ctrlSignals.regWrEn := True
      when(optCode(5) === False) {
        // IType
        ctrlSignals.immType := ImmType.I
        ctrlSignals.immSel := True
      }
    }
    is(InstTypePat.Load) {
      // Load instructions
      ctrlSignals.regWrEn := True
      ctrlSignals.immType := ImmType.I
      ctrlSignals.immSel := True
      ctrlSignals.memValid := True
      ctrlSignals.dataSextEn := funct3(2) === True // Sign extension
      val accessTypeCode = funct3(1 downto 0)
      ctrlSignals.accessType.assignFromBits(accessTypeCode)
      ctrlSignals.wbType := WriteBackType.Mem
    }
    is(InstTypePat.Store) {
      // Store instructions
      ctrlSignals.immType := ImmType.S
      ctrlSignals.immSel := True
      ctrlSignals.memValid := True
      ctrlSignals.memWrEn := True
      val accessTypeCode = funct3(1 downto 0)
      ctrlSignals.accessType.assignFromBits(accessTypeCode)
    }
    is(InstTypePat.Branch) {
      // Branch instructions
      ctrlSignals.pcAbsSel := False
      ctrlSignals.immType := ImmType.B
      switch(funct3) {
        is(M"00-") { ctrlSignals.aluOpType := AluOp.EQ }
        is(M"10-") { ctrlSignals.aluOpType := AluOp.SLT }
        is(M"01-") { ctrlSignals.aluOpType := AluOp.SLTU }
      }
      when(funct3(0) === True) {
        ctrlSignals.dataSextEn := funct3(2) === True // Sign extension
        // bne, bge, bgeu
        ctrlSignals.pcRelSel := True & ~io.aluRes.lsb
      } otherwise {
        // beq, blt, bltu
        ctrlSignals.pcRelSel := True & io.aluRes.lsb
      }
    }
    is(InstTypePat.JumpLink) {
      // Jump and Link instructions
      ctrlSignals.pcAbsSel := True
      ctrlSignals.regWrEn := True
      ctrlSignals.wbType := WriteBackType.Pc
      ctrlSignals.immSel := True
      when(optCode(3) === True) {
        // jal
        ctrlSignals.pcSel := True
        ctrlSignals.immType := ImmType.J

      } otherwise {
        // jalr
        ctrlSignals.immType := ImmType.I
      }
    }
    is(InstTypePat.Upper) {
      // Upper immediate instructions
      ctrlSignals.regWrEn := True
      ctrlSignals.immType := ImmType.U
      ctrlSignals.immSel := True

      when(optCode(5) === False) {
        // auipc
        ctrlSignals.pcSel := True
      } otherwise {
        // lui
        ctrlSignals.zeroSel := True
      }
    }
    is(InstTypePat.CSR) {
      csr_dpi.io.valid := io.inst(20) === True
    }
  }

  io.ctrl := ctrlSignals
}
