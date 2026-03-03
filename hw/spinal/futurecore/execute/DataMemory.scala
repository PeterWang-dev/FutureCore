package futurecore.execute

import spinal.core._

import futurecore.ip.blackbox.ram_dpi
import futurecore.misc.TypeExtensions.BitsSextExtension

object DataMemory {
  object AccessWidth extends SpinalEnum {
    val Byte, Half, Word = newElement()
  }

  def byte(b: Bits): Bits = b(7 downto 0)
  def half(b: Bits): Bits = b(15 downto 0)
  def word(b: Bits): Bits = b(31 downto 0)

  object WriteMask {
    val Byte = B"8'b0001"
    val Half = B"8'b0011"
    val Word = B"8'b1111"
  }

}

class DataMemory extends Component {
  import DataMemory._

  val io = new Bundle {
    val selAccessWidth = in port AccessWidth()
    val inAddr = in port UInt(32 bits)

    val enableReadSext = in port Bool()
    val outDataRead = out port Bits(32 bits)

    val enableWrite = in port Bool()
    val validDataWrite = in port Bool()
    val inDataWrite = in port Bits(32 bits)
  }

  val mem = new ram_dpi

  mem.io.valid := io.validDataWrite
  mem.io.raddr := io.inAddr

  val memData = mem.io.rdata
  val dataByteZeroExt = byte(memData).resized
  val dataByteSignExt = byte(memData).sext.asBits
  val dataHalfZeroExt = half(memData).resized
  val dataHalfSignExt = half(memData).sext.asBits

  io.outDataRead := io.selAccessWidth.mux(
    AccessWidth.Byte -> (io.enableReadSext ? dataByteSignExt | dataByteZeroExt),
    AccessWidth.Half -> (io.enableReadSext ? dataHalfSignExt | dataHalfZeroExt),
    AccessWidth.Word -> memData
  )

  mem.io.wen := io.enableWrite
  mem.io.waddr := io.inAddr
  mem.io.wdata := io.inDataWrite
  mem.io.wmask := io.selAccessWidth.mux(
    AccessWidth.Byte -> B"8'b0001",
    AccessWidth.Half -> B"8'b0011",
    AccessWidth.Word -> B"8'b1111"
  )
}
