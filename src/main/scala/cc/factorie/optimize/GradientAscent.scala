/* Copyright (C) 2008-2010 University of Massachusetts Amherst,
   Department of Computer Science.
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package cc.factorie.optimize
import cc.factorie._
import cc.factorie.la._

/** Change the weights in the direction of the gradient by a factor of "rate" for each step. */
class StepwiseGradientAscent(var rate: Double = 1.0) extends GradientOptimizer {
  def step(weights:Tensor, gradient:Tensor, value:Double, margin:Double): Unit = {
    weights.+=(gradient, rate)
    rate = nextRate(rate)
  }
  def nextRate(oldRate:Double): Double = oldRate // TODO What should go here?
  def isConverged = false // TODO What to put here?
  def reset(): Unit = {}
}

// Note: This implementation is slower than it should be in the online case, but should be fast enough in batch mode
class L2RegularizedGradientAscent(var l2: Double = 0.1, rate: Double = 1.0) extends StepwiseGradientAscent(rate) {
  override def step(weights:Tensor, gradient:Tensor, value:Double, margin:Double): Unit = {
    gradient += (weights, -l2)
    super.step(weights, gradient, value, margin)
  }
}

class SparseL2RegularizedGradientAscent(var l2: Double = 0.1, rate: Double = 1.0) extends StepwiseGradientAscent(rate) {
  override def step(weights:Tensor, gradient:Tensor, value:Double, margin:Double): Unit = {
    super.step(weights, gradient, value, margin)
    weights *= (1.0 - rate * l2)
  }
}

/** Change the weights in the direction of the gradient by using back-tracking line search to make sure we step up hill. */
class LineSearchGradientAscent(var stepSize: Double = 1.0) extends GradientOptimizer with FastLogging {
  private var _isConverged = false
  def isConverged = _isConverged
  var gradientTolerance = 0.001
  var valueTolerance = 0.0001
  var gradientNormMax = 100.0
  var eps = 1.0e-10
  var oldValue = Double.NaN
  var lineOptimizer: BackTrackLineOptimizer = null
  def reset(): Unit = {
    _isConverged = false
    oldValue = Double.NaN
  }
  def step(weights:Tensor, gradient:Tensor, value:Double, margin:Double): Unit = {
    if (_isConverged) return
    // Check for convergence by value
    if (2.0 * math.abs(value - oldValue) < valueTolerance * (math.abs(value) + math.abs(oldValue) + eps)) {
      logger.info("GradientAscent converged: old value="+oldValue+" new value="+value+" tolerance="+valueTolerance)
      _isConverged = true
      return
    }
    // Check for convergence by gradient
    val gradientTwoNorm = gradient.twoNorm
    if (gradientTwoNorm < gradientTolerance) {
      logger.info("GradientAscent converged: gradient twoNorm="+gradient.twoNorm+" tolerance="+gradientTolerance)
      _isConverged = true
      return
    }

    if (lineOptimizer eq null) {
      // Before giving the BackTrackLineOptimizer a line direction to search, ensure it isn't too steep
     // if (gradientTwoNorm > gradientNormMax) gradient.*=(gradientNormMax / gradientTwoNorm)
      lineOptimizer = new BackTrackLineOptimizer(gradient, gradient.copy, stepSize)
      oldValue = value
    }
    lineOptimizer.step(weights, gradient, value, margin)
    if (!lineOptimizer.isConverged) return
    lineOptimizer = null // So we create a new one next time
    lineOptimizer = new BackTrackLineOptimizer(gradient, gradient.copy, stepSize)
    lineOptimizer.step(weights, gradient, value, margin)
    oldValue = value
  }
}

