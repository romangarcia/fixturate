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
import fixturate.model.{TestOrderSubclass, TestUser, TestOrder, TestInvoice}

class FixtureSpec extends WordSpec with ShouldMatchers {

    "Fixture" should {
        "get complete TestUser" in {
            val john = Fixture[TestUser].get("john")
            john.getFirstName should be("John")
            john.getLastName should be("Doe")
            john.age should be(65)
            john.getAge should be(65)
            john.alive should be(true)
        }

        "get complete TestOrder" in {
            val order = Fixture[TestOrder].get()
            order.item should be(4321L)
            order.description should be("Some Default Order For John")
            order.user should be(TestUser("John", "Doe"))
        }

        "get complete TestInvoice" in {
            val invoice = Fixture[TestInvoice].get("invoice for john and jane")
            invoice.number should be(1L)
            invoice.orders should have length (2)
            invoice.orders(0).item should be (1L)
            invoice.orders(0).description should be ("Some Order For John")
            invoice.orders(0).user should be (TestUser("John", "Doe"))
        }

        "get any TestInvoice with similar probability" in {
            val anys = (1 to 100) map {
                i =>
                    Fixture[TestInvoice].any()
            }

            val diff = anys.count(_.getNumber == 1L) - anys.count(_.getNumber == 2)

            // similar probability between fixtures
            assert(diff < 10)
        }

        "get all TestInvoices" in {
            Fixture[TestInvoice].all() should have length (4)
        }

        "get any number of TestInvoices" in {
            Fixture[TestInvoice].any(2) should have length (2)
        }

        "get Fixture ref for specified type" in {
            val invoice: TestInvoice = Fixture[TestInvoice].get("invoice with overriden order type")
            invoice.orders should have length (2)
            invoice.orders(0).isInstanceOf[TestOrderSubclass] should be (true)
        }

    }


}