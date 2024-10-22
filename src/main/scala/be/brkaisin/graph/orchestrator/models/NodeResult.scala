package be.brkaisin.graph.orchestrator.models

/** Represents the result of a node computation.
  *
  * @tparam O
  *   the output type of the node
  * @tparam E
  *   the error type of the node
  */
enum NodeResult[+O, +E]:
  case Success(output: O)
  case Failure(error: E)
