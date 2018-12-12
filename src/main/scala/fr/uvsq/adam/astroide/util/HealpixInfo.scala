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

import healpix.essentials.HealpixBase
import healpix.essentials.Scheme
import healpix.essentials.Pointing

object HealpixInfo {

  def healpixCell(a: String, d: String, ns: Long): Double = {

    try {
      var alpha = a.toDouble
      var delta = d.toDouble
      var phi: Double = (90 - delta).toRadians
      var theta: Double = alpha.toRadians
      var ptg: Pointing = new Pointing(phi, theta)
      var b = new HealpixBase(ns, Scheme.NESTED);
      var ipix = b.ang2pix(ptg)
      return ipix
    } catch {
      case e: Exception => return (-1.0)
    }
  }

  def ExtractHealpix(a: String): Int = {

    var l = a.toLong
    var b = java.lang.Long.toBinaryString(l)
    while (b.length() < 64) {
      b = "0" + b
    }

    var str = b.substring(0, 31)
    val f = java.lang.Integer.parseInt(str, 2)
    return f

  }
}

