package com.gu.aws

case class IntervalDatum(mean: Double, coeffVariation: Double, min: Double, max: Double)

case class Baseline(intervalData: Map[String, IntervalDatum])

