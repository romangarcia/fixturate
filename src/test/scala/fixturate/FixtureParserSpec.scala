package fixturate

import org.scalatest.WordSpec
import dridco.tests.fixturate.FixtureParser
import java.io.Reader
import java.io.StringReader
import org.scalatest.matchers.ShouldMatchers
import scala.util.Success

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
        
    implicit def string2Reader(s: String) = new {
        def reader: Reader = new java.io.StringReader(s)
    }
        
    "Fixture Parser" should {
        "parse single variant" in {
            FixtureParser.parse(singleVariant.reader) should be ('success)
        }
        
        "parse multi variant" in {
        	FixtureParser.parse(multiVariant.reader) should be ('success)
        }
        
        "parse 2 variants in multi variants" in {
        	FixtureParser.parse(multiVariant.reader).get.variants.length should be (2)
        }
        
        "parse variant names" in {
        	val data = FixtureParser.parse(multiVariant.reader).get
			data.variants(0).name should be ("pepe")
        	data.variants(1).name should be ("cristina fernandez")
        }
        
        "parse complete variants data" in {
        	val data = FixtureParser.parse(multiVariant.reader).get
			val pepe = data.variant("pepe").get.properties
			pepe(0).key should be ("firstName")
        	pepe(0).value should be ("Pepe")
        	pepe(1).key should be ("lastName")
        	pepe(1).value should be ("Mugica")
        	pepe(2).key should be ("age")
        	pepe(2).value should be (65)
        	pepe(3).key should be ("alive")
        	pepe(3).value should equal (true)
        }
        
    }
}