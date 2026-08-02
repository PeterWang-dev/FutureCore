package futurecore.ip.blackbox

import spinal.core._

class rom_dpi extends BlackBox {
  val io = new Bundle {
    val clk = in port Bool()
    val resetn = in port Bool()
    val valid = in port Bool()
    val raddr = in port UInt(32 bits)
    val rdata = out port Bits(32 bits)
  }

  noIoPrefix()
  mapClockDomain(clock = io.clk, reset = io.resetn, resetActiveLevel = LOW)
  addRTLPath("hw/verilog/mem_dpi.sv")
}

class ram_dpi extends BlackBox {
  val io = new Bundle {
    val clk = in port Bool()
    val resetn = in port Bool()

    val ar = new Bundle {
      val valid = in port Bool()
      val addr = in port UInt(32 bits)
    }

    val r = new Bundle {
      val data = out port Bits(32 bits)
    }

    val aw = new Bundle {
      val valid = in port Bool()
      val addr = in port UInt(32 bits)
    }

    val w = new Bundle {
      val data = in port Bits(32 bits)
      val strb = in port Bits(8 bits)
    }
  }

  noIoPrefix()
  mapClockDomain(clock = io.clk, reset = io.resetn, resetActiveLevel = LOW)
  addRTLPath("hw/verilog/mem_dpi.sv")

  private def renameIO(): Unit = {
    io.flatten.foreach(bt => {
      bt.setName(
        bt.getName()
          .replace("ar_", "ar")
          .replace("r_", "r")
          .replace("aw_", "aw")
          .replace("w_", "w")
      )
    })
  }

  addPrePopTask(() => renameIO())
}
