package a
trait A {
  val b: Int
}

class B extends A {
  override val b = 10
}

class C {
  val a: A = new B()
  println(a.b + new B().b)
}
