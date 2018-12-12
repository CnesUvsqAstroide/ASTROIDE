
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

import fr.uvsq.adam.astroide.queries.optimized.KNN._
import org.apache.spark.sql.SparkSession

/**
  * Execute a knn query using dataFrames
  *
  */
object RunKNN {

  val usage =

    """
      |Usage: RunKNN infile healpixlevel column1 column2 ra dec k boundariesfile
    """.stripMargin

  type OptionMap = Map[Symbol, Any]

  def parseArguments(map: OptionMap, arguments: List[String]): OptionMap = {
    arguments match {

      case Nil => map
      case infile :: healpixlevel :: column1 :: column2 :: ra :: dec :: k :: boundariesfile :: Nil =>
        map ++ Map('infile -> infile) ++ Map('healpixlevel -> healpixlevel.toInt) ++ Map('column1 -> column1) ++ Map('column2 -> column2) ++ Map('ra -> ra.toDouble) ++ Map('dec -> dec.toDouble) ++ Map('k -> k.toInt) ++ Map('boundariesfile -> boundariesfile)
      case option :: tail =>
        println(usage)
        throw new IllegalArgumentException(s"Unknown argument $option");
    }

  }

  def main(args: Array[String]) {

    val configuration = parseArguments(Map(), args.toList)
    println(configuration)

    val inputFile = configuration('infile).toString
    val healpixlevel = configuration('healpixlevel).asInstanceOf[Int]
    val column1 = configuration('column1).toString
    val column2 = configuration('column2).toString
    val ra = configuration('ra).asInstanceOf[Double]
    val dec = configuration('dec).asInstanceOf[Double]
    val k = configuration('k).asInstanceOf[Int]
    val boundariesFile = configuration('boundariesfile).toString

    val spark = SparkSession.builder().appName("astroide").getOrCreate()
    import spark.implicits._

    val df = spark.read.parquet(inputFile)

    val resultdf = df.ExecuteKNN(healpixlevel, column1, column2, ra, dec, k, boundariesFile)

    resultdf.show()

  }
}


