package com.gu.aws

import java.util.{ Map => JMap }
import com.amazonaws.services.lambda.runtime.{ RequestHandler, Context }
import org.joda.time.DateTime

import scala.collection.JavaConverters._

class Lambda extends RequestHandler[JMap[String, Object], Unit] {
  import LambdaInput._

  /**
   * How we decide whether a given baseline datum is reliable or not.
   * If it's reliable, we use the mean as our baseline figure.
   * If not, we use the min or max, depending on whether the alarm threshold is less than or more than 1.0.
   */
  val CoeffVariationThreshold = 0.2

  val AlarmThreshold = 0.6 // TODO this should be part of the config

  override def handleRequest(event: JMap[String, Object], context: Context): Unit = {
    val endTime = DateTime.now
    val startTime = endTime.minusMinutes(config.periodMinutes)
    val metricsResponse = CloudWatch.getMetricStatistics(config, startTime, endTime)
    val datapoints = metricsResponse.getDatapoints.asScala.sortBy(_.getTimestamp.getTime)
    datapoints.foreach { point =>
      println(s"Checking metric value for ${config.periodMinutes} minutes up to $endTime")
      val intervalKey = DateManipulation.toIntervalKey(point.getTimestamp, config.periodMinutes)
      for (baselineDatum <- baseline.intervalData.get(intervalKey)) {
        println(s"Found the baseline datum for the interval $intervalKey")
        val baselineFigure = {
          if (baselineDatum.coeffVariation < CoeffVariationThreshold) {
            println("Baseline looks reliable for this interval. Using mean as the baseline figure.")
            baselineDatum.mean
          } else {
            println("Baseline looks unreliable for this interval. Using minimum as the baseline figure.")
            baselineDatum.min
          }
        }

        val actualValue = config.statistic.getValue(point)
        val thresholdValue = AlarmThreshold * baselineFigure
        if (actualValue < thresholdValue) {
          println(s"TODO send an alert! CloudWatch metric [${config.namespace}, ${config.metricName}, ${config.dimensionName}=${config.dimensionValue}, ${config.statistic} for the last ${config.periodMinutes} minutes] was less than $AlarmThreshold of the baseline value ($baselineFigure). The actual value was $actualValue.")
        } else {
          println(s"All good. CloudWatch metric [${config.namespace}, ${config.metricName}, ${config.dimensionName}=${config.dimensionValue}, ${config.statistic} for the last ${config.periodMinutes} minutes] was greater than $AlarmThreshold of the baseline value ($baselineFigure). The actual value was $actualValue.")
        }
      }
    }
  }

}
