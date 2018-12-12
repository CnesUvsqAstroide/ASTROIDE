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
import org.apache.spark.sql.functions._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.{StructField, StructType, StringType, DoubleType}

object CrossMatchBaseSQL {

  val usage =

    """
      |Usage: CrossMatch infile1 infile2 radius
    """.stripMargin

  type OptionMap = Map[Symbol, Any]

  def parseArguments(map: OptionMap, arguments: List[String]): OptionMap = {
    arguments match {

      case Nil => map
      case infile1 :: infile2 :: radius :: Nil =>
        map ++ Map('infile1 -> infile1) ++ Map('infile2 -> infile2) ++ Map('radius -> radius.toDouble)
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

    val radius = configuration('radius).asInstanceOf[Double]

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val tycho_schema = StructType(Array(StructField("TYC1", StringType, true), StructField("pflag", StringType, true), StructField("ramdeg", StringType, true), StructField("demdeg", StringType, true), StructField("pmra", StringType, true), StructField("pmde", StringType, true), StructField("e_ramdeg", StringType, true), StructField("e_demdeg", StringType, true), StructField("e_pmra", StringType, true), StructField("e_pmde", StringType, true), StructField("EpRAm", StringType, true), StructField("EpDEm", StringType, true), StructField("Num", StringType, true), StructField("q_RAmdeg", StringType, true), StructField("q_DEmdeg", StringType, true), StructField("q_pmRA", StringType, true), StructField("q_pmDE", StringType, true), StructField("BTmag", StringType, true), StructField("e_BTmag", StringType, true), StructField("VTmag", StringType, true), StructField("e_VTmag", StringType, true), StructField("prox", StringType, true), StructField("HIP", StringType, true), StructField("CCDM", StringType, true), StructField("ra", DoubleType, true), StructField("dec", DoubleType, true), StructField("EpRA-1990", StringType, true), StructField("EpDE-1990", StringType, true), StructField("e_RAdeg", StringType, true), StructField("e_DEdeg", StringType, true), StructField("posflg", StringType, true), StructField("corr", StringType, true)))

    val inputData1 = spark.read.option("header", "true").csv(inputFile1)

    var inputData2 = spark.read.option("delimiter", "|").option("header", "true").schema(tycho_schema).csv(inputFile2)

    val columnname = inputData2.columns
    val newnames = columnname.map(x => x + "_2")
    for (i <- newnames.indices) {
      inputData2 = inputData2.withColumnRenamed(columnname(i), newnames(i))
    }

    val sphe_dist = udf((a: Double, d: Double, a2: Double, d2: Double) => sphericalDistance(a, d, a2, d2))
    val results = inputData1.join(inputData2, sphe_dist($"ra", $"dec", $"ra_2", $"dec_2") < radius)

    println(results.count())

  }

}

