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
import org.apache.spark.sql.SparkSession
import healpix.essentials._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.row_number


object KNNJoin {

	def duplicate(ipix: Long): Array[Long] = {
		return HealpixProc.neighboursNest(12, ipix).filter(_ != -1).distinct

	}

	def duplicate2(array: Array[Long]): Array[Long] = {
		return array.flatMap(x => HealpixProc.neighboursNest(12, x)).filter(_ != -1).distinct
	}

	def repeteDuplication(n: Int, r: Array[Long]): Array[Long] = {
		return (1 to n).foldLeft(r)((rx, _) => duplicate2(rx))
	}


	implicit class DataFrameJoin(inputData1: DataFrame) {

		def ExecuteKNNJoin(spark: SparkSession, inputData2: DataFrame, healpixlevel: Int, k: Int): DataFrame = {

			import spark.implicits._


			val hist = inputData2.select("TYC1_2", "ipix_2").groupBy("ipix_2")
			val histCount = hist.count()
			var histSorted = histCount.sort(asc("count"))

			val verifyK = histSorted.filter("count < k ").count() > 0

			if (verifyK) histSorted.withColumn("degreeDuplication", lit(0)) else histSorted

			val NeighboursToJoin = udf((a: Long) => a +: repeteDuplication(2, duplicate(a)))

			val histWithNeighbours = histSorted.withColumn("neighbours", NeighboursToJoin($"ipix_2"))

			val dataWithNeighbours = histWithNeighbours.join(inputData2, inputData2.col("ipix_2") === histWithNeighbours("ipix_2"))


			val explodedData2 = dataWithNeighbours.withColumn("ipix_neigh", explode($"neighbours"))

			val joined = inputData1.join(explodedData2, explodedData2.col("ipix_neigh") === inputData1.col("ipix"))

			val sphe_dist = udf((a: Double, d: Double, a2: Double, d2: Double) => sphericalDistance(a, d, a2, d2))
			val matched = joined.withColumn("dist", sphe_dist($"ra", $"dec", $"ra_2", $"dec_2"))

			val result = matched.withColumn("rank", row_number().over(Window.partitionBy($"source_id").orderBy($"dist".asc))).filter($"rank" <= k)

			return result

		}
	}

}

