package futurecore

import futurecore.lib.{ImmGen, ImmType, RegisterFile}
import spinal.core._

case class InstDecodeUnit() extends Component {
  val io = new Bundle {
    // input datapath
    val inst = in Bits (32 bits)
    val regWrData = in Bits (32 bits)
    // control signals
    val regWrEn = in Bool ()
    val immType = in(ImmType())
    // output datapath
    val regSrc1 = out Bits (32 bits)
    val regSrc2 = out Bits (32 bits)
    val imm = out SInt (32 bits)
  }

  val regFile = RegisterFile()
  val immGen = ImmGen()

  regFile.io.addrRe1 := io.inst(19 downto 15).asUInt
  regFile.io.addrRe2 := io.inst(24 downto 20).asUInt
  regFile.io.addrWr := io.inst(11 downto 7).asUInt
  regFile.io.writeEn := io.regWrEn
  regFile.io.dataWr := io.regWrData
  io.regSrc1 := regFile.io.dataRe1
  io.regSrc2 := regFile.io.dataRe2

  immGen.io.immType := io.immType
  immGen.io.inst := io.inst
  io.imm := immGen.io.imm
}
