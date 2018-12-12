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
package fr.uvsq.adam.astroide.partitioner

import healpix.essentials._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql._
import java.io._

import scala.Console.{BLUE, GREEN, MAGENTA, RED, RESET}

/**
  * This is the main partitioner of ASTROIDE
  *
  */

object HealpixPartitioner {

  def saveBoundaries(f: java.io.File)(op: java.io.PrintWriter => Unit) {

    val p = new java.io.PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }

  @throws(classOf[Exception])
  def CreatePartitions(spark: SparkSession, input: String, inputData: DataFrame, outputFile: String, capacity: Double, level: Int, coordinates1: String, coordinates2: String, boundariesFile: String) {

    /**
      * Create the HEALPix index
      *
      */

    def udfToHealpix = udf((alpha: Double, delta: Double) => {

      val theta = math.Pi / 2 - delta.toRadians
      val phi = alpha.toRadians

      HealpixProc.ang2pixNest(level, new Pointing(theta, phi))
    })

    val inputDataWithHealpix = inputData.withColumn("ipix", udfToHealpix(col(coordinates1), col(coordinates2)))

    /**
      * Change the spark default number of partitions
      *
      */

    val numpartition = PartitionerUtil.numPartition(inputData, capacity)

    println(s"${BLUE}Number of partitions *****" + numpartition + "******" + RESET)

    spark.conf.set("spark.sql.shuffle.partitions", numpartition)
    val outputData = inputDataWithHealpix.sort(col("ipix"))


    /*implicit var encoder = RowEncoder(outputData.schema)

    implicit def tuple2[A1, A2](
                                 implicit e1: Encoder[A1],
                                 e2: Encoder[A2]): Encoder[(A1, A2)] = Encoders.tuple[A1, A2](e1, e2)

    val newstructure = StructType(Seq(StructField("nump", IntegerType, true)).++(inputDataWithHealpix.schema.fields))

    val mapped = outputData.rdd.mapPartitionsWithIndex((id, iter) => {
      val data = iter.toList
      data.map(x => Row.fromSeq(id +: x.toSeq)).iterator
    }, false)

    val indexDF = spark.createDataFrame(mapped, newstructure)*/

    val indexDF = outputData.withColumn("nump", spark_partition_id())

    indexDF.write.mode(SaveMode.Overwrite).partitionBy("nump").bucketBy(10, "ipix").options(Map("path" -> outputFile)).saveAsTable("t")
    println(s"${BLUE}Partitioned output file " + outputFile + " is created on HDFS" + RESET)

    println(s"${MAGENTA}***** Please wait for metadata creation on " + boundariesFile + "*****" + RESET)

    /**
      * Collect partition boundaries
      *
      */

    val boundaries = indexDF.groupBy(col("nump")).agg(first(col("ipix")), last(col("ipix"))).collect().toList

    saveBoundaries(new File(boundariesFile)) { p =>
      boundaries.foreach(p.println)
    }

    println(s"${BLUE}Boundaries file " + boundariesFile + " is saved " + RESET)


  }
}
