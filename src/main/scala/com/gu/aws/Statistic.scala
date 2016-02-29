package com.gu.aws

import com.amazonaws.services.cloudwatch.model.Datapoint

sealed trait Statistic {
  def getValue(datapoint: Datapoint): Double
}

case object Sum extends Statistic {
  def getValue(datapoint: Datapoint) = datapoint.getSum
}

case object Average extends Statistic {
  def getValue(datapoint: Datapoint) = datapoint.getAverage
}

