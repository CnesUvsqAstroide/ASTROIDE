
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

package fr.uvsq.adam.astroide

import healpix.essentials._
import org.apache.spark.sql.SparkSession

/**
  * Register SphericalDistance and Neighbours UDFs
  *
  */

object AstroideUDF {

  def sphericalDistance(RA1: Double, DEC1: Double, RA2: Double, DEC2: Double): Double = {

    val RA1_rad = RA1.toRadians
    val RA2_rad = RA2.toRadians
    val DEC1_rad = DEC1.toRadians
    val DEC2_rad = DEC2.toRadians
    var d = scala.math.pow(math.sin((DEC1_rad - DEC2_rad) / 2), 2)
    d += scala.math.pow(math.sin((RA1_rad - RA2_rad) / 2), 2) * math.cos(DEC1_rad) * math.cos(DEC2_rad)

    return (2 * math.asin(math.sqrt(d)).toDegrees)

  }

  def RegisterUDF(spark: SparkSession, order: Int) = {
    spark.udf.register("SphericalDistance", (x: Double, y: Double, x2: Double, y2: Double) => sphericalDistance(x, y, x2, y2))
    spark.udf.register("Neighbours", (ipix: Long) => ipix +: HealpixProc.neighboursNest(order, ipix))
  }

  def RegisterUDFWithRules(spark: SparkSession) = {
    spark.udf.register("SphericalDistance", (x: Double, y: Double, x2: Double, y2: Double) => sphericalDistance(x, y, x2, y2))
  }

  //substring les parametres de la requetes :
  def getParameters(condition: String) = {

    condition.substring(condition.toString().indexOf("SphericalDistance")).split("""[(|,<|)]""").filterNot(s => s.isEmpty || s.trim.isEmpty).filterNot(s => s.contains("cast"))
    //condition.substring(condition.toString().indexOf("SphericalDistance")).split("""[(|,<|)]""").filterNot(s => s.isEmpty || s.trim.isEmpty)

  }


}
