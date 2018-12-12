
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

package fr.uvsq.adam.astroide

import fr.uvsq.adam.astroide.optimizer.{AstroideRules, AstroideStrategies}
import org.apache.spark.sql.{SparkSession, SparkSessionExtensions}

/**
  * Extend the spark session with ASTROIDE rules and strategies
  *
  */

trait AstroideSession {

  type ExtensionsBuilder = SparkSessionExtensions => Unit
  val f: ExtensionsBuilder = { e => e.injectOptimizerRule(AstroideRules.ConeSearchRule) }
  val j: ExtensionsBuilder = { e => e.injectOptimizerRule(AstroideRules.kNNRule) }
  val l: ExtensionsBuilder = { e => e.injectOptimizerRule(AstroideRules.kNNRuleProject) }
  val k: ExtensionsBuilder = { e => e.injectPlannerStrategy(AstroideStrategies.CrossMatchStrategy) }
  val p: ExtensionsBuilder = { e => e.injectOptimizerRule(AstroideRules.SphericalDistanceOptimizationRule) }
  val s: ExtensionsBuilder = { e => e.injectOptimizerRule(AstroideRules.MatchRule) }
  val v: ExtensionsBuilder = { e => e.injectResolutionRule(AstroideRules.knnJoinRule) }
  val w: ExtensionsBuilder = { e => e.injectPlannerStrategy(AstroideStrategies.knnJoinStrategy) }
  val x: ExtensionsBuilder = { e => e.injectResolutionRule(AstroideRules.knnJoinRule2) }
  val m: ExtensionsBuilder = { e => e.injectOptimizerRule(AstroideRules.ProjectRule) }

  lazy val astroideSession: SparkSession = {
    SparkSession
      .builder()
      .appName("ASTROIDE")
      .withExtensions(s)
      .withExtensions(f)
      .withExtensions(j)
      .withExtensions(k)
      .withExtensions(l)
      .withExtensions(v)
      .withExtensions(w)
      .withExtensions(x)
      .withExtensions(m)
      .getOrCreate()
  }
}

