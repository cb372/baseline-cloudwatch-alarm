package com.gu.aws

import java.util.Date

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import scala.annotation.tailrec

object DateManipulation {

  val IntervalKeyFormat = DateTimeFormat.forPattern("EEEE HH:mm")

  def roundToStartOfInterval(timestamp: DateTime, intervalMinutes: Int): DateTime = {
    @tailrec
    def rewind(d: DateTime): DateTime = {
      if (d.getMinuteOfHour % intervalMinutes == 0)
        d
      else
        rewind(d.minusMinutes(1))
    }
    rewind(timestamp.withMillisOfSecond(0).withSecondOfMinute(0))
  }

  def toIntervalKey(timestamp: DateTime, intervalMinutes: Int): String =
    roundToStartOfInterval(timestamp, intervalMinutes).toString(IntervalKeyFormat)

  def toIntervalKey(timestamp: Date, intervalMinutes: Int): String =
    toIntervalKey(new DateTime(timestamp), intervalMinutes)

}
