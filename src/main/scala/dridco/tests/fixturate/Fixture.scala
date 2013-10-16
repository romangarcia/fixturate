package dridco.fixturate

import scala.reflect.ClassTag
import scala.collection.JavaConverters._
import dridco.tests.fixturate.FixtureData
import dridco.tests.fixturate.FixtureParser
import java.io.InputStreamReader
import scala.util.control.NonFatal
import java.lang.reflect.Constructor
import dridco.tests.fixturate.FixtureVariant
import grizzled.slf4j.Logging
import scala.collection.JavaConverters
import dridco.tests.fixturate.FixtureLiteral
import dridco.tests.fixturate.FixtureRef
import dridco.tests.fixturate.FixtureValue
import dridco.tests.fixturate.FixtureProperty
import java.beans.ReflectionUtils

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
        	val tmp = FixtureParser.parse(new InputStreamReader(stream, "UTF-8")).get
        	info("Succesfully parsed fixture: " + tmp)
        	tmp
        } finally {
            stream.close
        }
    }

    def get(): T = get("default")
    
    def get(variant: String): T = {

    	val fixtureVariant = data.variant(variant).getOrElse {
            throw new IllegalArgumentException(s"No valid variant found [name: ${variant}]")
        }

        val p = fixtureVariant.properties
        
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
        
        populateProperties(instance, fixtureVariant)
    }
    
    private def populateProperties(instance: T, variant: FixtureVariant): T = {
        import scala.collection.JavaConverters._
        import org.apache.commons.beanutils.BeanUtils._
        val propertiesMap = variant.properties.map( p => (p.key -> p.value) ).toMap.asJava
        populate(instance, propertiesMap)
        instance
    }

    private def findValidConstructor(variant: FixtureVariant, constructors: Array[Constructor[_]]): T = {
        
        var tempInstance: Option[T] = None
		constructors.foreach { c =>
		    if (tempInstance.isEmpty) {
		    	try {
		    		val constructorArguments = convertProperties(variant.properties, c.getParameterTypes())
		    		info(s"Converted constructor arguments: ${constructorArguments}")
    				tempInstance = Some(c.newInstance(constructorArguments.toSeq:_*).asInstanceOf[T])
		    	} catch {
		    		case NonFatal(e) =>
		    		    // ignore...we won't use this constructor
		    		    debug(s"Not valid constructor ${c} for model ${testClass}...", e)
		    	}
		    }
        }
        
        tempInstance.getOrElse {
            throw new Exception(s"No valid constructor found for ${testClass}")
        }
    }
    
    private def convertProperties(props: List[FixtureProperty], types: Array[Class[_]]): List[AnyRef] = {
    	import org.apache.commons.beanutils.ConvertUtils._
    	import org.apache.commons.beanutils.PropertyUtils._
        
        val args = props.take(types.length)

        val argsValues = args.zip(types).map {
        	case (FixtureProperty(_, FixtureLiteral(value)), expType) => value
        	case (FixtureProperty(pName, FixtureRef(variant, optClass)), expType) => {
        	    // dereference fixture class
        	    val fixtureType = optClass.orElse {
        	        getPropertyDescriptors(testClass).find(_.getName == pName).map(_.getPropertyType)
        	    }.orElse(Some(expType)).getOrElse {
        	        throw new IllegalStateException(s"Cannot convert arguments for fixture [ref: ${pName}, type: ${expType}]")
        	    }
        	    
        	    Fixture(fixtureType).get(variant)
        	}
    	}
        
        val newArgs = argsValues.zip(types) map { 
            case (arg, tpe) if arg.getClass.isAssignableFrom(tpe) => arg.asInstanceOf[AnyRef]
            case (arg, tpe) => {
            	info(s"Converting property [value: ${arg}, to-type: ${tpe}]")
            	convert(arg, tpe).asInstanceOf[AnyRef]
            } 
        }
        
        newArgs
    }    
    

}
