package dridco.fixturate

import scala.reflect.ClassTag
import scala.collection.JavaConverters._
import java.beans.Introspector
import dridco.tests.fixturate.FixtureData
import dridco.tests.fixturate.FixtureParser
import java.io.InputStreamReader
import scala.util.control.NonFatal
import java.lang.reflect.Constructor
import dridco.tests.fixturate.FixtureVariant
import grizzled.slf4j.Logging
import org.apache.commons.beanutils.ConvertUtils
import org.apache.commons.beanutils.ConstructorUtils
import scala.collection.JavaConverters
import org.apache.commons.beanutils.BeanUtils

object Fixture {
    def apply[T](testClass: Class[T]): Fixture[T] = new Fixture[T](testClass)
    def apply[T](implicit manifest: ClassTag[T]): Fixture[T] =
        new Fixture[T](manifest.erasure.asInstanceOf[Class[T]])
}

class Fixture[T](testClass: Class[T]) extends Logging {
    val path = "/" + testClass.getPackage.getName.replaceAll("\\.", "/") + "/" + testClass.getSimpleName + ".fixtures"

    lazy val data: FixtureData = {
        val stream = testClass.getResourceAsStream(path)
        try {
        	FixtureParser.parse(new InputStreamReader(stream, "UTF-8")).get
        } finally {
            stream.close
        }
    }

    def get(): T = get("default")
    
    def get(variant: String): T = {
        val fixtureVariant = data.variant(variant).getOrElse {
            throw new IllegalArgumentException(s"No valid variant found [name: ${variant}]")
        }

        val constructors = testClass.getConstructors()
        val instance = if (constructors.isEmpty) {
            throw new IllegalStateException(s"No constructors available to instantiate Fixture (${testClass.getSimpleName})")
        } else if  (constructors.length == 1 && constructors(0).getParameterTypes().length == 0) {
            // default constructor only
            testClass.newInstance()
        } else {
            // try every constructor out there
        	findValidConstructor(fixtureVariant, constructors)
        }
        
        populate(instance, fixtureVariant)
    }
    
    private def populate(instance: T, variant: FixtureVariant): T = {
        import scala.collection.JavaConverters._
        val propertiesMap = variant.properties.map( p => (p.key -> p.value) ).toMap.asJava
        BeanUtils.populate(instance, propertiesMap)
        instance
    }

    private def findValidConstructor(variant: FixtureVariant, constructors: Array[Constructor[_]]): T = {
        val values = variant.properties.map(_.value)
        
        var tempInstance: Option[T] = None
		constructors.foreach { c =>
		    if (tempInstance.isEmpty) {
		    	try {
		    		val constructorArguments = convertProperties(values, c.getParameterTypes())
		    		info(s"Converted constructor arguments: ${constructorArguments}")
    				tempInstance = Some(c.newInstance(constructorArguments.toSeq:_*).asInstanceOf[T])
		    	} catch {
		    		case NonFatal(e) => debug(s"Not valid constructor ${c} for model ${testClass}...", e)// ignore...we won't use this constructor
		    	}
		    }
        }
        
        tempInstance.getOrElse {
            throw new Exception(s"No valid constructor found for ${testClass}")
        }
    }
    
    private def convertProperties(props: List[Any], types: Array[Class[_]]): List[AnyRef] = {
        
    	val propsWithTypes = props.zip(types)
        val args = propsWithTypes.take(types.length)
        
        val newArgs = args map { 
            case (arg, tpe) if arg.getClass.isAssignableFrom(tpe) => arg.asInstanceOf[AnyRef]
            case (arg, tpe) => {
            	info(s"Converting property [value: ${arg}, to-type: ${tpe}]")
            	ConvertUtils.convert(arg, tpe).asInstanceOf[AnyRef]
            } 
        }
        
        newArgs
    }    
    

}
