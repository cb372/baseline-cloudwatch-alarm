package com.gu.aws

import java.nio.charset.StandardCharsets
import java.nio.file.{ Paths, Files }

import com.amazonaws.services.cloudwatch.model.Datapoint
import org.joda.time.DateTime
import scopt.{ Read, OptionParser }

import scala.collection.JavaConverters._

/**
 * Given configuration parameters passed in as command line args,
 *
 * 1. Requests the relevant metrics data for the last 2 weeks from CloudWatch
 * 2. Calculates the baseline figures for each N-minute period
 * 3. Writes the baseline data and the config, as Scala code, to the managed source folder
 */
object LambdaInputGenerator extends App {

  val optionParser = new OptionParser[LambdaConfig]("CodeGenerator") {

    arg[String]("namespace") action { (s, cfg) => cfg.copy(namespace = s)
    } text "The CloudWatch metrics namespace"

    arg[String]("metric-name") action { (s, cfg) => cfg.copy(metricName = s)
    } text "The CloudWatch metric name"

    arg[String]("dimension-name") action { (s, cfg) => cfg.copy(dimensionName = s)
    } text "The CloudWatch metric dimension name"

    arg[String]("dimension-value") action { (s, cfg) => cfg.copy(dimensionValue = s)
    } text "The CloudWatch metric dimension value"

    arg[Int]("period-minutes") action { (i, cfg) => cfg.copy(periodMinutes = i)
    } text "The CloudWatch metric period in minutes"

    implicit val statisticRead = Read.reads[Statistic] {
      case "Sum" | "sum" => Sum
      case "Average" | "average" => Average
    }
    arg[Statistic]("statistic") action { (s, cfg) => cfg.copy(statistic = s)
    } text "The CloudWatch metric statistic (Sum or Average)"

    checkConfig { c =>
      if (c.periodMinutes < 1)
        failure("Period must be greater than zero")
      else
        success
    }
  }

  optionParser.parse(args, LambdaConfig("", "", "", "", -1, Sum)) foreach { config =>
    val configAsString =
      s"""LambdaConfig(
         |    namespace = "${config.namespace}",
         |    metricName = "${config.metricName}",
         |    dimensionName = "${config.dimensionName}",
         |    dimensionValue = "${config.dimensionValue}",
         |    periodMinutes = ${config.periodMinutes},
         |    statistic = ${config.statistic}
         |  )
       """.stripMargin

    val baseline = generateBaseline(config)
    val baselineAsString =
      s"""Baseline(Map(
         |${baseline.intervalData.toSeq.sortBy(_._1).map { case (k, v) => s"""    "$k" -> $v""" }.mkString(",\n")}
         |  ))
       """.stripMargin

    val code = s"""
       |package com.gu.aws
       |
       |object LambdaInput {
       |
       |  val config = $configAsString
       |
       |  val baseline = $baselineAsString
       |}
     """.stripMargin

    val targetDir = Paths.get("target/scala-2.11/src_managed/lambda")
    Files.createDirectories(targetDir)

    val targetFile = targetDir.resolve("LambdaInput.scala")
    Files.write(targetFile, code.getBytes(StandardCharsets.UTF_8))

    println(s"Generated code and wrote it to $targetFile")
  }

  def generateBaseline(config: LambdaConfig): Baseline = {
    val startTime = DateManipulation.roundToStartOfInterval(DateTime.now.minusWeeks(2), config.periodMinutes)
    val endTime = DateManipulation.roundToStartOfInterval(DateTime.now, config.periodMinutes)
    val metricsResponse = CloudWatch.getMetricStatistics(config, startTime, endTime)
    val datapoints = metricsResponse.getDatapoints.asScala.sortBy(_.getTimestamp.getTime)

    calculateBaseline(datapoints, config.periodMinutes, config.statistic)
  }

  def calculateBaseline(datapoints: Seq[Datapoint], intervalMinutes: Int, statistic: Statistic): Baseline = {
    val datapointsByInterval = datapoints.foldLeft[Map[String, List[Datapoint]]](Map.empty) {
      case (map, p) =>
        val intervalKey = DateManipulation.toIntervalKey(p.getTimestamp, intervalMinutes)
        val intervalPoints = map.getOrElse(intervalKey, Nil)
        map + (intervalKey -> (p :: intervalPoints))
    }
    val intervalData = datapointsByInterval.mapValues { datapoints =>
      val values = datapoints.map(p => statistic.getValue(p))
      val mean = values.sum / datapoints.size
      val variance = datapoints.map { p =>
        val deviation = statistic.getValue(p) - mean
        deviation * deviation
      }.sum / datapoints.size
      val stdDev = Math.sqrt(variance)
      val coeffVariation = stdDev / mean
      IntervalDatum(mean, coeffVariation, values.min, values.max)
    }
    Baseline(intervalData)
  }

}
