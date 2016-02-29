scalaVersion := "2.11.7"

val awsSdkVersion = "1.10.56"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-java-sdk-cloudwatch" % awsSdkVersion,
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "joda-time" % "joda-time" % "2.9.2",
  "com.github.scopt" %% "scopt" % "3.4.0"
)

lazy val generateLambdaInput = inputKey[Unit]("Generate the input data for the Lambda")

generateLambdaInput := (runMain in Compile).partialInput(" com.gu.aws.LambdaInputGenerator").evaluated

managedSources in Compile += sourceManaged.value / "lambda" / "LambdaInput.scala"

scalariformSettings
