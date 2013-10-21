/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fixturate

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.WordSpec
import dridco.tests.fixturate.Fixture
import fixturate.model.{TestUser, TestOrder, TestInvoice}

class FixtureSpec extends WordSpec with ShouldMatchers {

  "Fixture" should {
    "retrieve complete TestUser" in {
      val pepe = Fixture[TestUser].get("pepe")
      println(pepe)
      pepe.getFirstName should be("Pepe")
      pepe.getLastName should be("Mugica")
      pepe.age should be(65)
      pepe.getAge should be(65)
    }

    "retrieve complete TestOrder" in {
      val order = Fixture[TestOrder].get()
      println(order)

      order.item should be(4321L)
      order.description should be("Un producto berreta")
      order.user should be(TestUser("Pepe", "Mugica"))
    }

    "retrieve complete TestInvoice" in {
      val invoice = Fixture[TestInvoice].get("default")
      println(invoice)

      invoice.number should be(1L)
      invoice.orders should have length (2)
      invoice.orders(0) should be(TestOrder(1L, "Producto Uno", TestUser("Pepe", "Mugica")))
      invoice.orders(1) should be(TestOrder(2L, "Producto Dos", TestUser("Cristina", "Kirchner")))
    }

    /*"retrieve complete JavaInvoice" in {
      val invoice = Fixture[JavaInvoice].get()
      println(invoice)

      invoice.getNumber should be(1L)
      invoice.getOrders should have length (2)
      val firstOrder = invoice.getOrders.get(0)
      firstOrder.getDescription should be("Producto Uno")
      firstOrder.getItem should be(1L)
      firstOrder.getUser.getFirstName should be("Pepe")
    }*/
  }


}