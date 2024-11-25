package a

import org.junit.Assert

object A extends App {
  def generator = geny.Generator(1)
  Assert.assertEquals(generator, "")
}
