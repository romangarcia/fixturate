package fixturate.model

import scala.reflect.BeanProperty


case class TestInvoice(@BeanProperty val number: Long, @BeanProperty val orders: List[TestOrder])
