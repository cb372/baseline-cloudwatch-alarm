package com.gu.aws

case class LambdaConfig(
  namespace: String,
  metricName: String,
  dimensionName: String,
  dimensionValue: String,
  periodMinutes: Int,
  statistic: Statistic)
