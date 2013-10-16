package fixturate

import org.scalatest.WordSpec
import dridco.tests.fixturate.FixtureParser
import java.io.Reader
import java.io.StringReader
import org.scalatest.matchers.ShouldMatchers
import scala.util.Success

class FixtureParserSpec extends WordSpec with ShouldMatchers {

    val singleSection = """
[prueba]
firstName = "Pepe"
lastName  = "Mugica"
age       = 65        
"""
        
	val multiSection = singleSection + """
[otra prueba]
firstName = "Cristina"
lastName  = "Kirchner"
age       = 50
"""
        
    implicit def string2Reader(s: String) = new {
        def reader: Reader = new java.io.StringReader(s)
    }
        
    "Fixture Parser" should {
        "parse single section" in {
            FixtureParser.parse(singleSection.reader) should be ('success)
        }
        
        "parse multi section" in {
        	FixtureParser.parse(multiSection.reader) should be ('success)
        }
        
        "parse 2 sections in multi sections" in {
        	FixtureParser.parse(multiSection.reader).get.variants.length should be (2)
        }
        
        "parse section names" in {
        	val file = FixtureParser.parse(multiSection.reader).get
			file.variants(0).name should be ("prueba")
        	file.variants(1).name should be ("otra prueba")
        }
        
    }
}