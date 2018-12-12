
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

package fr.uvsq.adam.astroide.executor

import fr.uvsq.adam.astroide.queries.optimized.CrossMatch._
import org.apache.spark.sql.SparkSession

/**
  * Execute a cross-matching query using dataFrames
  *
  */

object RunCrossMatch {

  val usage =

    """
      |Usage: RunCrossMatch infile infile2 radius healpixlevel
    """.stripMargin

  type OptionMap = Map[Symbol, Any]

  def parseArguments(map: OptionMap, arguments: List[String]): OptionMap = {
    arguments match {

      case Nil => map
      case infile :: infile2 :: radius :: healpixlevel :: Nil =>
        map ++ Map('infile -> infile) ++ Map('infile2 -> infile2) ++ Map('radius -> radius.toDouble) ++ Map('healpixlevel -> healpixlevel.toInt)
      case option :: tail =>
        println(usage)
        throw new IllegalArgumentException(s"Unknown argument $option");
    }

  }

  def main(args: Array[String]) {

    val configuration = parseArguments(Map(), args.toList)
    println(configuration)

    val inputFile_1 = configuration('infile).toString
    val inputFile_2 = configuration('infile2).toString
    val radius_cross = configuration('radius).asInstanceOf[Double]
    val healpixlevel = configuration('healpixlevel).asInstanceOf[Int]

    val spark = SparkSession.builder().appName("astroide").getOrCreate()
    import spark.implicits._

    val inputData_1 = spark.read.parquet(inputFile_1)

    var inputData_2 = spark.read.parquet(inputFile_2)

    val columnname = inputData_2.columns
    val newnames = columnname.map(x => x + "_2")

    for (i <- newnames.indices) {
      inputData_2 = inputData_2.withColumnRenamed(columnname(i), newnames(i))
    }

    val output = inputData_1.ExecuteXMatch(spark, inputData_2, radius_cross, healpixlevel)

    println(output.count())


    spark.stop()
  }

}

