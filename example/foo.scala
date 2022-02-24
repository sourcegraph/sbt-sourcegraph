case class A(a: Int)
case class B(a: Int)
object Main {
  A.apply(a = 42).copy(a = 41).productElement(0)
  B.apply(a = 42).copy(a = 41).productElement(0)
}
