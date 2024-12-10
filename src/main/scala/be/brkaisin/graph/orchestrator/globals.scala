package be.brkaisin.graph.orchestrator

import be.brkaisin.graph.orchestrator.utils.OptionFields
import be.brkaisin.graph.orchestrator.utils.OptionFields.*

object globals:

  type OptionsTuple[Tup <: Tuple]     = Tuple.Map[Tup, Option]
  type TupleFromOptions[Tup <: Tuple] = Tuple.InverseMap[Tup, Option]

  // perhaps use it later with case classes
  type OptionsProduct[P <: Product] =
    OptionsTuple[scala.deriving.Mirror.ProductOf[P]#MirroredElemTypes]

  def getTupleOptions[Tup <: Tuple](tup: OptionsTuple[Tup])(using
      optionFields: OptionFields[OptionsTuple[Tup]]
  ): Tup =
    if tup.isIncomplete then
      throw new IllegalArgumentException("OptionsTuple is not complete")
    else
      Tuple
        .fromArray(
          tup.productIterator.map {
            case Some(value) => value
            // case below is impossible because of the isComplete check
            case None => throw new NoSuchElementException("Option was None")
          }.toArray
        )
        .asInstanceOf[Tup]
