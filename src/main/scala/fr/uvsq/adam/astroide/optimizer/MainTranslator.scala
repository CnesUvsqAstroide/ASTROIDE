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
package fr.uvsq.adam.astroide.optimizer

import adql.query.ADQLObject

object MainTranslator {

  /**
    * Translate queries in case of query rewriting
    *
    */

  def translateWithRules(query: ADQLObject): String = {

    val translatorJoin = new TranslatorJoin()
    val translatorContains = new TranslatorContains()
    val translatorDistance = new TranslatorDistance()
    val translator = new AstroideTranslator()

    translatorContains.searchAndReplace(query)
    translatorJoin.searchAndReplace(query)
    translatorDistance.searchAndReplace(query)

    val newquery = translator.translate(query)
    return newquery
  }

  /**
    * Translate queries in case of transformation rules
    *
    */

  def translateWithoutRules(query: ADQLObject): String = {
    val translator = new SparkTranslator()

    val newquery = translator.translate(query)
    return newquery
  }

}
