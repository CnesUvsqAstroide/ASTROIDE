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

import healpix.essentials._

import scala.collection.immutable.Range

object ConeSearchCells {

  def getCells(order: Int, alpha: Double, delta: Double, radius: Double): List[Long] = {

    val f: Int = 4

    val theta = (90 - delta).toRadians
    val phi = alpha.toRadians

    val ptg = new Pointing(theta, phi)
    val list = HealpixProc.queryDiscInclusiveNest(order, ptg, radius.toRadians, f)

    return list.toArray().toList

  }

  def groupByRange(in: List[Long], acc: List[List[Long]] = Nil): List[List[Long]] = (in, acc) match {

    case (Nil, a) => a.map(_.reverse).reverse
    case (n :: tail, (last :: t) :: tailAcc) if n == last + 1 => groupByRange(tail, (n :: t) :: tailAcc)
    case (n :: tail, a) => groupByRange(tail, (n :: n :: Nil) :: a)
  }


  def intersectCone(x: List[Long], y: List[Long]): List[Long] = {

    if (x(1) > y(1) || x(2) < y(0)) {

      return Nil
    }
    return List(x(0), math.max(x(1), y(0)), math.min(x(2), y(1)))
  }

  def getIntervals(coneSearchCells: List[Long], boundaries: List[List[Long]]): Map[Long, List[Long]] = {

    var cone = ConeSearchCells.groupByRange(coneSearchCells)

    val intersection = for {
      b <- boundaries;
      c <- cone
      r = ConeSearchCells.intersectCone(b, c)
      if r != Nil
    } yield r

    val intersectiongroup = intersection.groupBy(w => w(0)).mapValues(l => l.map(x => Range.Long(x(1), x(2) + 1, 1)).flatten)
    return intersectiongroup

  }

}


