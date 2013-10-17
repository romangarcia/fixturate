package fixturate.model

import scala.beans.BeanProperty

case class TestInvoice(@BeanProperty val number: Long, @BeanProperty val orders: List[TestOrder])
