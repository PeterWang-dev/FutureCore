package futurecore

import futurecore._
import spinal.core._

case class FutureCore() extends Component {
  val io = new Bundle {
    val pc = out UInt (32 bits)
  }
  noIoPrefix()

  val ifu = IFU()
  val idu = IDU()
  val exu = EXU()
  val mem = MAU()
  val wbu = WBU()
  val ctrl = Controller()

  val pc = ifu.pcGen.pcReg.pull()
  io.pc := pc

  ctrl.io.inst := ifu.io.output.inst
  ctrl.io.aluRes := exu.io.output.aluRes

  ifu.io.input.offsetRel := idu.io.output.imm
  ifu.io.input.targetAbs := exu.io.output.aluRes.asUInt
  ifu.io.ctrl <> ctrl.io.ctrl.ifu

  idu.io.input.inst := ifu.io.output.inst
  idu.io.input.regWrData := wbu.io.output.wbRes
  idu.io.ctrl <> ctrl.io.ctrl.idu

  exu.io.input.pc := ifu.io.output.pcCurr
  exu.io.input.rs1 := idu.io.output.regSrc1
  exu.io.input.rs2 := idu.io.output.regSrc2
  exu.io.input.imm := idu.io.output.imm
  exu.io.ctrl <> ctrl.io.ctrl.exu

  mem.io.input.memAddr := exu.io.output.aluRes.asUInt
  mem.io.input.memWr := idu.io.output.regSrc2
  mem.io.ctrl <> ctrl.io.ctrl.mau

  wbu.io.input.aluRes := exu.io.output.aluRes
  wbu.io.input.memRes := mem.io.output.memRe
  wbu.io.input.pcLink := ifu.io.output.pcNeigh
  wbu.io.input.retStatus := idu.regFile.ret.pull()
  wbu.io.ctrl <> ctrl.io.ctrl.wbu
}

object Elaborate extends App {
  Config.spinal.generateSystemVerilog(FutureCore()).mergeRTLSource("blackbox")
}
