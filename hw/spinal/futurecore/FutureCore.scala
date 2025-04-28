package futurecore

import futurecore._
import spinal.core._

case class FutureCore() extends Component {
  val io = new Bundle {
    val pc = out UInt (32 bits)
    val ret = out UInt (32 bits)
  }
  noIoPrefix()

  val ifu = InstFetchUnit()
  val idu = InstDecodeUnit()
  val exu = ExecuteUnit()
  val mem = MemoryAccessUnit()
  val wbu = WriteBackUnit()
  val ctrl = Controller()

  val pc = ifu.pcGen.pcReg.pull()
  val ret = idu.regFile.ret.pull()
  io.pc := pc
  io.ret := ret.asUInt

  ctrl.io.inst := ifu.io.inst
  ctrl.io.aluRes := exu.io.aluRes

  ifu.io.offsetRel := idu.io.imm
  ifu.io.targetAbs := exu.io.aluRes.asUInt
  ifu.io.pcAbsSel := ctrl.io.ctrl.pcAbsSel
  ifu.io.pcRelSel := ctrl.io.ctrl.pcRelSel

  idu.io.inst := ifu.io.inst
  idu.io.regWrEn := ctrl.io.ctrl.regWrEn
  idu.io.regWrData := wbu.io.wbRes
  idu.io.immType := ctrl.io.ctrl.immType

  exu.io.pc := ifu.io.pcCurr
  exu.io.rs1 := idu.io.regSrc1
  exu.io.rs2 := idu.io.regSrc2
  exu.io.imm := idu.io.imm
  exu.io.pcSel := ctrl.io.ctrl.pcSel
  exu.io.zeroSel := ctrl.io.ctrl.zeroSel
  exu.io.immSel := ctrl.io.ctrl.immSel
  exu.io.aluOpType := ctrl.io.ctrl.aluOpType

  mem.io.memAddr := exu.io.aluRes.asUInt
  mem.io.memValid := ctrl.io.ctrl.memValid
  mem.io.memWrEn := ctrl.io.ctrl.memWrEn
  mem.io.memWr := idu.io.regSrc2
  mem.io.dataSextEn := ctrl.io.ctrl.dataSextEn
  mem.io.accessType := ctrl.io.ctrl.accessType

  wbu.io.aluRes := exu.io.aluRes
  wbu.io.memRes := mem.io.memRe
  wbu.io.pcLink := ifu.io.pcNeigh
  wbu.io.wbType := ctrl.io.ctrl.wbType
}

object Elaborate extends App {
  Config.spinal.generateSystemVerilog(FutureCore()).mergeRTLSource("blackbox")
}
