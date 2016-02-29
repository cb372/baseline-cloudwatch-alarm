package com.gu.aws

import com.amazonaws.auth.{ InstanceProfileCredentialsProvider, AWSCredentialsProviderChain }
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.cloudwatch.model.{ GetMetricStatisticsRequest, Dimension, GetMetricStatisticsResult }
import com.amazonaws.services.cloudwatch.{ AmazonCloudWatch, AmazonCloudWatchClient }
import org.joda.time.DateTime

object CloudWatch {

  val credsProvider = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("capi"),
    new InstanceProfileCredentialsProvider()
  )
  val cloudWatchClient: AmazonCloudWatch = new AmazonCloudWatchClient(credsProvider).withRegion(Regions.EU_WEST_1)

  def getMetricStatistics(config: LambdaConfig, startTime: DateTime, endTime: DateTime): GetMetricStatisticsResult = {
    val metricsRequest = new GetMetricStatisticsRequest()
    metricsRequest.setNamespace(config.namespace)
    metricsRequest.setMetricName(config.metricName)
    metricsRequest.setDimensions(java.util.Arrays.asList(new Dimension().withName(config.dimensionName).withValue(config.dimensionValue)))
    metricsRequest.setStatistics(java.util.Arrays.asList(config.statistic.toString))
    metricsRequest.setPeriod(config.periodMinutes * 60)
    metricsRequest.setStartTime(startTime.toDate)
    metricsRequest.setEndTime(endTime.toDate)

    cloudWatchClient.getMetricStatistics(metricsRequest)
  }
}
