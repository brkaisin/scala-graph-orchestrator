package be.brkaisin.graph.orchestrator.utils

import scala.deriving.*
import scala.compiletime.{erasedValue, summonInline}
import scala.Tuple

trait OptionFields[A]:
  def isComplete(value: A): Boolean
  def merge(existing: A, updates: A): A
  def mergeField[T](existing: A, index: Int, newValue: Option[T]): A
  def empty: A

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

  // derive OptionFields for a case class using a Mirror
  given derived[A](using
      m: Mirror.ProductOf[A],
      elems: TupleOptionFields[m.MirroredElemTypes]
  ): OptionFields[A] =
    val elemInstances = elems.instances

    new OptionFields[A]:
      def isComplete(value: A): Boolean =
        value
          .asInstanceOf[Product]
          .productIterator
          .zip(elemInstances.iterator)
          .forall {
            case (Some(_), _) => true
            case (None, _)    => false
            case _            => true // Non-Option fields are assumed complete
          }

      def merge(existing: A, updates: A): A =
        val mergedValues = existing
          .asInstanceOf[Product]
          .productIterator
          .zip(updates.asInstanceOf[Product].productIterator)
          .zip(elemInstances.iterator)
          .map {
            case ((Some(_), Some(updated)), _) => Some(updated)
            case ((None, Some(updated)), _)    => Some(updated)
            case ((existing, None), _)         => existing
            case ((_, updated), _)             => updated
          }
          .toList

        createInstance(mergedValues)

      def mergeField[T](existing: A, index: Int, newValue: Option[T]): A =
        val values = existing.asInstanceOf[Product].productIterator.toArray

        val updatedValues = values.zipWithIndex.map {
          case (value, i) if i == index =>
            // Handle the newValue correctly by unwrapping if it’s already an Option
            newValue match
              case Some(optVal: Option[?]) => optVal
              case _                       => newValue
          case (value, _) => value
        }

        createInstance(updatedValues.toList)

      def empty: A =
        val emptyValues = elemInstances.map(_ => None)
        createInstance(emptyValues)

  private def createInstance[A](values: List[Any])(using
      m: Mirror.ProductOf[A]
  ): A =
    m.fromProduct(Tuple.fromArray(values.toArray))
