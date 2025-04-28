package futurecore.lib

import futurecore.Config
import spinal.core.sim._
import scala.util.Random
import org.scalatest.flatspec.AnyFlatSpec
import spinal.core.log2Up

class AluTest extends AnyFlatSpec {
  object Util {
    def generateTestCases(n: Int, width: Int, hasNegative: Boolean = true): Seq[(Int, Int)] = {
      val random = new Random()
      (0 until n).map { _ =>
        if (hasNegative) {
          val a = random.between(-(1 << (width - 1)), 1 << (width - 1) - 1)
          val b = random.between(-(1 << (width - 1)), 1 << (width - 1) - 1)
          (a, b)
        } else {
          val a = random.between(0, (1 << width) - 1)
          val b = random.between(0, (1 << width) - 1)
          (a, b)
        }
      }
    }
  }

  it should "correctly perform 32 bit addition and subtraction" in {
    Config.sim.compile(Alu(32)).doSim { dut =>
      val testCases = Util.generateTestCases(100, 32)
      for ((a, b) <- testCases) {
        dut.io.inA #= a & 0xffffffffL
        dut.io.inB #= b & 0xffffffffL
        dut.io.op #= AluOp.ADD
        sleep(1)
        assert(
          // Do not need to mask here as we are using 32 bits
          dut.io.outRes.toInt == (a + b),
          s"Failed for a=$a, b=$b on ADD, got ${dut.io.outRes.toInt}"
        )
        dut.io.op #= AluOp.SUB
        sleep(1)
        assert(
          dut.io.outRes.toInt == (a - b),
          s"Failed for a=$a, b=$b on SUB, got ${dut.io.outRes.toInt}"
        )
      }
    }
  }

  it should "correctly perform 32 bit shift operations" in {
    Config.sim.compile(Alu(32)).doSim { dut =>
      val testCases = Util.generateTestCases(100, 32)
      for ((a, b) <- testCases) {
        val shiftAmount = b & 0x1f // Ensure shift amount is within 0-31
        dut.io.inA #= a & 0xffffffffL
        dut.io.inB #= shiftAmount
        dut.io.op #= AluOp.SLL
        sleep(1)
        assert(
          dut.io.outRes.toInt == (a << shiftAmount),
          s"Failed for a=$a, b=$shiftAmount on SLL, got ${dut.io.outRes.toInt & 0xff}"
        )
        dut.io.op #= AluOp.SRL
        sleep(1)
        assert(
          dut.io.outRes.toInt == (a >>> shiftAmount),
          s"Failed for a=$a, b=$shiftAmount on SRL, got ${dut.io.outRes.toInt & 0xff}"
        )
        dut.io.op #= AluOp.SRA
        sleep(1)
        assert(
          dut.io.outRes.toInt == (a >> shiftAmount),
          s"Failed for a=$a, b=$shiftAmount on SRA, got ${dut.io.outRes.toInt & 0xff}"
        )
      }
    }
  }

  it should "correctly perform 32 bit comparison operations" in {
    Config.sim.compile(Alu(32)).doSim { dut =>
      val testCases = Util.generateTestCases(100, 32)
      for ((a, b) <- testCases) {
        dut.io.inA #= a & 0xffffffffL
        dut.io.inB #= b & 0xffffffffL
        dut.io.op #= AluOp.SLT
        sleep(1)
        assert(
          dut.io.outRes.toInt == (if (a < b) 1 else 0),
          s"Failed for a=$a, b=$b on SLT, got ${dut.io.outRes.toInt}"
        )
        dut.io.op #= AluOp.SLTU
        sleep(1)
        assert(
          // Cast to Long to handle unsigned comparison correctly in Scala
          dut.io.outRes.toInt == (if ((a & 0xffffffffL) < (b & 0xffffffffL)) 1 else 0),
          s"Failed for a=$a, b=$b on SLTU, got ${dut.io.outRes.toInt}"
        )
      }
    }
  }

  it should "correctly perform 32 bit bitwise operations" in {
    Config.sim.compile(Alu(32)).doSim { dut =>
      val testCases = Util.generateTestCases(100, 32)
      for ((a, b) <- testCases) {
        dut.io.inA #= a & 0xffffffffL
        dut.io.inB #= b & 0xffffffffL
        dut.io.op #= AluOp.XOR
        sleep(1)
        assert(
          dut.io.outRes.toInt == (a ^ b),
          s"Failed for a=$a, b=$b on XOR, got ${dut.io.outRes.toInt}"
        )
        dut.io.op #= AluOp.OR
        sleep(1)
        assert(
          dut.io.outRes.toInt == (a | b),
          s"Failed for a=$a, b=$b on OR, got ${dut.io.outRes.toInt}"
        )
        dut.io.op #= AluOp.AND
        sleep(1)
        assert(
          dut.io.outRes.toInt == (a & b),
          s"Failed for a=$a, b=$b on AND, got ${dut.io.outRes.toInt}"
        )
      }
    }
  }
}
