package futurecore.misc

import spinal.core._

object TypeExtensions {
  implicit class BitsSextExtension(b: Bits) {
    def sext: SInt = b.asSInt.resize(32 bits)
  }

  implicit class BitsZeroExtension(b: Bits) {
    def zext: UInt = b.asUInt.resize(32 bits)
  }
}
