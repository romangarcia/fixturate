package dridco.tests.fixturate

import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.Try
import scala.util.Try._
import java.io.Reader
import java.util.Date
import java.text.DateFormat

case class FixtureData(variants: List[FixtureVariant]) {
	private lazy val variantsMap = variants map ( s => (s.name -> s) ) toMap
	def variant(name: String): Option[FixtureVariant] = variantsMap.get(name)
}

case class FixtureProperty(key: String, values: List[FixtureValue]) 

case class FixtureVariant(name: String, properties: List[FixtureProperty]) {
	private lazy val propertiesMap = properties map ( p => (p.key -> p.values) ) toMap
	def property[T](key: String): Option[T] = propertiesMap.get(key).flatMap(_.headOption.asInstanceOf[Option[T]])
	def propertyList[T](key: String): Option[List[T]] = propertiesMap.get(key).map(_.asInstanceOf[List[T]])
}

sealed abstract class FixtureValue
case class FixtureLiteral(value: Any) extends FixtureValue
case class FixtureRef(variant: String, model: Option[Class[_]]) extends FixtureValue

object FixtureParser extends JavaTokenParsers {
	private def stringsAndWhitespaces = """[\w\s]+""".r

	private def boolean = "(?i)true|false".r ^^ { b => FixtureLiteral(b.toBoolean) }
	private def long = wholeNumber ^^ { n => FixtureLiteral(n.toLong) }
	private def double = decimalNumber ^^ { n => FixtureLiteral(n.toDouble) }
	private def string = stringLiteral ^^ { s => FixtureLiteral(s.substring(1, s.size - 1)) }
	private def ref = "$[" ~> stringsAndWhitespaces <~ "]" ^^ { r => FixtureRef(r, None) } 

	private def propertyKey = ident
    private def equalSign = ":|=".r
    private def propertyValue: Parser[FixtureValue] = (double | long | string | boolean | ref)
    
    private def property = (propertyKey ~ equalSign ~ repsep(propertyValue, ",")) ^^ {
        case k ~ _ ~ v => FixtureProperty(k, v)
    } 
    
    private def propertyList = rep(property)
    
    private def variant = (("[" ~> stringsAndWhitespaces <~ "]") ~ propertyList) ^^ {
        case (variantName ~ pList) => FixtureVariant(variantName, pList)
    }
    
    private def expression = rep(variant)
    
    def parse(input: Reader): Try[FixtureData] = {
        parseAll(expression, input) match {
            case Success(result, _) => Try(FixtureData(result))
            case NoSuccess(msg, next) => 
            	new scala.util.Failure(new Exception(msg + " at '" + next.pos.longString + "'"))
        }
        
    }
}