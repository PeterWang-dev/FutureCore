package futurecore.execute

import spinal.core._

import futurecore.ip.blackbox.ram_dpi
import futurecore.misc.TypeExtensions.{BitsSextExtension, BitsZeroExtension}

object DataMemory {
  object AccessWidth extends SpinalEnum {
    val Byte, Half, Word = newElement()
  }

  def byte(b: Bits): Bits = b(7 downto 0)
  def half(b: Bits): Bits = b(15 downto 0)
  def word(b: Bits): Bits = b(31 downto 0)

  object WriteMask {
    def byte(): Bits = B"8'b0001"
    def half(): Bits = B"8'b0011"
    def word(): Bits = B"8'b1111"
  }
}

class DataMemory extends Component {
  import DataMemory._

  val io = new Bundle {
    val inSelAccessWidth = in port AccessWidth()
    val inAddr = in port UInt(32 bits)
    val inValidAddr = in port Bool()

    val inEnableReadSext = in port Bool()
    val outDataRead = out port Bits(32 bits)

    val inEnableWrite = in port Bool()
    val inDataWrite = in port Bits(32 bits)
  }

  val mem = new ram_dpi

  mem.io.ar.valid := io.inValidAddr & !io.inEnableWrite
  mem.io.ar.addr := io.inAddr

  val memData = mem.io.r.data
  val dataByteZeroExt = byte(memData).zext.asBits
  val dataByteSignExt = byte(memData).sext.asBits
  val dataHalfZeroExt = half(memData).zext.asBits
  val dataHalfSignExt = half(memData).sext.asBits

  io.outDataRead := io.inSelAccessWidth.mux(
    AccessWidth.Byte -> (io.inEnableReadSext ? dataByteSignExt | dataByteZeroExt),
    AccessWidth.Half -> (io.inEnableReadSext ? dataHalfSignExt | dataHalfZeroExt),
    AccessWidth.Word -> memData
  )

  mem.io.aw.valid := io.inEnableWrite & io.inValidAddr
  mem.io.aw.addr := io.inAddr
  mem.io.w.data := io.inDataWrite
  mem.io.w.strb := io.inSelAccessWidth.mux(
    AccessWidth.Byte -> WriteMask.byte(),
    AccessWidth.Half -> WriteMask.half(),
    AccessWidth.Word -> WriteMask.word()
  )
}
