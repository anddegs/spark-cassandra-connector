package com.datastax.spark.connector.mapper

import java.lang.reflect.Method

import com.datastax.spark.connector.cql.TableDef

import scala.reflect.ClassTag

class JavaBeanColumnMapper[T : ClassTag](columnNameOverride: Map[String, String] = Map.empty) extends ReflectionColumnMapper[T] {

  import com.datastax.spark.connector.mapper.JavaBeanColumnMapper._

  override def classTag: ClassTag[T] = implicitly[ClassTag[T]]

  private def propertyName(accessorName: String) = {
    val AccessorRegex(_, strippedName) = accessorName
    strippedName(0).toLower + strippedName.substring(1)
  }

  override protected def isGetter(method: Method): Boolean =
    GetterRegex.findFirstMatchIn(method.getName).isDefined &&
    method.getParameterTypes.size == 0 &&
    method.getReturnType != Void.TYPE

  override protected def isSetter(method: Method): Boolean =
    SetterRegex.findFirstMatchIn(method.getName).isDefined &&
    method.getParameterTypes.size == 1 &&
    method.getReturnType == Void.TYPE

  def resolve(name: String, tableDef: TableDef, aliasToColumnName: Map[String, String]): String = {
    columnNameOverride orElse aliasToColumnName applyOrElse(name, columnNameForProperty(_: String, tableDef))
  }

  override protected def getterToColumnName(getterName: String, tableDef: TableDef, aliasToColumnName: Map[String, String]) = {
    val p = propertyName(getterName)
    resolve(p, tableDef, aliasToColumnName)
  }

  override protected def setterToColumnName(setterName: String, tableDef: TableDef, aliasToColumnName: Map[String, String]) = {
    val p = propertyName(setterName)
    resolve(p, tableDef, aliasToColumnName)
  }

  override protected def constructorParamToColumnName(paramName: String, tableDef: TableDef, aliasToColumnName: Map[String, String]) = {
    resolve(paramName, tableDef, aliasToColumnName)
  }

  /** Java Beans allow nulls in property values */
  override protected def allowsNull = true
}

object JavaBeanColumnMapper {
  val GetterRegex = "^(get|is)(.+)$".r
  val SetterRegex = "^(set)(.+)$".r
  val AccessorRegex = "^(get|is|set)(.+)$".r
}