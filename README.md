# baseline-cloudwatch-alarm

An expiriment into using a Lambda to provide a baseline-aware CloudWatch alarm.

The problem with CloudWatch alarms is that they have no concept of a baseline. You can tell CloudWatch to alert you when a given metric drops below a certain value, but that's not very useful when the metric's value fluctuates throughout the day according to changes in your site's traffic or user behaviour.

The idea is to keep using CloudWatch metrics but replace the alarm functionality with a Lambda. This Lambda understands what value the metric should have on a normal day, so it can alert you more accurately to anomalies.

## How to use

1. Run `sbt generateLambdaInput` with appropriate arguments to calculate the baseline data. This uses the last 2 weeks of data (unfortunately that's all you get with CloudWatch) to calculate the baseline. Run the argument with no arguments to see detailed usage notes. e.g.:

    ```
    # Generates baseline data for the number of records written to a Kinesis stream in 15 minute intervals
    > generateLambdaInput "AWS/Kinesis" "IncomingRecords" "StreamName" "my-kinesis-stream" 15 Sum
    ```

2. Run `sbt assembly` to build your Lambda. Upload it to AWS and configure it to run as a scheduled task. If you want to be alerted to an anomaly within `N` minutes, you should schedule your Lambda to run at least every `N` minutes.

3. Periodically re-run `sbt generateLambdaInput` to update the baseline data, as the normal values will change over time. You will need to rebuild and re-upload your Lambda whenever you update the baseline data.
