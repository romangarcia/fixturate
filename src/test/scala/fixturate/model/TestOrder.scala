package fixturate.model

import scala.reflect.BeanProperty

class TestOrder(@BeanProperty val item: Long, @BeanProperty val description: String, @BeanProperty val user: TestUser)