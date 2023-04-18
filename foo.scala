package foo
case class MinimizedCaseClass(value: String) {
  def this() = this(value = "value")
}
object MinimizedCaseClass {
  def main(): Unit = {
    println(MinimizedCaseClass.apply(value = "value1").copy(value = "value2").value)
  }
}
