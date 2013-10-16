package fixturate.model

import dridco.fixturate.Fixture
import scala.beans.BeanProperty

case class TestUser(@BeanProperty val firstName: String, @BeanProperty val lastName: String) {
  @BeanProperty
  var age: Int = 0
  @BeanProperty
  var alive: Boolean = _
}