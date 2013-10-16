package fixturate.model

import scala.beans.BeanProperty

case class TestOrder(@BeanProperty val item: Long, @BeanProperty val description: String, @BeanProperty val user: TestUser)