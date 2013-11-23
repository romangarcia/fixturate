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

import _root_.java.io.Reader
import _root_.java.io.StringReader
import org.scalatest.WordSpec
import dridco.tests.fixturate._
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import dridco.tests.fixturate.FixtureRef
import dridco.tests.fixturate.FixtureLiteral
import dridco.tests.fixturate.FixtureEnum

@RunWith(classOf[JUnitRunner])
class FixtureParserSpec extends WordSpec with ShouldMatchers {

    val singleVariant = """
[pepe]
firstName = "Pepe"
lastName  = "Mugica"
age       = 65        
alive     = true        
                        """

    val multiVariant = singleVariant + """
[cristina fernandez]
firstName = "Cristina"
lastName  = "Kirchner"
age       = 50
alive     = false	
                                       """

    val listsVariant = """
[lista de strings]
strings = "Lula", "Cristina", "Pepe", "Fernando", "Evo"

[lista de doubles]
doubles = 5.31, 3.23, 6.43, 8.54, 10

[lista de booleans]
booleans = true, false, TRUE, FALSE

[lista de refs]
refs  = $[lista de strings], $[lista de doubles], $[lista de booleans] 	    
                       """

    val enumsVariant =
        """
[enumsVariant]
singleEnum = AN_ENUM
listOfEnums = ENUM_ONE, ENUM_TWO
        """

    val refsVariant =
        """
[refsVariant]
ref1 = $[no class]
ref2 = $[java.lang.String:a class defined]
        """

    implicit def string2Reader(s: String) = new {
        def reader: Reader = new StringReader(s)
    }

    "Fixture Parser" should {
        "parse single variant" in {
            FixtureParser.parse(singleVariant.reader) should be('right)
        }

        "parse multi variant" in {
            FixtureParser.parse(multiVariant.reader) should be('right)
        }

        "parse 2 variants in multi variants" in {
            FixtureParser.parse(multiVariant.reader).right.get.variants.length should be(2)
        }

        "parse variant names" in {
            val data = FixtureParser.parse(multiVariant.reader).right.get
            data.variants(0).name should be("pepe")
            data.variants(1).name should be("cristina fernandez")
        }

        "parse complete variants data" in {
            val data = FixtureParser.parse(multiVariant.reader).right.get
            val pepe = data.variant("pepe").get.properties
            pepe(0).key should be("firstName")
            pepe(0).values should be(List(FixtureLiteral("Pepe")))
            pepe(1).key should be("lastName")
            pepe(1).values should be(List(FixtureLiteral("Mugica")))
            pepe(2).key should be("age")
            pepe(2).values should be(List(FixtureLiteral(65)))
            pepe(3).key should be("alive")
            pepe(3).values should equal(List(FixtureLiteral(true)))
        }

        "parse list of literals" in {
            val data = FixtureParser.parse(listsVariant.reader).right.get

            val lista = data.variant("lista de strings").get
            lista.propertyList("strings") should be('defined)

            val names = lista.propertyList[FixtureLiteral]("strings").get
            names should have length (5)
            names(0) should be(FixtureLiteral("Lula"))
            names(1) should be(FixtureLiteral("Cristina"))
            names(2) should be(FixtureLiteral("Pepe"))
            names(3) should be(FixtureLiteral("Fernando"))
            names(4) should be(FixtureLiteral("Evo"))
        }

        "parse list of refs" in {
            val data = FixtureParser.parse(listsVariant.reader).right.get

            val lista = data.variant("lista de refs").get
            lista.propertyList("refs") should be('defined)

            val refs = lista.propertyList[FixtureLiteral]("refs").get
            refs should have length (3)
            refs(0) should be(FixtureRef("lista de strings", None))
            refs(1) should be(FixtureRef("lista de doubles", None))
            refs(2) should be(FixtureRef("lista de booleans", None))
        }

        "parse list of doubles" in {
            val data = FixtureParser.parse(listsVariant.reader).right.get

            val lista = data.variant("lista de doubles").get
            lista.propertyList("doubles") should be('defined)

            val doubles = lista.propertyList[FixtureLiteral]("doubles").get
            doubles should have length (5)
            doubles.forall(_.value.isInstanceOf[Double])
            doubles.forall(_.value.asInstanceOf[Double] <= 10)
        }

        "parse list of booleans" in {
            val data = FixtureParser.parse(listsVariant.reader).right.get

            val lista = data.variant("lista de booleans").get
            lista.propertyList("booleans") should be('defined)

            val booleans = lista.propertyList[FixtureLiteral]("booleans").get
            booleans should have length (4)
            booleans.forall(_.value.isInstanceOf[Boolean])
            booleans(0) should be(FixtureLiteral(true))
            booleans(1) should be(FixtureLiteral(false))
            booleans(2) should be(FixtureLiteral(true))
            booleans(3) should be(FixtureLiteral(false))
        }

        "parse enums" in {
            val data = FixtureParser.parse(enumsVariant.reader).right.get
            data.variants should have length (1)
            val enums = data.variant("enumsVariant").get
            enums.properties should have length (2)
            enums.properties(0) should be(FixtureProperty("singleEnum", List(FixtureEnum("AN_ENUM", None))))
            enums.properties(1) should be(FixtureProperty("listOfEnums", List(FixtureEnum("ENUM_ONE", None), FixtureEnum("ENUM_TWO", None))))
        }

        "parse refs" in {
            val data = FixtureParser.parse(refsVariant.reader).right.get
            data.variants should have length (1)

            val refs = data.variant("refsVariant").get
            refs.properties(0) should be (FixtureProperty("ref1", List(FixtureRef("no class", None))))
            refs.properties(1) should be (FixtureProperty("ref2", List(FixtureRef("a class defined", Some(classOf[String])))))
        }

    }
}