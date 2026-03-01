package futurecore.misc

import spinal.core._

object TypeExtensions {
  implicit class BitsSextExtension(b: Bits) {
    def sext: SInt = b.asSInt.resize(32 bits)
  }
}
