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

object Interval {

  def is_overlapping(x1: Long, x2: Long, y1: Long, y2: Long) = math.max(x1, y1) <= math.min(x2, y2)

  def intersectionList(l1: List[Long], l2: List[Long]) = if (is_overlapping(l1(1), l1(2), l2(0), l2(1))) List(l1(0), math.max(l1(1), l2(0)), math.min(l1(2), l2(1))) else Nil


}
