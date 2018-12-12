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

import adql.query.ADQLObject
import adql.query.from.CrossJoin
import adql.search.SimpleSearchHandler

/**
	* This class identifies the cross-join in ADQL queries
	*
	*/

class SearchKNNJoin extends SimpleSearchHandler(true, false) {

	override def `match`(obj: ADQLObject): Boolean = {
		try {
			val comp = obj.asInstanceOf[CrossJoin]

			return true
		}

		catch {
			case cce: ClassCastException ⇒ false
		}
	}


}
