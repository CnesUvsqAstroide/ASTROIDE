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

import scala.io.Source
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.encoders.RowEncoder

object CrossMatchBase {

  val usage =

    """
      |Usage: CrossMatch infile1 infile2 healpixlevel radius
    """.stripMargin

  type OptionMap = Map[Symbol, Any]

  def parseArguments(map: OptionMap, arguments: List[String]): OptionMap = {
    arguments match {

      case Nil => map
      case infile1 :: infile2 :: healpixlevel :: radius :: Nil =>
        map ++ Map('infile1 -> infile1) ++ Map('infile2 -> infile2) ++ Map('healpixlevel -> healpixlevel.toInt) ++ Map('radius -> radius.toDouble)
      case option :: tail =>
        println(usage)
        throw new IllegalArgumentException(s"Unknown argument $option");
    }

  }

  def main(args: Array[String]) {

    val configuration = parseArguments(Map(), args.toList)
    println(configuration)

    val inputFile1 = configuration('infile1).toString
    val inputFile2 = configuration('infile2).toString
    val healpixlevel = configuration('healpixlevel).asInstanceOf[Int]
    val radius = configuration('radius).asInstanceOf[Double]

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val inputData1 = spark.read.parquet(inputFile1)
    val inputData2 = spark.read.parquet(inputFile2)

    inputData1.first()
    inputData2.first()

    def udfToHealpix = udf((alpha: Double, delta: Double) => {

      val theta = math.Pi / 2 - delta.toRadians
      val phi = alpha.toRadians

      HealpixProc.ang2pixNest(healpixlevel, new Pointing(theta, phi))
    })

    val dataWithHealpix1 = inputData1.withColumn("ipix", udfToHealpix($"alpha", $"delta"))
    val dataWithHealpix2 = inputData2.withColumn("ipix", udfToHealpix($"alpha", $"delta"))

    def udfNeighbours = udf((ipix: Long) => {

      HealpixProc.neighboursNest(healpixlevel, ipix)
    })

    val dataWithNeighbours2 = dataWithHealpix2.withColumn("neighbours", udfNeighbours($"ipix"))

    val results = dataWithHealpix1.join(dataWithNeighbours2, dataWithNeighbours2.col("neighbours").cast("string").contains(dataWithHealpix1.col("ipix")))
    results.show()

  }

}

