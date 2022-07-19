package a
trait A {
  val b: Int
}

class B extends A {
  override val b = 10
}
