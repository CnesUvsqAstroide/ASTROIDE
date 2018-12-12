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
package fr.uvsq.adam.astroide.util

object DirCheck {
  def dirExists(file: String, hdfs: String): Boolean = {
    val conf = new org.apache.hadoop.conf.Configuration()
    conf.set("fs.defaultFS", hdfs)
    val fs = org.apache.hadoop.fs.FileSystem.get(conf)
    val path = new org.apache.hadoop.fs.Path(file.substring(0,file.lastIndexOf("/")+1))
    val exists = fs.exists(path)
    return exists
  }

  def dirParquet(file: String, hdfs: String): Boolean = {
    val conf = new org.apache.hadoop.conf.Configuration()
    conf.set("fs.defaultFS", hdfs)
    val fs = org.apache.hadoop.fs.FileSystem.get(conf)
    val path = new org.apache.hadoop.fs.Path(file)
    val exists = fs.listStatus(path).apply(1).getPath.getName.contains("nump")
    return exists
  }
}

