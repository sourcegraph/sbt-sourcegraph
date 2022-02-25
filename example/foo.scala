case class A(a: Int)
case class B(a: Int)
class C(val a: Int)
object Main {
  new A(a = 42)
  A.apply(a = 42).copy(a = 41).productElement(0)
  new B(a = 42)
  B.apply(a = 42).copy(a = 41).productElement(0)
  new C(a = 42).a
}
