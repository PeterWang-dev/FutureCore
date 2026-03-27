package futurecore.legacy

import spinal.core._
import spinal.lib._
import futurecore.ip.blackbox.ram_dpi

object MAU {
  object AccessWidth extends SpinalEnum {
    val Byte, Half, Word = newElement()
  }

  case class In() extends Bundle {
    val memAddr = in UInt (32 bits)
    val memWr = in Bits (32 bits)
    in(memAddr, memWr)
  }

  case class Out() extends Bundle {
    val memRe = out Bits (32 bits)
    out(memRe)
  }

  case class Ctrl() extends Bundle with IMasterSlave {
    val memValid = in Bool ()
    val memWrEn = Bool()
    val dataSextEn = Bool()
    val accessType = AccessWidth()
    override def asMaster(): Unit = {
      out(memValid, memWrEn, dataSextEn, accessType)
    }
  }
}

case class MAU() extends Component {
  import MAU._

  val io = new Bundle {
    val input = In()
    val ctrl = slave(Ctrl())
    val output = Out()
  }

  val mem = new ram_dpi
  val dataReaded = Bits(32 bits)

  mem.io.ar.valid := io.ctrl.memValid
  switch(io.ctrl.accessType) {
    mem.io.ar.addr := io.input.memAddr
    is(AccessWidth.Byte) {
      dataReaded := Mux(
        io.ctrl.dataSextEn,
        mem.io.r.data(7 downto 0).asSInt.resize(32 bits).asBits,
        mem.io.r.data(7 downto 0).resize(32 bits)
      )
    }
    is(AccessWidth.Half) {
      dataReaded := Mux(
        io.ctrl.dataSextEn,
        mem.io.r.data(15 downto 0).asSInt.resize(32 bits).asBits,
        mem.io.r.data(15 downto 0).resize(32 bits)
      )
    }
    is(AccessWidth.Word) {
      dataReaded := mem.io.r.data
    }
  }

  io.output.memRe := dataReaded

  mem.io.aw.valid := False
  mem.io.w.strb := B"8'b0"
  mem.io.aw.addr := U"32'b0"
  mem.io.w.data := B"32'b0"

  when(io.ctrl.memWrEn) {
    mem.io.aw.valid := True
    mem.io.aw.addr := io.input.memAddr
    mem.io.w.data := io.input.memWr
    switch(io.ctrl.accessType) {
      is(AccessWidth.Byte) {
        mem.io.w.strb := B"8'b0001"
      }
      is(AccessWidth.Half) {
        mem.io.w.strb := B"8'b0011"
      }
      is(AccessWidth.Word) {
        mem.io.w.strb := B"8'b1111"
      }
    }
  }
}
