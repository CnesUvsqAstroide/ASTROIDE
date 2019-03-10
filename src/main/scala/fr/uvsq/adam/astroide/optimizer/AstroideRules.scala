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

import fr.uvsq.adam.astroide.AstroideUDF
import fr.uvsq.adam.astroide.executor.AstroideQueries.astroideVariables
import fr.uvsq.adam.astroide.queries.optimized.{Boundaries, ConeSearchCells, KNNCells}
import healpix.essentials.{HealpixProc, Pointing}
import org.apache.spark.sql.catalyst.plans.Cross
import org.apache.spark.sql.types.DoubleType
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.rules.Rule
import org.apache.spark.sql.execution.datasources.LogicalRelation


object AstroideRules {

  /**
    * Inject cone search rule in filter operator that uses spherical distance expression
    *
    */
  // changer l'ordre de parameters pour collecter les cells
  case class ConeSearchRule(spark: SparkSession) extends Rule[LogicalPlan] {

    @throws(classOf[Exception])
    override def apply(plan: LogicalPlan): LogicalPlan = plan transform {

      case f@Filter(condition, child)=>

        if (condition.toString().contains("SphericalDistance")) {
          val nump = child.outputSet.toList.find(x => x.toString().contains("nump"))
          val ipix = child.outputSet.toList.find(x => x.toString().contains("ipix"))

          var boundaries = Boundaries.ReadFromFile(astroideVariables.getFile2())

          val parameters = AstroideUDF.getParameters(condition.toString())
          //println(parameters.length);
          //for(p <- parameters)
           // println(p);

          var coneSearchCells = ConeSearchCells.getCells(astroideVariables.getOrder(), parameters(3).toDouble, parameters(4).toDouble, parameters(5).toDouble).sorted

          val intervals = coneSearchCells.distinct.toSet[Any]

          val intervalsnump = ConeSearchCells.getIntervals(coneSearchCells, boundaries).keys.map(x=> Literal(x.toInt)).toSeq

          val newfilter = And(condition, And(In(ExpressionSet(nump).head, intervalsnump), InSet(ExpressionSet(ipix).head, intervals)))

          Filter(condition = newfilter, child)
        }
        else f
    }

  }

  /**
    * Identify knn trees and inject rules to filter selected partitions (or execute a cone search with limit)
    *
    */


  // changer l'ordre de parametres pour collecter theta et phi pour les requetes de type Knn
  case class kNNRule(spark: SparkSession) extends Rule[LogicalPlan] {

    @throws(classOf[Exception])
    override def apply(plan: LogicalPlan): LogicalPlan = plan transform {

      case l@LocalLimit(_, sort@Sort(sortOrder, true, p@Project(projectList, child))) =>

        val condition = p.expressions.mkString

        if (condition.contains("SphericalDistance")) {

          val parameters = AstroideUDF.getParameters(condition)
          var boundaries = Boundaries.ReadFromFile(astroideVariables.getFile2())
          println(parameters.length);
          for(p <- parameters)
           println(p);
         // val coordinates1 = parameters(5)
         // val coordinates2 = parameters(6)

          val coordinates1 = parameters(3)
          val coordinates2 = parameters(4)

          val theta = (90 - (coordinates2.toDouble)).toRadians
          val phi = (coordinates1.toDouble).toRadians

          val ptg = new Pointing(theta, phi)

          val cell = HealpixProc.ang2pixNest(astroideVariables.getOrder(), ptg)
          var overlappingPartition = KNNCells.overlap(boundaries, cell)
          val nump = child.outputSet.toList.find(x => x.toString().contains("nump"))

          val newfilter = EqualTo(ExpressionSet(nump).head, Literal(overlappingPartition(0).toInt))
         // var coneSearchCells = ConeSearchCells.getCells(astroideVariables.getOrder(), parameters(5).toDouble, parameters(6).toDouble, astroideVariables.getRadius()).sorted

          var coneSearchCells = ConeSearchCells.getCells(astroideVariables.getOrder(), parameters(3).toDouble, parameters(4).toDouble, astroideVariables.getRadius()).sorted

          val RangePartition = KNNCells.getRange(boundaries, overlappingPartition(0))

          if ((coneSearchCells.last <= RangePartition(1)) && (coneSearchCells.head >= RangePartition(0))) {
            l.copy(child = Sort(sortOrder, true, p.copy(child = Filter(newfilter, child))))
          }


          else {

            val intervals = coneSearchCells.toSet[Any]

            val intervalsnump = ConeSearchCells.getIntervals(coneSearchCells, boundaries).keys.map(x => Literal(x.toInt)).toSeq

            val filter1 = In(ExpressionSet(nump).head, intervalsnump)

            val ipix = child.outputSet.toList.find(x => x.toString().contains("ipix"))

            val filter2 = InSet(ExpressionSet(ipix).head, intervals)

            val newfilter = And(filter1, filter2)

            l.copy(child = Sort(sortOrder, true, p.copy(child = Filter(newfilter, child))))
          }

        }
        else l

    }
  }

  /**
    * Identify knn trees with project and inject rules to filter selected partitions
    *
    */

  case class kNNRuleProject(spark: SparkSession) extends Rule[LogicalPlan] {

    @throws(classOf[Exception])
    override def apply(plan: LogicalPlan): LogicalPlan = plan transform {

      case l@LocalLimit(_, Project(plist1, sort@Sort(sortOrder, true, p@Project(projectList, child)))) =>

        val condition = p.expressions.mkString

        if (condition.contains("SphericalDistance")) {

          val parameters = AstroideUDF.getParameters(condition)

          var boundaries = Boundaries.ReadFromFile(astroideVariables.getFile2())

          val coordinates1 = parameters(4)
          val coordinates2 = parameters(5)

          val theta = (90 - (coordinates2.toDouble)).toRadians
          val phi = (coordinates1.toDouble).toRadians

          val ptg = new Pointing(theta, phi)

          val cell = HealpixProc.ang2pixNest(astroideVariables.getOrder(), ptg)
          var overlappingPartition = KNNCells.overlap(boundaries, cell)

          val nump = child.outputSet.toList.find(x => x.toString().contains("nump"))

          val newfilter = EqualTo(ExpressionSet(nump).head, Literal(overlappingPartition(0).toInt))

          var coneSearchCells = ConeSearchCells.getCells(astroideVariables.getOrder(), parameters(5).toDouble, parameters(6).toDouble, astroideVariables.getRadius()).sorted

          val RangePartition = KNNCells.getRange(boundaries, overlappingPartition(0))

          if ((coneSearchCells.last <= RangePartition(1)) && (coneSearchCells.head >= RangePartition(0))) {
            l.copy(child = Project(plist1, Sort(sortOrder, true, p.copy(child = Filter(newfilter, child)))))
          }

          else {

            val intervals = coneSearchCells.toSet[Any]

            val intervalsnump = ConeSearchCells.getIntervals(coneSearchCells, boundaries).keys.map(x => Literal(x.toInt)).toSeq

            val filter1 = In(ExpressionSet(nump).head, intervalsnump)

            val ipix = child.outputSet.toList.find(x => x.toString().contains("ipix"))

            val filter2 = InSet(ExpressionSet(ipix).head, intervals)

            val newfilter = And(filter1, filter2)

            l.copy(child = Project(plist1, Sort(sortOrder, true, p.copy(child = Filter(newfilter, child)))))
          }



        }
        else l

    }
  }


  /**
    * Identify joining trees with spherical distance expression and transform the plan into an equi-join
    *
    */

  case class MatchRule(spark: SparkSession) extends Rule[LogicalPlan] {

    @throws(classOf[Exception])
    override def apply(plan: LogicalPlan): LogicalPlan = plan transformUp {

      case Project(projectList, r@LogicalRelation(t, relationList, _)) if !projectList.toString().contains("SphericalDistance")=>

        val ipix = relationList.find(x => x.toString().contains("ipix"))

        Project(projectList ++ ipix, r)
    }

  }


  case class ProjectRule(spark: SparkSession) extends Rule[LogicalPlan] {

    @throws(classOf[Exception])
    override def apply(plan: LogicalPlan): LogicalPlan = plan transformUp {

      case Project(projectList, r@LogicalRelation(t, relationList, _)) =>

        val ipix = relationList.find(x => x.toString().contains("ipix10"))

        Project(projectList ++ ipix, r)
    }

  }

  /**
    * Identify knn-join trees and transform the plan into an internal plan in ASTROIDE
    *
    */

  case class knnJoinRule(spark: SparkSession) extends Rule[LogicalPlan] {

    @throws(classOf[Exception])
    override def apply(plan: LogicalPlan): LogicalPlan = plan transform {

      case g@GlobalLimit(_, l@LocalLimit(limit, p@Project(plist, Sort(sortOrder, true, Project(_, SubqueryAlias(_, Project(_, SubqueryAlias(_, SubqueryAlias(_, child))))))))) => {

        LocalLimit(limit, Project(plist, child))

      }

    }
  }

  /**
    * knn-join plan transformation
    *
    */

  case class knnJoinRule2(spark: SparkSession) extends Rule[LogicalPlan] {

    @throws(classOf[Exception])
    override def apply(plan: LogicalPlan): LogicalPlan = plan transform {

      case p@Project(listAll,f@Filter(filterCondition, j@Join(left, right, Cross, _))) if filterCondition.toString().contains("IN") && (filterCondition.references.contains(right.outputSet.find(x => x.toString().substring(0, x.toString().indexOf("#")).contentEquals(astroideVariables.getid2())).last.toAttribute)) => {

        val ra = left.outputSet.find(x => x.toString().substring(0, x.toString().indexOf("#")).contentEquals("ra")).last
        val dec = left.outputSet.find(x => x.toString().substring(0, x.toString().indexOf("#")).contentEquals("dec")).last

        val ra_2 = right.outputSet.find(x => x.toString().substring(0, x.toString().indexOf("#")).contentEquals("ra")).last
        val dec_2 = right.outputSet.find(x => x.toString().substring(0, x.toString().indexOf("#")).contentEquals("dec")).last

        val id1 = left.outputSet.find(x => x.toString().substring(0, x.toString().indexOf("#")).contentEquals(astroideVariables.getid1())).last

        val id2 = right.outputSet.find(x => x.toString().substring(0, x.toString().indexOf("#")).contentEquals(astroideVariables.getid2())).last


        val udfTest = ScalaUDF((v1: String, v2: String, v3: String, v4: String) => AstroideUDF.sphericalDistance(v1.toDouble, v2.toDouble, v3.toDouble, v4.toDouble), DoubleType, Seq(ra, dec, ra_2, dec_2))

        val windowExpr = WindowExpression('rank.function('dist), WindowSpecDefinition(Seq(id1), Seq('dist.asc), UnspecifiedFrame)).as("rank")


        val newlist = listAll.union(Seq(udfTest.as("dist")))

        val cd = 'rank <= astroideVariables.getLimit()

        val filterResult = Filter(cd, Window(Seq(windowExpr), Seq(id1), Seq('dist.asc), Project(newlist, j)))

        Project(filterResult.output.dropRight(2),child = filterResult)
      }
    }
  }

  case class SphericalDistanceOptimizationRule(spark: SparkSession) extends Rule[LogicalPlan] {
    override def apply(plan: LogicalPlan): LogicalPlan = {

      val newright = 0.05

      plan transformAllExpressions {

        case LessThan(left, right) if right.asInstanceOf[Literal].value.isInstanceOf[Double] && left.numberedTreeString.contains("SphericalDistance") =>

          LessThan(left, Literal(newright, DoubleType))
      }
    }
  }


}
