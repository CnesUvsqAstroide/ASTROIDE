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

import adql.query._
import adql.query.constraint.Comparison
import adql.query.from.{ADQLJoin, InnerJoin}
import adql.search.SimpleReplaceHandler

/**
  * Identify join function using a sphericalDistance
  *
  */

class TranslatorJoin extends SimpleReplaceHandler(true, false) {


  override def `match`(obj: ADQLObject): Boolean = {
    try {
      val join = obj.asInstanceOf[InnerJoin].getJoinCondition.get(0).asInstanceOf[Comparison]

      return (join.getLeftOperand().getName.contains("SphericalDistance"))

    }

    catch {
      case cce: ClassCastException ⇒ false
    }
  }

  /**
    * Replace join function using a sphericalDistance with an equi-join
    *
    */

  override def getReplacer(obj: ADQLObject): ADQLObject = {
    try {

      val join = obj.asInstanceOf[ADQLJoin]

      val leftTable = join.getLeftTable
      val rightTable = join.getRightTable

      return new InnerJoin(leftTable, rightTable, join.getJoinCondition)
    }

    catch {
      case cce: ClassCastException ⇒ return null
    }
  }
}

