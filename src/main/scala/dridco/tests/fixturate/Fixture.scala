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

package dridco.tests.fixturate

import _root_.java.io.InputStreamReader
import _root_.java.lang.reflect.{Type, ParameterizedType, Constructor}
import scala.reflect._
import scala.util.control.NonFatal
import grizzled.slf4j.Logging
import org.apache.commons.beanutils.PropertyUtils._
import org.apache.commons.beanutils.ConvertUtils._
import org.apache.commons.beanutils.BeanUtils._
import org.apache.commons.beanutils.PropertyUtils
import scala.collection.JavaConverters._
import scala.collection.immutable.Stack

object Fixture {
  def apply[T](testClass: Class[T]): Fixture[T] = new Fixture[T](testClass)

  def apply[T](implicit manifest: ClassTag[T]): Fixture[T] =
    new Fixture[T](manifest.runtimeClass.asInstanceOf[Class[T]])
}

class Fixture[T](testClass: Class[T]) extends Logging {
  val path = "/" + testClass.getPackage.getName.replaceAll("\\.", "/") + "/" + testClass.getSimpleName + ".fixtures"

  lazy val data: FixtureData = {
    val stream = testClass.getResourceAsStream(path).ensuring(_ != null, s"Fixture cannot be found at $path")
    try {
      val fixData = FixtureParser.parse(new InputStreamReader(stream, "UTF-8")).get
      debug("Successfully parsed fixture: " + fixData)
      fixData
    } finally {
      stream.close()
    }
  }

  def get(): T = get("default")

  def get(variant: String): T = {
    val fixtureVariant = data.variant(variant).getOrElse {
      throw new IllegalArgumentException(s"No valid variant found [name: $variant]")
    }

    getFixture(fixtureVariant)
  }

  def all(): List[T] =  data.variants.map(getFixture(_))

  def any(): T = {
    import scala.util.Random._
    getFixture(data.variants(nextInt(data.variants.length)))
  }

  def any(n: Int): List[T] = all().take(n).sortBy( t => scala.util.Random.nextBoolean)

  private def getFixture(fixtureVariant: FixtureVariant): T = {
    val constructors = testClass.getConstructors
    val instance = if (constructors.isEmpty) {
      throw new IllegalStateException(s"No constructors available to instantiate Fixture (${testClass.getSimpleName})")
    } else if (constructors.length == 1 && constructors(0).getParameterTypes.isEmpty) {
      // default constructor only
      testClass.newInstance()
    } else {
      // try every constructor out there
      findValidConstructor(fixtureVariant, constructors)
    }

    populateProperties(instance, fixtureVariant)
  }

  private def populateProperties(instance: T, variant: FixtureVariant): T = {

    // retrieve setters only (hopefully none)
    val propAndTypes = variant.properties.map {
      case fp@FixtureProperty(key, _) => (fp, getPropertyDescriptor(instance, key))
    } filter {
      case (_, pd) => Option(PropertyUtils.getWriteMethod(pd)).isDefined
    } map {
      case (fp, pd) => (fp, pd.getPropertyType.asInstanceOf[Class[AnyRef]])
    }

    if (!propAndTypes.isEmpty) {
      val (props, types) = propAndTypes.unzip
      val propValues = convertProperties(props, types.toArray)
      val propertiesMap = props.map(_.key).zip(propValues).toMap.asJava
      populate(instance, propertiesMap)
    }

    instance
  }

  private def findValidConstructor(variant: FixtureVariant, constructors: Array[Constructor[_]]): T = {

    var tempInstance: Option[T] = None
    constructors.foreach {
      c =>
        if (tempInstance.isEmpty) {
          try {
            val constructorTypes = c.getGenericParameterTypes
            val constructorArguments = convertProperties(variant.properties, constructorTypes)
            debug(s"Converted constructor arguments: $constructorArguments")
            tempInstance = Some(c.newInstance(constructorArguments.toSeq: _*).asInstanceOf[T])
          } catch {
            case NonFatal(e) =>
              // ignore...we won't use this constructor
              debug(s"Not valid constructor $c for model $testClass...", e)
          }
        }
    }

    tempInstance.getOrElse {
      throw new Exception(s"No valid constructor found for $testClass")
    }
  }

  def resolveFixtureRef(pName: Option[String], optClass: Option[Class[_]], expType: Class[_], variant: String): AnyRef = {
    val fixtureType = optClass.getOrElse(expType)
    Fixture(fixtureType).get(variant).asInstanceOf[AnyRef] // FIXME: check variance to avoid casting?
  }

  private def convertProperties(props: List[FixtureProperty], types: Array[Type]): List[AnyRef] = {
    val args = props.take(types.length)
    val argsValues = args.zip(types).map {
      propAndType =>
        convertProperty(propAndType)
    }

    argsValues
  }

  private def convertProperty(propAndType: (FixtureProperty, Type)): AnyRef = propAndType match {
    case (FixtureProperty(pName, values), expType) if expType.isJavaCollection => {
      // java collection
      withCollection(values, expType) {
        actualValues =>
          ClassTag(expType.toClass) match {
            case c if c <:< classTag[_root_.java.util.List[_]] => new _root_.java.util.ArrayList(actualValues.asJavaCollection)
            case c if c <:< classTag[_root_.java.util.Set[_]] => new _root_.java.util.HashSet(actualValues.asJavaCollection)
          }
      }
    }
    case (FixtureProperty(pName, values), expType) if expType.isScalaCollection => {
      // scala collection
      withCollection(values, expType) {
        actualValues =>
          ClassTag(expType.toClass) match {
            case c if c <:< classTag[List[_]] => actualValues
            case c if c <:< classTag[Seq[_]] => actualValues.toSeq
            case c if c <:< classTag[Vector[_]] => actualValues.toVector
          }
      }
    }
    case (FixtureProperty(_, List(FixtureLiteral(value))), expType) =>
      // fixture literal
      debug(s"Converting property [value: $value, to-type: $expType]")
      convert(value, expType.toClass)
    case (FixtureProperty(pName, List(FixtureRef(variant, optClass))), expType) => {
      // fixture ref
      resolveFixtureRef(Some(pName), optClass, expType.toClass, variant)
    }
    case (FixtureProperty(pName, List(FixtureEnum(value, optClass))), expType) => {
      // fixture enum
      resolveEnum(value, expType)
    }
  }

  private def resolveEnum(value: String, expType: Type): AnyRef = {
    val values = expType.toClass.getEnumConstants
    values.find(v => v.toString == value).getOrElse {
      throw new Exception(s"Cannot map enum with value [$value] to enum type [$expType.toClass]")
    }.asInstanceOf[AnyRef]
  }

  private def withCollection(values: List[FixtureValue], expType: Type)(block: List[AnyRef] => AnyRef): AnyRef = {
    val elemClass = expType match {
      case pt: ParameterizedType => pt.getActualTypeArguments()(0).toClass
      case cl: Class[_] => cl
    }

    val actualValues = values map {
      case FixtureLiteral(value) => convert(value, elemClass)
      case FixtureRef(variant, optClass) => resolveFixtureRef(None, optClass, elemClass, variant)
      case FixtureEnum(value, optClass) => resolveEnum(value, elemClass)
    }

    block(actualValues)

  }

  implicit class PimpedType(aType: Type) {
    def toClass: Class[_] = resolveClass(aType)

    def isJavaCollection: Boolean = classOf[_root_.java.util.Collection[_]].isAssignableFrom(aType.toClass)

    def isScalaCollection: Boolean = classOf[scala
    .collection.immutable.Seq[_]].isAssignableFrom(aType.toClass)

    private def resolveClass(aType: Type): Class[_] = aType match {
      case cl: Class[_] => cl
      case pt: ParameterizedType => resolveClass(pt.getRawType)
      case x => throw new IllegalArgumentException(s"Cannot resolve class from ${x.getClass}")
    }
  }

}
