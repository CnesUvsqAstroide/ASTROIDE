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

import adql.query.{ADQLQuery, SelectAllColumns, SelectItem}
import adql.translator.{PostgreSQLTranslator, TranslationException}

class AstroideTranslator extends PostgreSQLTranslator {


  @throws(classOf[TranslationException])
  def appendIdentifier(str: StringBuilder, id: String, caseSensitive: Boolean): StringBuilder = {

    return str.append(id)
  }

  /**
    * Translate ADQL alias
    *
    */

  override def translate(item: SelectItem): String = {

    if (item.isInstanceOf[SelectAllColumns])
      return "*"

    if (item.hasAlias()) {
      var str = StringBuilder.newBuilder.append(translate(item.getOperand()))
      str.append(" AS ")

      if (item.isCaseSensitive())
        appendIdentifier(str, item.getAlias(), true)
      else
        appendIdentifier(str, item.getAlias().toLowerCase(), true)

      return str.toString()
    }

    else {

      var str = StringBuilder.newBuilder.append(translate(item.getOperand()))

      return str.toString()

    }

  }

  /**
    * Identify query clauses
    *
    */

  @throws(classOf[TranslationException])
  override def translate(query: ADQLQuery): String = {

    val sql = StringBuilder.newBuilder

    sql.append((translate(query.getSelect())))

    sql.append("\nFROM ").append(translate(query.getFrom()))


    if (!query.getWhere().isEmpty())
      sql.append('\n').append(translate(query.getWhere()))


    if (!query.getGroupBy().isEmpty())
      sql.append('\n').append(translate(query.getGroupBy()))

    if (!query.getHaving().isEmpty())
      sql.append('\n').append(translate(query.getHaving()))

    if (!query.getOrderBy().isEmpty())
      sql.append('\n').append(translate(query.getOrderBy()))


    if (query.getSelect().hasLimit())
      sql.append("\nLimit ").append(query.getSelect().getLimit())

    return sql.toString()
  }

}