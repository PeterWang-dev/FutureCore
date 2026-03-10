package futurecore.riscv

import spinal.core._
import spinal.lib.logic.Masked

case class Instruction(
    pattern: MaskedLiteral,
    fields: Seq[BitField]
) {
  require(
    {
      // Flatten all ranges from all BitFields and check for overlaps
      val allRanges = fields.flatMap(_.ranges)
      allRanges.sortBy(_.head).sliding(2).forall(pair => pair.head.last < pair.last.head)
    },
    "Bit fields must not overlap"
  )
  def toMasked = Masked(pattern)
}

abstract class BitField(val ranges: Range*) {
  private val name = this.getClass.getSimpleName.toLowerCase

  // Validate that ranges within this BitField don't overlap
  require(
    ranges.length <= 1 ||
      ranges
        .sortBy(_.head)
        .sliding(2)
        .forall(pair => pair.head.last < pair.last.head),
    s"BitField $name has overlapping ranges: ${ranges.mkString(", ")}"
  )

  def extract(from: Bits): Bits = {
    ranges.map(from(_)).reduceLeft(_ ## _)
  }

  override def toString: String = {
    s"$name [$ranges]"
  }
}

trait InstructionSet {
  def ident: String
  def instructions: Seq[Instruction]
  override def toString(): String = s"Isa Extension: ${ident}\n\tAll Instructions: ${instructions}"
}
