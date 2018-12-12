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
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import org.apache.spark.sql.{Encoders, Encoder}
import healpix.essentials._

object CrossMatchIntermediateTable {

  implicit class DataFrameCross(inputData1: DataFrame) {

    def ExecuteXMatch(spark: SparkSession, inputData2: DataFrame, radius: Double): DataFrame = {

      import spark.implicits._

      def udfNeighbours = udf((ipix: Long) => {

        ipix +: HealpixProc.neighboursNest(12, ipix)

      })

      val explodedData2 = inputData2.withColumn("ipix_2", explode(udfNeighbours($"ipix_2")))

      val joined = inputData1.join(explodedData2, explodedData2.col("ipix_2") === inputData1.col("ipix"))

      val sphe_dist = udf((a: Double, d: Double, a2: Double, d2: Double) => sphericalDistance(a, d, a2, d2))
      val result = joined.filter(sphe_dist($"ra", $"dec", $"ra_2", $"dec_2") < radius)

      implicit var encoder = RowEncoder(result.schema)

      implicit def tuple2[A1, A2](
                                   implicit e1: Encoder[A1],
                                   e2: Encoder[A2]): Encoder[(A1, A2)] = Encoders.tuple[A1, A2](e1, e2)

      result.select(col("sourceid"), col("sourceid_2")).withColumn("distance", sphe_dist($"ra", $"dec", $"ra_2", $"dec_2"))

    }
  }

}






