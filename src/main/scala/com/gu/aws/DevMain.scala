package com.gu.aws

object DevMain extends App {

  val fakeScheduledEvent = new java.util.HashMap[String, Object]()
  new Lambda().handleRequest(fakeScheduledEvent, null)

}
