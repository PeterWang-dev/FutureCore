package futurecore

import futurecore._
import futurecore.lib.Debugger
import spinal.core._

case class FutureCore(rv32e: Boolean = true, hasDebugger: Boolean = true) extends Component {
  noIoPrefix()

  val ifu = IFU()
  val idu = IDU(rv32e)
  val exu = EXU()
  val mem = MAU()
  val wbu = WBU()
  val ctrl = Controller()

  ctrl.io.input.inst := ifu.io.output.inst
  ctrl.io.input.aluRes := exu.io.output.aluRes

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
  wbu.io.input.retStatus := idu.io.output.ret
  wbu.io.ctrl <> ctrl.io.ctrl.wbu

  if (hasDebugger) {
    val debugger = Debugger(rv32e)
    debugger.io.npc := ifu.pcGen.pcReg.pull()
    debugger.io.inst := ifu.io.output.inst
    debugger.io.gprs := idu.regFile.io.gprsDbg.pull()
  }
}

object Elaborate extends App {
  Config.spinal.generateSystemVerilog(FutureCore(rv32e = true, hasDebugger = true)).mergeRTLSource("blackbox")
}
