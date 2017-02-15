# Hadoop xargs

This software provides possibility to utilize resources of 
Hadoop cluster to run ordinal Linux processes
 
### Building
#### Prerequisites
1. Java 1.8
2. Maven

#### Building uber-jar
This build will include all the dependencies (including Spark), 
and give you huge single JAR file

```bash
mvn clean install
```

During the build, tests run will be performed. 
After run is complete, you will have jar at `target/hadoop-xargs-1.0.jar`.

You can run it with:
```bash
java -jar target/hadoop-xargs-1.0.jar $CONFIG_PATH
```

#### Building JAR to run with Spark in cluster
Building:

```bash
mvn clean install -Dwork.scope=provided
```
You will get the same jar, but it will be much more thin.
To run it with Spark on cluster, do:
```bash
/local/spark/bin/spark-submit hadoop-xargs-1.0.jar $CONFIG_PATH
```

### Running

For appropriate work, Hadoop xargs need two things:
1. List of commands, passed to STDIN, one per-line, ending with empty line
2. (optionaly) Config file

Example:
```bash
user@cloududs1.mlan:/local/tmp/uds_local> echo '/bin/sleep 2' | /local/spark/bin/spark-submit  --conf "spark.driver.host=10.10.224.14" hadoop-xargs-1.0.jar sample.properties 
17/02/02 07:03:38 INFO Application: Starting application
17/02/02 07:03:38 INFO Main: Expect commands to be passed to STDIN, one per line
17/02/02 07:03:38 INFO Application: Got 1 jobs:
17/02/02 07:03:38 INFO Application: /bin/sleep 2
17/02/02 07:03:38 INFO Application: Application name: Xargs sample
17/02/02 07:03:38 INFO Application: Execution environment: yarn-client
17/02/02 07:03:38 INFO Application: Explicit executor count was not specified, making same as job count
17/02/02 07:03:38 INFO Application: Initializing Spark
17/02/02 07:03:47 INFO Application: Initialization completed, starting jobs
17/02/02 07:03:50 INFO Application: Command '/bin/sleep 2' finished on host bihadoop67.mlan
17/02/02 07:03:50 INFO Application: All the jobs completed in 0:00:03.262
```
### Config file explanation
You can find sample config file at [conf/sample.properties](conf/sample.properties):

```
# Sample properties file for Hadoop xargs on YARN

# MANDATORY SECTION START
# Where to run (local, yarn-client)
spark.master=yarn-client
# Name of the application
spark.app.name=Xargs sample
# MANDATORY SECTION END

# Behaviour settings
# Do not show progress bar (actually, we do it by our own)
spark.ui.showConsoleProgress=false
# If this is greater than one, than re-tries of task will be performed
spark.task.maxFailures=1

# Environment settings
spark.yarn.queue=default
# How many physical JVM should we spawn, uncomment to force
#spark.executor.instances=10
# How many processes per one JVM to spawn
#spark.executor.cores=1
# Memory consumption for one JVM worker (should be small, since no JAVA code executed)
spark.executor.memory=2G
```

Mandatory options are:
* `spark.master` - in what env do the app run (local[*] - all cores on localhost, yarn-client - use cluster)
* `spark.app.name` - the name of the application to be used in YARN

Optional settings:
* `spark.ui.showConsoleProgress` - show Spark's progress bar (Hadoop xargs also writes info about finished jobs)
* `spark.task.maxFailures` - how many retries for one task should be performed before failing the whole application
* `spark.yarn.queue` - which YARN queue to use for container allocation
* `spark.executor.instances` - how many containers should we ask YARN to allocate. If not provided, we assume one container per one xargs job
* `spark.executor.cores` - how many processes to spawn per one executor (works only with `spark.executor.instances` given)
* `spark.executor.memory` - size of one container heap. Since we are running jobs not in JVM, should be small
* `spark.driver.host` - IP address of machine, where job is submitted (<span style="color:red;font-weight:bold">IS MANDATORY FOR HOSTS WITH 2 ETH INTERFACES</span>)

When submitting job via spark-submit, you may pass config variables as shown below:
```bash
cat commands.list | /local/spark/bin/spark-submit --conf "spark.yarn.queue=my.queue" --conf "spark.app.name=My application" --conf "spark.master=yarn-client"  hadoop-xargs-1.0.jar  
```

### Example of launch

Let's assume, we have such file with commands, and `spark.task.maxFailures=3`:
```
/bin/sleep 1
/bin/sleep 1
/bin/sleesdfsdfp 1
/bin/sleep 1
```
Launch:
```
akrasheninnikov@cloududs1.mlan:/local/tmp/uds_local> cat commands.list | /local/spark/bin/spark-submit  --conf "spark.driver.host=$my_ip"  hadoop-xargs-1.0.jar sample.properties 
17/02/02 07:16:58 INFO Application: Starting application
17/02/02 07:16:58 INFO Main: Expect commands to be passed to STDIN, one per line
17/02/02 07:16:58 INFO Application: Got 4 jobs:
17/02/02 07:16:58 INFO Application: /bin/sleep 1
17/02/02 07:16:58 INFO Application: /bin/sleep 1
17/02/02 07:16:58 INFO Application: /bin/sleesdfsdfp 1
17/02/02 07:16:58 INFO Application: /bin/sleep 1
17/02/02 07:16:58 INFO Application: Application name: Sample application
17/02/02 07:16:58 INFO Application: Execution environment: yarn-client
17/02/02 07:16:58 INFO Application: Explicit executor count was not specified, making same as job count
17/02/02 07:16:58 INFO Application: Initializing Spark
17/02/02 07:17:12 INFO Application: Initialization completed, starting jobs
17/02/02 07:17:13 ERROR Application: Command '/bin/sleesdfsdfp 1' failed on host bihadoop6.mlan, 1 times
17/02/02 07:17:13 ERROR Application: Command '/bin/sleesdfsdfp 1' failed on host bihadoop6.mlan, 2 times
17/02/02 07:17:13 ERROR Application: Command '/bin/sleesdfsdfp 1' failed on host bihadoop6.mlan, 3 times
17/02/02 07:17:13 ERROR Application: Command '/bin/sleep 1' failed on host bihadoop21.mlan, 1 times
17/02/02 07:17:14 ERROR Main: FATAL ERROR: Failed to execute all the jobs
java.lang.InstantiationException: Cannot run program "/bin/sleesdfsdfp": error=2, No such file or directory
	at com.badoo.bi.hadoop.xargs.JobExecutor.call(JobExecutor.java:52)
	at com.badoo.bi.hadoop.xargs.JobExecutor.call(JobExecutor.java:15)
	at org.apache.spark.api.java.JavaRDDLike$$anonfun$foreachAsync$1.apply(JavaRDDLike.scala:690)
	at org.apache.spark.api.java.JavaRDDLike$$anonfun$foreachAsync$1.apply(JavaRDDLike.scala:690)
	at scala.collection.Iterator$class.foreach(Iterator.scala:727)
	at org.apache.spark.InterruptibleIterator.foreach(InterruptibleIterator.scala:28)
	at org.apache.spark.rdd.AsyncRDDActions$$anonfun$foreachAsync$1$$anonfun$apply$15.apply(AsyncRDDActions.scala:118)
	at org.apache.spark.rdd.AsyncRDDActions$$anonfun$foreachAsync$1$$anonfun$apply$15.apply(AsyncRDDActions.scala:118)
	at org.apache.spark.SparkContext$$anonfun$37.apply(SparkContext.scala:1984)
	at org.apache.spark.SparkContext$$anonfun$37.apply(SparkContext.scala:1984)
	at org.apache.spark.scheduler.ResultTask.runTask(ResultTask.scala:66)
	at org.apache.spark.scheduler.Task.run(Task.scala:88)
	at org.apache.spark.executor.Executor$TaskRunner.run(Executor.scala:214)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
	at java.lang.Thread.run(Thread.java:745)
``` 
