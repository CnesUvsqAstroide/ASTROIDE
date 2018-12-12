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
package fr.uvsq.adam.astroide.queries.basic

import fr.uvsq.adam.astroide.AstroideUDF.sphericalDistance
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.functions._

object KNNSQL {

  def Execute(df: DataFrame, ra: Double, dec: Double, k: Int): DataFrame = {

    val sphe_dist = udf((a: Double, d: Double) => sphericalDistance(a, d, ra, dec))

    val resultdf = df.withColumn("distance", sphe_dist(col("ra"), col("dec"))).orderBy(col("distance")).limit(k)

    return resultdf

  }
}


