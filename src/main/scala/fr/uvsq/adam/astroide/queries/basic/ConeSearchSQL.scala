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

import healpix.essentials._
import fr.uvsq.adam.astroide.partitioner.PartitionerUtil
import fr.uvsq.adam.astroide.AstroideUDF.sphericalDistance
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.encoders.RowEncoder
import java.io._
import org.apache.spark.sql._

object ConeSearchSQL {

  val usage =

    """
      |Usage: ConeSearcSQL [-fs hdfs://...] infile.parquet outfile.parquet alpha delta radius
    """.stripMargin

  type OptionMap = Map[Symbol, Any]

  def parseArguments(map: OptionMap, arguments: List[String]): OptionMap = {
    arguments match {

      case Nil => map
      case "-fs" :: hdfs :: tail =>
        parseArguments(map ++ Map('hdfs -> hdfs), tail)
      case infile :: outfile :: alpha :: delta :: radius :: Nil =>
        map ++ Map('infile -> infile) ++ Map('outfile -> outfile) ++ Map('alpha -> alpha.toDouble) ++ Map('delta -> delta.toDouble) ++ Map('radius -> radius.toDouble)
      case option :: tail =>
        println(usage)
        throw new IllegalArgumentException(s"Unknown argument $option");
    }

  }

  def main(args: Array[String]) {

    val configuration = parseArguments(Map(), args.toList)
    println(configuration)

    val t0 = System.currentTimeMillis()

    val hdfs = configuration('hdfs).toString
    val inputFile = configuration('infile).toString
    val outputFile = configuration('outfile).toString
    val alpha = configuration('alpha).asInstanceOf[Double]
    val delta = configuration('delta).asInstanceOf[Double]
    val radius = configuration('radius).asInstanceOf[Double]

    val spark = SparkSession.builder().getOrCreate()

    import spark.implicits._

    println("size of input file:" + PartitionerUtil.parquetSize(inputFile) + "MB")
    val inputData = spark.read.parquet(inputFile)

    val sphe_dist = udf((a: Double, d: Double) => sphericalDistance(a, d, alpha, delta))

    var results = inputData.filter(sphe_dist($"alpha", $"delta") < radius).show()

  }

}

