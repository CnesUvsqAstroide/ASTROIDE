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
import adql.query.operand.function.geometry.DistanceFunction
import adql.query.operand.function.geometry.PointFunction
import adql.query.operand.function.DefaultUDF

class TranslatorDistance extends SimpleReplaceHandler(true, false) {


  /**
    * Identify distance function and get spherical coordinates
    *
    */

  override def `match`(obj: ADQLObject): Boolean = {
    try {
      val distance = obj.asInstanceOf[DistanceFunction]

      val point1 = distance.getP1.getValue.asInstanceOf[PointFunction]
      val point2 = distance.getP2.getValue.asInstanceOf[PointFunction]

      val coordinates11 = point1.getCoord1
      val coordinates12 = point1.getCoord2

      val coordinates21 = point2.getCoord1
      val coordinates22 = point2.getCoord2

      if (coordinates11.isString() && coordinates21.isNumeric())
        return coordinates11.getName.equalsIgnoreCase("ra") && coordinates12.getName.equalsIgnoreCase("dec")

      else coordinates21.getName.equalsIgnoreCase("ra") && coordinates22.getName.equalsIgnoreCase("dec")

    }

    catch {
      case cce: ClassCastException ⇒ false
    }
  }

  /**
    * Transform distance function into a UDF called SphericalDistance
    *
    */

  override def getReplacer(obj: ADQLObject): ADQLObject = {
    try {

      val distance = obj.asInstanceOf[DistanceFunction]

      val point1 = distance.getP1.getValue.asInstanceOf[PointFunction]
      val point2 = distance.getP2.getValue.asInstanceOf[PointFunction]

      val coordinates11 = point1.getCoord1
      val coordinates12 = point1.getCoord2

      val coordinates21 = point2.getCoord1
      val coordinates22 = point2.getCoord2

      return new DefaultUDF("SphericalDistance", Array(coordinates11, coordinates12, coordinates21, coordinates22))

    }

    catch {
      case cce: ClassCastException ⇒ return null
    }
  }
}
    




