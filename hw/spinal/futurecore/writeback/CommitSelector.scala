package futurecore.writeback

import spinal.core._

object CommitSelector {
  object CommitSource extends SpinalEnum {
    val AluResults, CsrValue, Memory = newElement()
  }
}

class CommitSelector extends Component {
  import CommitSelector._

  val io = new Bundle {
    val inAluResult = in port SInt(32 bits)
    val inCsrValue = in port Bits(32 bits)
    val inMemory = in port Bits(32 bits)
    val inSelSource = in port CommitSource()
    val outCommit = out port Bits(32 bits)
  }

  io.outCommit := io.inSelSource.mux(
    CommitSource.AluResults -> io.inAluResult.asBits,
    CommitSource.CsrValue   -> io.inCsrValue,
    CommitSource.Memory     -> io.inMemory
  )
}
