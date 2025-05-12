package futurecore

import futurecore.lib.{ImmGen, RegFile}
import spinal.core._
import spinal.lib._

object IDU {
  case class In() extends Bundle {
    val inst = Bits(32 bits)
    val regWrData = Bits(32 bits)
    in(inst, regWrData)
  }

  case class Out() extends Bundle {
    val regSrc1 = Bits(32 bits)
    val regSrc2 = Bits(32 bits)
    val imm = SInt(32 bits)
    out(regSrc1, regSrc2, imm)
  }

  case class Ctrl() extends Bundle with IMasterSlave {
    val regWrEn = Bool()
    val immType = ImmGen.ImmType()
    override def asMaster(): Unit = {
      out(regWrEn, immType)
    }
  }
}

case class IDU() extends Component {
  import IDU._
  val io = new Bundle {
    val input = In()
    val output = Out()
    val ctrl = slave(Ctrl())
  }

  val regFile = RegFile()
  val immGen = ImmGen()

  regFile.io.addrRe1 := io.input.inst(19 downto 15).asUInt
  regFile.io.addrRe2 := io.input.inst(24 downto 20).asUInt
  regFile.io.addrWr := io.input.inst(11 downto 7).asUInt
  regFile.io.writeEn := io.ctrl.regWrEn
  regFile.io.dataWr := io.input.regWrData
  io.output.regSrc1 := regFile.io.dataRe1
  io.output.regSrc2 := regFile.io.dataRe2

  immGen.io.immType := io.ctrl.immType
  immGen.io.inst := io.input.inst
  io.output.imm := immGen.io.imm
}
