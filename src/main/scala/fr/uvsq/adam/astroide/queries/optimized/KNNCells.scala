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

object KNNCells {

  def intersectCell(x: Long, y: List[Long]): Boolean = {

    if (x > y(2) || x < y(1)) {

      return false
    }
    else return true
  }

  def overlap(b: List[List[Long]], x: Long): List[Long] = {
    b.foreach { elem => if (intersectCell(x, elem)) return List(elem(0), elem(1), elem(2)) }.asInstanceOf[List[Long]]
  }

  def getRange(b: List[List[Long]], x: Long): List[Long] = {
    b.foreach { elem => if (elem(0) == x) return List(elem(1), elem(2)) }.asInstanceOf[List[Long]]
  }

}
