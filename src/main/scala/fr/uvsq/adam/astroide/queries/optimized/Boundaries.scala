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
package fr.uvsq.adam.astroide.queries.optimized

import scala.io.Source

/**
  * Contains the main boundaries functions such as read, intersect
  *
  */

object Boundaries {

  def intersectWithBoundaries(x: List[Long], y: List[Long], i: Int): List[Long] = {

    if (x(i) > y(i + 1) || x(i + 1) < y(i)) {
      return Nil
    }
    return List(x(i - 1), y(i - 1), math.max(x(i), y(i)), math.min(x(i + 1), y(i + 1)))
  }

  def intersectWithElement(x: Long, bl: List[List[Long]]) = for {

    b <- bl
    lu = Range.Long(b(1), b(2) + 1, 1)
    if (lu.contains(x))
  } yield b(0)

  def convertToRange(boundaries: List[List[Long]]) = for {

    i <- boundaries
    ranges = (i(0), i(1), Range.Long(i(2), i(3) + 1, 1))

  } yield ranges

  def convertToRange2(boundaries: List[List[Long]]) = for {

    i <- boundaries
    ranges = (Range.Long(i(1), i(2) + 1, 1))

  } yield ranges

  @throws(classOf[java.io.IOException])
  def ReadFromFile(file: String): List[List[Long]] = {

    var boundaries = Source.fromFile(file).getLines.map { line =>
      line.drop(1).dropRight(1).split(",").map(_.toLong)
        .toList
    }.toList

    return boundaries

  }
}
