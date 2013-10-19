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

import scala.reflect.ClassTag
import java.io.InputStreamReader
import scala.util.control.NonFatal
import java.lang.reflect.{ParameterizedType, Type, Constructor}
import grizzled.slf4j.Logging
import org.apache.commons.beanutils.PropertyUtils._
import org.apache.commons.beanutils.ConvertUtils._
import org.apache.commons.beanutils.BeanUtils._
import scala.collection.JavaConverters._
import org.apache.commons.beanutils.PropertyUtils

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
      val tmp = FixtureParser.parse(new InputStreamReader(stream, "UTF-8")).get
      info("Successfully parsed fixture: " + tmp)
      tmp
    } finally {
      stream.close()
    }
  }

  def get(): T = get("default")

  def get(variant: String): T = {

    val fixtureVariant = data.variant(variant).getOrElse {
      throw new IllegalArgumentException(s"No valid variant found [name: $variant]")
    }

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
            info(s"Converted constructor arguments: $constructorArguments")
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
    /*val fixtureType = optClass.orElse {
      getPropertyDescriptors(testClass).find(pd => pName.forall(_ == pd.getName)).map(_.getPropertyType)
    }.orElse(Some(expType)).getOrElse {
      throw new IllegalStateException(s"Cannot convert arguments for fixture [ref: $pName, type: $expType]")
    }*/

    val fixtureType = if (optClass.isDefined) optClass.get
    else {
      /*val propClass = getPropertyDescriptors(testClass).find(pd => pName.forall(_ == pd.getName)).map(_.getPropertyType)
      if (propClass.isDefined) propClass.get
      else */ expType
    }

    Fixture(fixtureType).get(variant).asInstanceOf[AnyRef] // FIXME: check variance to avoid casting?
  }

  implicit class PimpedType(aType: Type) {
    def toClass: Class[_] = resolveClass(aType)

    def isJavaCollection: Boolean = toClass.isAssignableFrom(classOf[java.util.Collection[_]])

    def isScalaCollection: Boolean = toClass.isAssignableFrom(classOf[scala.collection.immutable.List[_]])

    private def resolveClass(aType: Type): Class[_] = aType match {
      case cl: Class[_] => cl
      case pt: ParameterizedType => resolveClass(pt.getRawType)
      case x => throw new IllegalArgumentException(s"Cannot resolve class from ${x.getClass}")
    }
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
    case (FixtureProperty(_, List(FixtureLiteral(value))), expType) =>
      info(s"Converting property [value: $value, to-type: $expType]")
      convert(value, expType.toClass)
    case (FixtureProperty(pName, List(FixtureRef(variant, optClass))), expType) => {
      // dereference fixture class
      resolveFixtureRef(Some(pName), optClass, expType.toClass, variant)
    }
    case (FixtureProperty(pName, values), expType) if expType.isJavaCollection => {
      withCollection(values, expType) {
        actualValues =>
          import scala.collection.JavaConverters._
          expType match {
            case l: java.util.List[_] => new java.util.ArrayList(actualValues.asJavaCollection)
            case s: java.util.Set[_] => new java.util.HashSet(actualValues.asJavaCollection)
          }
      }
    }
    case (FixtureProperty(pName, values), expType) if expType.isScalaCollection => {
      withCollection(values, expType) {
        actualValues =>
          import scala.reflect._
          ClassTag(expType.toClass) match {
            case c if c <:< classTag[List[_]] => actualValues
            case c if c <:< classTag[Seq[_]] => actualValues.toSeq
            case c if c <:< classTag[Vector[_]] => actualValues.toVector
          }
      }
    }
  }

  def withCollection(values: List[FixtureValue], expType: Type)(block: List[AnyRef] => AnyRef): AnyRef = {
    val elemClass = expType match {
      case pt: ParameterizedType => pt.getActualTypeArguments()(0).toClass
      case cl: Class[_] => cl
    }

    val actualValues = values map {
      case FixtureLiteral(value) => convert(value, elemClass)
      case FixtureRef(variant, optClass) => resolveFixtureRef(None, optClass, elemClass, variant)
    }

    block(actualValues)

  }
}
