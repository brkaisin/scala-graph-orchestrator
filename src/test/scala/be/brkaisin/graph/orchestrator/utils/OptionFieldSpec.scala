package be.brkaisin.graph.orchestrator.utils

import be.brkaisin.graph.orchestrator.utils.OptionFields.given
import be.brkaisin.graph.orchestrator.utils.OptionFields.*
import org.scalatest.funsuite.AnyFunSuite

case class User(name: Option[String], age: Option[Int], email: Option[String])
case class Product(
    name: Option[String],
    price: Option[Double],
    stock: Option[Int]
)
class OptionFieldsSpec extends AnyFunSuite:

  // implicit instance lookup method for empty
  def empty[A](using optionFields: OptionFields[A]): A = optionFields.empty

  test("isComplete should return true when all fields are defined") {
    val user = User(Some("Alice"), Some(30), Some("alice@example.com"))
    assert(user.isComplete)
  }

  test("isComplete should return false when some fields are not defined") {
    val user = User(Some("Alice"), None, Some("alice@example.com"))
    assert(!user.isComplete)
  }

  test("merge should combine fields from two instances") {
    val user1      = User(Some("Alice"), None, Some("alice@example.com"))
    val user2      = User(None, Some(25), None)
    val mergedUser = user1.merge(user2)

    assert(
      mergedUser == User(Some("Alice"), Some(25), Some("alice@example.com"))
    )
  }

  test("mergeField should update a specific field by index") {
    val user        = User(Some("Alice"), None, Some("alice@example.com"))
    val updatedUser = user.mergeField(1, Some(30))

    assert(
      updatedUser == User(Some("Alice"), Some(30), Some("alice@example.com"))
    )
  }

  test("mergeField should not change the instance if the new value is None") {
    val user        = User(Some("Alice"), None, Some("alice@example.com"))
    val updatedUser = user.mergeField(1, None)

    assert(updatedUser == user)
  }

  test("merge should work for a different case class") {
    val product1      = Product(Some("Laptop"), None, Some(50))
    val product2      = Product(None, Some(1200.00), None)
    val mergedProduct = product1.merge(product2)

    assert(mergedProduct == Product(Some("Laptop"), Some(1200.00), Some(50)))
  }

  test("mergeField should work for a different case class") {
    val product = Product(Some("Laptop"), None, Some(50))
    val updatedProduct =
      product.mergeField(1, Some(999.99))

    assert(updatedProduct == Product(Some("Laptop"), Some(999.99), Some(50)))
  }

  test("empty should return an instance with all fields as None for User") {
    val emptyUser = empty[User]

    assert(emptyUser == User(None, None, None))
  }

  test("empty should return an instance with all fields as None for Product") {
    val emptyProduct = empty[Product]

    assert(emptyProduct == Product(None, None, None))
  }

  test("empty instance should be incomplete for User") {
    val emptyUser = empty[User]

    assert(emptyUser.isIncomplete)
  }

  test("empty instance should be incomplete for Product") {
    val emptyProduct = empty[Product]

    assert(emptyProduct.isIncomplete)
  }
