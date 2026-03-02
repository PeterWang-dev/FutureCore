package futurecore.decode

import spinal.core._
import spinal.core.fiber.Retainer
import spinal.lib.logic.{DecodingSpec, Masked}
import spinal.lib.misc.plugin.FiberPlugin

import scala.collection.mutable.{Map, ArrayBuffer}

import futurecore.riscv.{Instruction, InstructionSet}

object CtrlService {
  class CtrlDef[T <: BaseType, V](val signalType: T, val default: V) {
    private val valueMap = Map[V, ArrayBuffer[Instruction]]()

    def setWhen(value: V, insts: Instruction*): this.type = {
      require(insts.nonEmpty, "At least one instruction must be provided")
      val buffer = valueMap.getOrElseUpdate(value, ArrayBuffer())
      buffer ++= insts
      this
    }

    def setWhen(
        value: V,
        insts: Seq[Instruction]
    )(implicit d: DummyImplicit): this.type =
      setWhen(value, insts: _*)

    protected[decode] def toDecodingSpec: DecodingSpec[T] = {
      val spec = new DecodingSpec[T](HardType(signalType))
      spec.setDefault(Masked(default))
      valueMap.foreach { case (signalValue, instructions) =>
        spec.addNeeds(instructions.map(_.toMasked), Masked(signalValue))
      }
      spec
    }
  }

  object CtrlDef {
    def apply[T <: BaseType, V <: BaseType](
        signalType: T,
        default: V
    ): CtrlDef[T, V] =
      new CtrlDef(signalType, default)

    def apply[T <: SpinalEnum](
        signalType: SpinalEnumCraft[T],
        default: SpinalEnumElement[T]
    ): CtrlDef[SpinalEnumCraft[T], SpinalEnumElement[T]] =
      new CtrlDef(signalType, default)
  }
}

trait CtrlService {
  import CtrlService._
  val ctrlSignals = ArrayBuffer[CtrlDef[_ <: BaseType, _]]()
  val ctrlLock = Retainer()

  def registerCtrlSignal[T <: BaseType](signal: CtrlDef[T, _]) = {
    ctrlSignals += signal
  }

  def getCtrlSignal[T <: BaseType](key: CtrlDef[T, _]): T
}
