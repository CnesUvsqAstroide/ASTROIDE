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

import fr.uvsq.adam.astroide.executor.AstroideQueries.astroideVariables
import healpix.essentials.{HealpixProc, Pointing}
import org.apache.spark.sql.types._
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans._
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.execution.{ProjectExec, SparkPlan, TakeOrderedAndProjectExec}
import org.apache.spark.sql.{SparkSession, Strategy, execution}
import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.dsl.plans._


object AstroideStrategies {

  /**
    * Identify knn-join trees and inject new strategies to duplicate each cell in the neighboring cells
    *
    */

  case class knnJoinStrategy(spark: SparkSession) extends Strategy {

    @throws(classOf[Exception])
    override def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {

      case j@Join(Project(l, left), right, Cross, condition) => {

        val ipix1 = left.outputSet.find(x => x.toString().contains("neighbours"))

        val ra = right.outputSet.find(x => x.toString().substring(0, x.toString().indexOf("#")).contentEquals("ra")).last.toAttribute
        val dec = right.outputSet.find(x => x.toString().substring(0, x.toString().indexOf("#")).contentEquals("dec")).last.toAttribute
        val udfTest = ScalaUDF((alpha: String, delta: String) => HealpixProc.ang2pixNest(10, new Pointing(math.Pi / 2 - delta.toDouble.toRadians, alpha.toDouble.toRadians)), LongType, Seq(ra, dec))

        val id2 = right.outputSet.find(x => x.toString().substring(0, x.toString().indexOf("#")).contentEquals(astroideVariables.getid2())).last

        val newlist = Seq(id2, udfTest.as("ipix10"), ra, dec)

        val right1 = Project(newlist, right)

        val ipix2 = right1.outputSet.find(x => x.toString().substring(0, x.toString().indexOf("#")).contentEquals("ipix10")).last.toAttribute

        val l1 = l ++ ipix1

        val left1 = Project(l1, left)

        execution.joins.SortMergeJoinExec(Seq(ipix1.last.expr), Seq(ipix2.expr), Inner, condition, planLater(left1), planLater(right1)) :: Nil
      }

      case _ => Nil

    }
  }

  /**
    * Identify cross-matching trees and inject new strategies to duplicate each cell in the neighboring cells
    *
    */


  case class CrossMatchStrategy(spark: SparkSession) extends Strategy {

    @throws(classOf[Exception])
    override def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {

      case j@Join(left, right, _, Some(condition)) if condition.toString().contains("SphericalDistance") => {

        val ipix1 = left.outputSet.find(x => x.toString().contains("ipix"))
        val ipix = right.outputSet.find(x => x.toString().contains("ipix"))

        val udfNeighbours = ScalaUDF((ipix: Long) => ipix +: HealpixProc.neighboursNest(astroideVariables.getOrder(), ipix), ArrayType(LongType), Seq(ipix.last.toAttribute))

        val explode = Explode(udfNeighbours)

        val generated = right.generate(generator = explode, true, false, alias = Some("alias"), Seq.empty).analyze

        val ipix2 = generated.output.last

        execution.joins.SortMergeJoinExec(Seq(ipix1.last.expr), Seq(ipix2.expr), Inner, Option(condition).reduceOption(And), planLater(left), planLater(generated.analyze)) :: Nil

      }

      case _ => Nil

    }
  }


}


