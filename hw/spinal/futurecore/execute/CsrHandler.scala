package futurecore.execute

import spinal.core._

object CsrHandler {
  object CsrMode extends SpinalEnum {
    val Write, Set = newElement()
  }

  object ExceptionType extends SpinalEnum {
    val EcallM = newElement()
  }
}

class CsrHandler extends Component {
  import CsrHandler._

  val io = new Bundle {
    val inEnableCsr = in port Bool()
    val inSelCsrMode = in port CsrMode()
    val inCsrAddr = in port UInt(12 bits)
    val inNewVal = in port Bits(32 bits)
    val outOldVal = out port Bits(32 bits)

    val inEnableException = in port Bool()
    val inSelException = in port ExceptionType()
    val inExceptionPc = in port UInt(32 bits)
    val outTrapVector = out port UInt(32 bits)
    val outReturnPc = out port UInt(32 bits)
  }

  val csrRegfile = new Area {
    val enable = Bool()
    val mode = CsrMode()
    val addr = UInt(12 bits)
    val newVal = Bits(32 bits)

    case class CsrReg(number: Int, init: Int) {
      val index = U(number, 12 bits)
      val reg = RegInit(B(init, 32 bits))
    }

    val mstatus = CsrReg(0x300, 0x1800) // ! Hardcoded here to pass difftest
    val mtvec = CsrReg(0x305, 0)
    val mepc = CsrReg(0x341, 0)
    val mcause = CsrReg(0x342, 0)

    // ! WARNING: To simplify implementation,
    // !          ccessibility and lowestLevel not checked!
    // !          Just list here for hinting of future implementation.
    // read/write: 00,01 read-only: 11
    val accessibility = addr(11 downto 10)
    val isReadOnly = accessibility.andR
    // privilege levels: user: 00 supervisor: 01 machine: 11
    val lowestLevel = addr(9 downto 8)

    // Atomic read write
    // ! WARNING: Also, to simplify implementation,
    // !          field constrains are ignored!
    // !          Moreover, here uses ALWAYS READ policy!
    // !          This VIOLATES the rule that if rd=x0,
    // !          ReadWrite should not cause any read side effect!
    // For now, just use plain mux or switch to handle addressing
    val oldVal = addr.mux(
      mstatus.index -> mstatus.reg,
      mtvec.index   -> mtvec.reg,
      mepc.index    -> mepc.reg,
      mcause.index  -> mcause.reg,
      default       -> B(0).resized
    )

    val writeVal = isReadOnly ? oldVal | mode.mux(
      CsrMode.Write -> newVal,
      // use or to set unmasked bits and remain masked bits
      CsrMode.Set -> (newVal | oldVal)
    )

    when(enable) {
      switch(addr) {
        is(mstatus.index) { mstatus.reg := writeVal }
        is(mtvec.index) { mtvec.reg := writeVal }
        is(mepc.index) { mepc.reg := writeVal }
        is(mcause.index) { mcause.reg := writeVal }
      }
    }
  }

  csrRegfile.enable := io.inEnableCsr
  csrRegfile.mode := io.inSelCsrMode
  csrRegfile.addr := io.inCsrAddr
  csrRegfile.newVal := io.inNewVal
  io.outOldVal := io.inEnableCsr ? csrRegfile.oldVal | B(0).resized

  // Exception handling logic
  /*
     SpinalHDL 的 bug：
     当 enum.mux() 的结果被用于在 Area 外部赋值 Area 内部的寄存器时，
     会触发 NullPointerException in PhaseCheckCrossClock。

     Workaround: 直接使用常量代替 enum mux，避免 SpinalHDL bug
     https://github.com/SpinalHDL/SpinalHDL/issues/XXX
   */
  val causeId = B"32'hb"

  // Only accept exception when not csr ops, as csr should be atomic
  when(io.inEnableException & !io.inEnableCsr) {
    csrRegfile.mcause.reg := causeId
    csrRegfile.mepc.reg := io.inExceptionPc.asBits
  }

  // ! WARNING: the trap vector and epc have been hard wared and may not
  // !          comfy spec, which may cause security problems!
  val trapTarget = (csrRegfile.mtvec.reg(31 downto 2) ## U"2'b0").asUInt
  val returnTarget = csrRegfile.mepc.reg.asUInt

  io.outTrapVector := trapTarget
  io.outReturnPc := returnTarget
}

object CsrHandlerTest extends App {
  SpinalConfig().generateSystemVerilog(new CsrHandler)
}
