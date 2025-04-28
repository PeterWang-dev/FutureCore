package futurecore

import spinal.core._
import futurecore.blackbox.ram_dpi

object AccessWidth extends SpinalEnum {
  val Byte, Half, Word = newElement()
}

case class MemoryAccessUnit() extends Component {
  val io = new Bundle {
    val memValid = in Bool ()
    val memAddr = in UInt (32 bits)
    val memWr = in Bits (32 bits)
    val memWrEn = in Bool ()
    val dataSextEn = in Bool ()
    val accessType = in(AccessWidth())
    val memRe = out Bits (32 bits)
  }

  val mem = new ram_dpi
  val dataReaded = Bits(32 bits)

  mem.io.valid := io.memValid
  switch(io.accessType) {
    mem.io.raddr := io.memAddr
    is(AccessWidth.Byte) {
      dataReaded := Mux(
        io.dataSextEn,
        mem.io.rdata(7 downto 0).asSInt.resize(32 bits).asBits,
        mem.io.rdata(7 downto 0).resize(32 bits)
      )
    }
    is(AccessWidth.Half) {
      dataReaded := Mux(
        io.dataSextEn,
        mem.io.rdata(15 downto 0).asSInt.resize(32 bits).asBits,
        mem.io.rdata(15 downto 0).resize(32 bits)
      )
    }
    is(AccessWidth.Word) {
      dataReaded := mem.io.rdata
    }
  }

  mem.io.wen := False
  mem.io.wmask := B"8'b0"
  mem.io.waddr := U"32'b0"
  mem.io.wdata := B"32'b0"

  when(io.memWrEn) {
    mem.io.wen := True
    mem.io.waddr := io.memAddr
    mem.io.wdata := io.memWr
    switch(io.accessType) {
      is(AccessWidth.Byte) {
        mem.io.wmask := B"8'b0001"
      }
      is(AccessWidth.Half) {
        mem.io.wmask := B"8'b0011"
      }
      is(AccessWidth.Word) {
        mem.io.wmask := B"8'b1111"
      }
    }
  }

  io.memRe := Mux(io.memWrEn, B"32'b0", dataReaded)
}
