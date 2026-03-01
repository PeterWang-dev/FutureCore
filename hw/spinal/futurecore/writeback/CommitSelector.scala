package futurecore.writeback

import spinal.core._

object CommitSelector {
  object CommitSource extends SpinalEnum {
    val Result, Memory = newElement()
  }
}

class CommitSelector extends Component {
  import CommitSelector._

  val io = new Bundle {
    val inResult = in port SInt(32 bits)
    val inMemory = in port Bits(32 bits)
    val selSource = in port CommitSource()
    val outCommit = out port Bits(32 bits)
  }

  io.outCommit := io.selSource.mux {
    CommitSource.Result -> io.inResult.asBits
    CommitSource.Memory -> io.inMemory
  }
}
