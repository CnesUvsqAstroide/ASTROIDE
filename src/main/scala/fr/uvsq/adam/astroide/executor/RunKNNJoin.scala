
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

import fr.uvsq.adam.astroide.AstroideUDF.sphericalDistance
import healpix.essentials.{HealpixProc, Pointing}
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

import scala.collection.Map

/**
  * Execute a knn Join query using dataFrames
  *
  */

object RunKNNJoin {

  val usage =

    """
      |Usage: RunKNNJoin infile infile2 healpixlevel k x y x2 y2 id1 id2
    """.stripMargin

  type OptionMap = Map[Symbol, Any]

  def parseArguments(map: OptionMap, arguments: List[String]): OptionMap = {
    arguments match {

      case Nil => map
      case infile :: infile2 :: healpixlevel :: k :: x :: y :: x2 :: y2 :: id1 :: id2 :: Nil =>
        map ++ Map('infile -> infile) ++ Map('infile2 -> infile2) ++ Map('healpixlevel -> healpixlevel.toInt) ++ Map('k -> k.toInt) ++ Map('x -> x.toInt) ++ Map('y -> y.toInt) ++ Map('x2 -> x2.toInt) ++ Map('y2 -> y2.toInt) ++ Map('id1 -> id1) ++ Map('id2 -> id2)
      case option :: tail =>
        println(usage)
        throw new IllegalArgumentException(s"Unknown argument $option");
    }

  }

  def toLong(s: String): Option[Long] = {
    try {
      Some(s.toLong)
    } catch {
      case e: NumberFormatException => None
    }
  }


  def Neighbours(set: Set[Long], healpixlevel: Int): Set[Long] = {
    return set.flatMap(x => HealpixProc.neighboursNest(12, x)).filter(_ != -1)
  }


  def addNeighbours(a: Long, histogram: Broadcast[Map[Long, Long]], histogramS: Broadcast[Map[Long, Long]], k: Int, healpixlevel: Int): Set[Long] = {

    var ipix_precedant = Set(a)
    var limit = 3
    var Nb_objets = histogram.value.filterKeys(ipix_precedant).foldLeft(0L)(_ + _._2)
    var neighbours = Set.empty[Long]
    var neighboursGlobal = Set.empty[Long]
    var nb_voisins: Long = 0

    while ((Nb_objets < k) && (limit >= 0)) {
      neighbours = Neighbours(ipix_precedant, healpixlevel).diff(neighboursGlobal)

      nb_voisins = histogram.value.filterKeys(neighbours).foldLeft(0L)(_ + _._2)

      Nb_objets = Nb_objets + nb_voisins

      ipix_precedant = neighbours
      neighboursGlobal = neighboursGlobal ++ neighbours
      limit = limit - 1

    }


    /*neighbours = Neighbours(ipix_precedant, healpixlevel).diff(neighboursGlobal)
    nb_voisins = histogram.value.filterKeys(neighbours).foldLeft(0L)(_+_._2)

    Nb_objets = Nb_objets+nb_voisins
    neighboursGlobal=neighboursGlobal ++ neighbours

    //val histS = histogram.value.filter(x => x != 0)*/

    val finalSet = neighboursGlobal.filter(histogramS.value.keySet)

    return finalSet
  }

  def main(args: Array[String]) {

    val configuration = parseArguments(Map(), args.toList)
    println(configuration)


    val inputFile_1 = configuration('infile).toString
    val inputFile_2 = configuration('infile2).toString
    val healpixlevel = configuration('healpixlevel).asInstanceOf[Int]
    val k = configuration('k).asInstanceOf[Int]
    val x = configuration('x).asInstanceOf[Int]
    val y = configuration('y).asInstanceOf[Int]
    val x2 = configuration('x2).asInstanceOf[Int]
    val y2 = configuration('y2).asInstanceOf[Int]
    val id1 = configuration('id1).toString
    val id2 = configuration('id2).toString


    val spark = SparkSession.builder().appName("astroide").getOrCreate()
    import spark.implicits._

    val sc: SparkContext = spark.sparkContext
    sc.setLogLevel("ERROR")

    def udfToHealpix = udf((alpha: Double, delta: Double) => {

      val theta = math.Pi / 2 - delta.toRadians
      val phi = alpha.toRadians

      HealpixProc.ang2pixNest(10, new Pointing(theta, phi))
    })

    val inputData_1 = spark.read.parquet(inputFile_1).withColumn("ipix10", udfToHealpix($"ra", $"dec")).filter($"ipix".between(x, y))

    println(inputData_1.count())
    var inputData_2 = spark.read.parquet(inputFile_2).withColumn("ipix10", udfToHealpix($"ra", $"dec")).filter($"ipix".between(x2, y2))

    println(inputData_2.count())

    val start = System.currentTimeMillis()

    val histR = inputData_1.select("ipix10").except(inputData_2.select("ipix10")).withColumn("count", lit(0L))


    val hist = inputData_2.select(id2, "ipix10").groupBy("ipix10")
    val histCount = hist.count()
    var histS = histCount.sort(desc("count"))

    val histSMap = histS.rdd.map(row => (row(0).asInstanceOf[Long], row(1).asInstanceOf[Long])).collectAsMap()

    val histogram = histR.union(histS)


    val histogramMap = histogram.rdd.map(row => (row(0).asInstanceOf[Long], row(1).asInstanceOf[Long])).collectAsMap()

    val br = sc.broadcast(histogramMap)

    val brS = sc.broadcast(histSMap)

    val NeighboursToJoin = udf((a: Long) => (addNeighbours(a, br, brS, k, healpixlevel) + a).toList)

    var explodedData_1 = inputData_1.withColumn("neighbours", explode(NeighboursToJoin($"ipix10")))

    val columnname = inputData_2.columns
    val newnames = columnname.map(x => x + "_2")

    for (i <- newnames.indices) {
      inputData_2 = inputData_2.withColumnRenamed(columnname(i), newnames(i))
    }

    var joined = explodedData_1.join(inputData_2, explodedData_1.col("neighbours") === inputData_2.col("ipix10_2"))

    val sphe_dist = udf((a: Double, d: Double, a2: Double, d2: Double) => sphericalDistance(a, d, a2, d2))

    joined = joined.withColumn("dist", sphe_dist($"ra", $"dec", $"ra_2", $"dec_2"))

    joined = joined.withColumn("rank", row_number().over(Window.partitionBy(id1).orderBy($"dist".asc))).filter($"rank" <= k)

    joined.explain(true)

    println("result" + joined.count())

    val queryTime = System.currentTimeMillis() - start

    println("Query time " + ((queryTime / 1000)) + " sec ")


    spark.stop()
  }

}

