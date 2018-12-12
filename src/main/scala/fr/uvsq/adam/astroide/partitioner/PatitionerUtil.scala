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

import org.apache.spark.util.SizeEstimator
import org.apache.spark.sql.DataFrame

object PartitionerUtil {

  def DFSize(df: DataFrame): Double = {

    //val dfsize = SizeEstimator.estimate(df)
    val dfsize = df.count()*(df.first().size*8)
    return dfsize / (1024 * 1024)

  }

  def numPartition(df: DataFrame, partitionSize: Double): Int = {

    val dfSize = PartitionerUtil.DFSize(df)
    val np = math.ceil(dfSize * 1.3 / partitionSize)

    return (np.toInt)

  }

  def parquetSize(file: String): Double = {

    val hdfscf: org.apache.hadoop.fs.FileSystem =
      org.apache.hadoop.fs.FileSystem.get(
        new org.apache.hadoop.conf.Configuration())

    val hadoopPath = new org.apache.hadoop.fs.Path(file)

    val recursive = false
    val ri = hdfscf.listFiles(hadoopPath, recursive)
    val it = new Iterator[org.apache.hadoop.fs.LocatedFileStatus]() {
      override def hasNext = ri.hasNext

      override def next() = ri.next()
    }
    val files = it.toList

    val size = files.map(_.getLen).sum
    val sizeMB = size / (1024 * 1024)
    return sizeMB
  }


}




