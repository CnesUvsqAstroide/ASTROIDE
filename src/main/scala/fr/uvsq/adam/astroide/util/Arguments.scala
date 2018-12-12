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

class Arguments {

  private var inputFile1: String = " "
  private var inputFile2: String = " "
  private var order: Int = 12
  private var queryFile: String = " "
  private var hdfs: String = " "
    private var radius: Double = 0.0027
    private var median: Double = 1
    private var pruningRadius: Double = 1
    private var id1: String = " "
    private var id2: String = " "
    private var limit: Int = 0

  def getFile1(): String = {
    return this.inputFile1
  }

  def setFile1(inputFile1: String) {
    this.inputFile1 = inputFile1
  }

  def getFile2(): String = {
    return this.inputFile2
  }

  def setFile2(inputFile2: String) {
    this.inputFile2 = inputFile2
  }

  def getOrder(): Int = {
    return this.order
  }

  def setOrder(order: Int) {
    this.order = order
  }

  def getQueryFile(): String = {
    return this.queryFile
  }

  def setQueryFile(queryFile: String) {
    this.queryFile = queryFile
  }

  def getHDFS(): String = {
    return this.hdfs
  }

  def setHDFS(hdfs: String) {
    this.hdfs = hdfs
  }

    def getRadius(): Double = {
	return this.radius
    }

    def setRadius(radius: Double) {
	this.radius = radius
    }

    def getMedian(): Double = {
	return this.median
    }

    def setMedian(median: Double) {
	this.median = median
    }


    def getPruningRadius(): Double = {
	return this.pruningRadius
    }

    def setPruningRadius(radius: Double) {
	this.pruningRadius = pruningRadius
    }


    def getid1(): String = {
	return this.id1
    }

    def setid1(id1: String) {
	this.id1 = id1
    }

    def getid2(): String = {
	return this.id2
    }

    def setid2(id2: String) {
	this.id2 = id2
    }


    def getLimit(): Int = {
	return this.limit
    }

    def setLimit(limit: Int) {
	this.limit = limit
    }


}
