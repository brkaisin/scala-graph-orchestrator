package be.brkaisin.graph.orchestrator.utils

import scala.Tuple
import scala.deriving.*

trait OptionFields[T]:
  def isComplete(value: T): Boolean
  def isIncomplete(value: T): Boolean = !isComplete(value)
  def merge(existing: T, updates: T): T
  def mergeField[U](existing: T, index: Int, newValue: Option[U]): T
  def empty: T

object OptionFields:

  // provide a given instance for Option[T] to avoid deriving mirrors for Option
  given optionFields[T]: OptionFields[Option[T]] with
    def isComplete(value: Option[T]): Boolean = value.isDefined

    def merge(existing: Option[T], updates: Option[T]): Option[T] =
      updates.orElse(existing)

    def mergeField[U](
        existing: Option[T],
        index: Int,
        newValue: Option[U]
    ): Option[T] =
      newValue match
        case Some(v) => Some(v.asInstanceOf[T])
        case None    => existing

    def empty: Option[T] = None

  extension [T](t: T)(using optionFields: OptionFields[T])
    def isComplete: Boolean = optionFields.isComplete(t)
    def isIncomplete: Boolean = optionFields.isIncomplete(t)
    def merge(other: T): T  = optionFields.merge(t, other)
    def mergeField[U](index: Int, newValue: Option[U]): T =
      optionFields.mergeField(t, index, newValue)

  // type class to derive OptionFields for all elements of a tuple
  trait TupleOptionFields[T <: Tuple]:
    def instances: List[OptionFields[?]]

  object TupleOptionFields:
    given emptyTupleOptionFields: TupleOptionFields[EmptyTuple] with
      def instances: List[OptionFields[?]] = Nil

    given nonEmptyTupleOptionFields[H, T <: Tuple](using
        head: OptionFields[H],
        tail: TupleOptionFields[T]
    ): TupleOptionFields[H *: T] with
      def instances: List[OptionFields[?]] = head :: tail.instances

  // Derive OptionFields for a case class using a Mirror
  given derived[T <: Product](using
      m: Mirror.ProductOf[T],
      elems: TupleOptionFields[m.MirroredElemTypes]
  ): OptionFields[T] with
    def isComplete(value: T): Boolean =
      value.productIterator
        .forall {
          case Some(_) => true
          case None    => false
          case _       => true // non-Option fields are assumed complete
        }

    def merge(existing: T, updates: T): T =
      val mergedValues = existing.productIterator
        .zip(updates.productIterator)
        .map {
          case (Some(_), Some(updated)) => Some(updated)
          case (None, Some(updated))    => Some(updated)
          case (existing, None)         => existing
          case (_, updated)             => updated
        }
        .toList

      createInstance(mergedValues)

    def mergeField[U](existing: T, index: Int, newValue: Option[U]): T =
      val updatedValues = existing.productIterator.toList.zipWithIndex.map {
        case (value, i) if i == index =>
          newValue match
            case Some(optVal: Option[?]) => optVal
            case _                       => newValue
        case (value, _) => value
      }

      createInstance(updatedValues)

    def empty: T =
      createInstance(elems.instances.map(_ => None))

  private def createInstance[T](values: List[?])(using
      m: Mirror.ProductOf[T]
  ): T = m.fromProduct(Tuple.fromArray(values.toArray))
