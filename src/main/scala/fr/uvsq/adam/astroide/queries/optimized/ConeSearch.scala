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
package fr.uvsq.adam.astroide.queries.optimized

import fr.uvsq.adam.astroide.AstroideUDF.sphericalDistance
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._

/**
  * Extend the dataFrame API with a new operation ExecuteConeSearch
  *
  */

object ConeSearch {

  implicit class DataFrameCone(df: DataFrame) {

    def ExecuteConeSearch(healpixlevel: Int, column1: String, column2: String, ra: Double, dec: Double, radius: Double, boundariesFile: String): DataFrame = {

      var boundaries = Boundaries.ReadFromFile(boundariesFile)

      var coneSearchCells = ConeSearchCells.getCells(healpixlevel, ra, dec, radius).sorted

      var cone = ConeSearchCells.groupByRange(coneSearchCells)

      val intersection = for {
        b <- boundaries;
        c <- cone
        r = ConeSearchCells.intersectCone(b, c)
        if r != Nil
      } yield r

      val intersectiongroup = intersection.groupBy(w => w(0)).mapValues(l => l.map(x => Range.Long(x(1), x(2) + 1, 1)).flatten)

      val sphe_dist = udf((a: Double, d: Double) => sphericalDistance(a, d, ra, dec))

      val resultdf = df.where(col("nump").isin(intersectiongroup.keys.toSeq: _*)).where(col("ipix").isin(intersectiongroup.values.flatten.toSeq: _*)).filter(sphe_dist(col(column1), col(column2)) < radius)
      return resultdf

    }
  }

}


