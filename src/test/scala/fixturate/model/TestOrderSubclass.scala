package fixturate.model

import scala.reflect.BeanProperty

class TestOrderSubclass(item: Long, description: String, user: TestUser,
                         @BeanProperty val price: Double) extends TestOrder(item, description, user)