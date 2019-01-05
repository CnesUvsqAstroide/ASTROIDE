
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

import fr.uvsq.adam.astroide.partitioner.HealpixPartitioner
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import fr.uvsq.adam.astroide.util.DirCheck.dirExists
import java.io.IOException

import fr.uvsq.adam.astroide.AstroideSession
import healpix.essentials.Pointing
import org.apache.commons.io.FilenameUtils

import Console.{BLUE, GREEN, RED, RESET}
import scala.io.Source

object BuildHealpixPartitioner extends AstroideSession {
  val usage =

    """
      |Usage: BuildHealpixPartitioner -fs hdfs://... schema(optional) infile separator outfile.parquet hdfscapacity healpixlevel column1 column2 boundariesfile
    """.stripMargin

  type OptionMap = Map[Symbol, Any]

  def parseArguments(map: OptionMap, arguments: List[String]): OptionMap = {
    arguments match {
      case Nil => map
      case "-fs" :: hdfs :: tail =>
        parseArguments(map ++ Map('hdfs -> hdfs), tail)
      case "-schema" :: schema :: tail =>
        parseArguments(map ++ Map('schema -> schema), tail)
      case "-hdu" :: hdu :: tail =>
        parseArguments(map ++ Map('hdu -> hdu), tail)
      case infile :: separator :: outfile :: capacity :: healpixlevel :: column1 :: column2 :: boundariesfile :: Nil =>
        map ++ Map('infile -> infile) ++ Map('separator -> separator) ++ Map('outfile -> outfile) ++ Map('capacity -> capacity.toDouble) ++ Map('healpixlevel -> healpixlevel.toInt) ++ Map('column1 -> column1) ++ Map('column2 -> column2) ++ Map('boundariesfile -> boundariesfile)
      case infile :: outfile :: capacity :: healpixlevel :: column1 :: column2 :: boundariesfile :: Nil =>
        map ++ Map('infile -> infile) ++ Map('separator -> None) ++ Map('outfile -> outfile) ++ Map('capacity -> capacity.toDouble) ++ Map('healpixlevel -> healpixlevel.toInt) ++ Map('column1 -> column1) ++ Map('column2 -> column2) ++ Map('boundariesfile -> boundariesfile)
      case option :: tail =>
        println(usage)
        throw new IllegalArgumentException(s"${RED}Unknown argument $option" + RESET);
    }

  }

  /**
    * Start partitioning of astronomical data
    *
    */

  def main(args: Array[String]) {

    val configuration = parseArguments(Map(), args.toList)
    println(configuration)

    val schema = if(configuration.contains('schema)) configuration('schema).toString
                 else None.toString()
    val hdfs = configuration('hdfs).toString
    var input = configuration('infile).toString
    val separator = configuration('separator).toString
    val hdu = if(configuration.contains('hdu)) configuration('hdu).toString
              else None.toString
    val output = configuration('outfile).toString
    val capacity_hdfs = configuration('capacity).asInstanceOf[Double]
    val level_healpix = configuration('healpixlevel).asInstanceOf[Int]
    val column1 = configuration('column1).toString
    val column2 = configuration('column2).toString
    val boundaries = configuration('boundariesfile).toString


    import astroideSession.implicits._

    /**
      * Check input file
      *
      */

    if (!dirExists(input, hdfs)) {
      throw new IOException(s"${RED}Input file " + input + " does not exist" + RESET)
    }

    val format = List("csv", "gz","fits")

    if (!format.contains(FilenameUtils.getExtension(input)))
      throw new Exception(s"${RED}Input file " + input + " should be in csv or fits format" + RESET)


    def ParseAndLoadDataFrame(structSchema : Any) = {

    }

    val Dataframe = if (schema == "None") {
      if (FilenameUtils.getExtension(input) == "csv" || FilenameUtils.getExtension(input) == "gz")
        astroideSession.read.format("csv").option("delimiter", separator).option("header", true).load(input)
      else if (FilenameUtils.getExtension(input) == "fits"){
        if(hdu != None)
          astroideSession.read.format("fits").option("hdu", hdu).option("header", true).load(input)
        else
          throw new Exception("argument Hdu not found");
      }
      else null
    }
    else {
      val schemaString = Source.fromFile(schema).getLines.mkString
      val structSchema = StructType(schemaString.split(",").map(fieldName => StructField(fieldName, StringType, true)))
      if (FilenameUtils.getExtension(input) == "csv" || FilenameUtils.getExtension(input) == "gz")
        astroideSession.read.format("csv").option("delimiter", separator).option("header", true).schema(structSchema).load(input)
      else if (FilenameUtils.getExtension(input) == "fits"){
        if(hdu != None)
          astroideSession.read.format("fits").option("hdu", hdu).option("header", true).schema(structSchema).load(input)
        else
          throw new Exception("argument Hdu not found");
      }
      else null
    }

    if (level_healpix < 0 || level_healpix > 29) {
      throw new Exception(s"${RED}HEALPix order should be in range [0,29]" + RESET)
    }


    try {

      val healpixClass = new Pointing(0, 0)

      val start = System.currentTimeMillis()

      HealpixPartitioner.CreatePartitions(astroideSession, input, Dataframe, output, capacity_hdfs, level_healpix, column1, column2, boundaries)

      val timeTotal = System.currentTimeMillis() - start

      println(s"${GREEN}Total partitioning time " + timeTotal / 1000 + " sec " + RESET)

      astroideSession.stop()
    }

    catch {
      case a: java.lang.ClassNotFoundException => println(s"${RED}Please defind a valid class path to HEALPix library " + a.getMessage + RESET)
      case j: IOException => println(s"${RED}Error occurred while writing boundaries " + j.getMessage + RESET)
      case p: Exception ⇒ println(s"${RED}Error occurred while partitioning " + p.getMessage + RESET)
    }


  }
}
