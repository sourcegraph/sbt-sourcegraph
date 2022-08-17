case class AccountId(x: Int) extends AnyVal

object Main {
  def main(): Unit = {
    AccountId(42).x
    val x = classOf[AccountId]
    val y = List.empty[AccountId]
  }
}
