/*
 * Copyright (C) 2018  by Mariem Brahem
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package fr.uvsq.adam.astroide.optimizer

import adql.query.ADQLObject
import adql.search.SimpleReplaceHandler
import adql.query.constraint.Comparison
import adql.query.operand.function.geometry.PointFunction
import adql.query.operand.function.geometry.ContainsFunction
import adql.query.operand.function.geometry.CircleFunction
import adql.query.operand.function.DefaultUDF
import adql.query.constraint.ComparisonOperator

/**
  * Identify comparison ADQL clauses
  *
  */

class TranslatorContains extends SimpleReplaceHandler(true, false) {


  override def `match`(obj: ADQLObject): Boolean = {
    try {
      val comp = obj.asInstanceOf[Comparison]

      return (comp.getLeftOperand().isInstanceOf[ContainsFunction] || comp.getRightOperand().isInstanceOf[ContainsFunction])

    }

    catch {
      case cce: ClassCastException ⇒ false
    }
  }


  /**
    * Transform comparison ADQL clauses associated to a cone search condition
    *
    */

  override def getReplacer(obj: ADQLObject): ADQLObject = {
    try {

      val comp = obj.asInstanceOf[Comparison]

      val operand = if ((comp.getLeftOperand().isInstanceOf[ContainsFunction]) && (comp.getOperator() == ComparisonOperator.EQUAL) && comp.getRightOperand().isNumeric()) {
        comp.getLeftOperand()

      }
      else if ((comp.getRightOperand().isInstanceOf[ContainsFunction]) && (comp.getOperator() == ComparisonOperator.EQUAL) && comp.getLeftOperand().isNumeric()) {

        comp.getRightOperand()

      }
      val contains = operand.asInstanceOf[ContainsFunction]

      val point = contains.getLeftParam.getValue.asInstanceOf[PointFunction]
      val circle = contains.getRightParam.getValue.asInstanceOf[CircleFunction]

      val leftOp = new DefaultUDF("SphericalDistance", Array(point.getCoord1, point.getCoord2, circle.getCoord1, circle.getCoord2))

      return new Comparison(leftOp, ComparisonOperator.LESS_THAN, circle.getRadius)
    }

    catch {
      case cce: ClassCastException ⇒ return null
    }
  }
}
