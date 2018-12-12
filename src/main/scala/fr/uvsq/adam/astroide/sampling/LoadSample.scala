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
package fr.uvsq.adam.astroide.sampling

import org.apache.spark.sql.SparkSession

object LoadSample {

  def main(args: Array[String]) {

    val spark = SparkSession.builder().getOrCreate()
    import spark.implicits._

    val inputDataframe = spark.read.option("header", "true").csv(args(0).toString)
    val sampleData = inputDataframe.sample(false, args(1).toDouble)

    sampleData.write.options(Map("header" -> "true")).options(Map("codec" -> "org.apache.hadoop.io.compress.GzipCodec")).csv((args(2).toString))

    println(args(1) + " | " + sampleData.count())

  }
}




