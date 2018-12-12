
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

import scala.io.Source
import fr.uvsq.adam.astroide.queries.basic.KNNSQL
import org.apache.spark.sql.SparkSession
import fr.uvsq.adam.astroide.queries.optimized.Boundaries
import healpix.essentials._

/**
  * Execute a knn query using baseline spark sql
  *
  */

object RunKNNSQL {

  val usage =

    """
      |Usage: RunKNNSQL infile alpha delta k
    """.stripMargin

  type OptionMap = Map[Symbol, Any]

  def parseArguments(map: OptionMap, arguments: List[String]): OptionMap = {
    arguments match {

      case Nil => map
      case infile :: ra :: dec :: k :: Nil =>
        map ++ Map('infile -> infile) ++ Map('ra -> ra.toDouble) ++ Map('dec -> dec.toDouble) ++ Map('k -> k.toInt)
      case option :: tail =>
        println(usage)
        throw new IllegalArgumentException(s"Unknown argument $option");
    }

  }

  def main(args: Array[String]) {

    val configuration = parseArguments(Map(), args.toList)
    println(configuration)

    val inputFile = configuration('infile).toString
    val alpha = configuration('alpha).asInstanceOf[Double]
    val delta = configuration('delta).asInstanceOf[Double]
    val k = configuration('k).asInstanceOf[Int]

    val spark = SparkSession.builder().appName("astroide").getOrCreate()
    import spark.implicits._

    val df = spark.read.parquet(inputFile)

    val resultdf = KNNSQL.Execute(df, alpha, delta, k)

    resultdf.show()

  }
}


