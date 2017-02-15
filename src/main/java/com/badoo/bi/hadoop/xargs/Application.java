package com.badoo.bi.hadoop.xargs;

import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.hadoop.util.ThreadUtil;
import org.apache.spark.JavaSparkListener;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaFutureAction;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.scheduler.SparkListenerTaskEnd;
import org.apache.spark.scheduler.TaskInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A controlling application
 * Created by krash on 01.02.17.
 */
@Log4j
public class Application {

    private final Properties properties;
    private final Supplier<Collection<String>> jobProvider;

    public Application(Properties properties, Supplier<Collection<String>> jobProvider) {
        this.properties = properties;
        this.jobProvider = jobProvider;
    }

    private SparkConf createConfig() {
        SparkConf conf = new SparkConf();
        this.properties.forEach((key, value) -> conf.set(String.valueOf(key), String.valueOf(value)));
        return conf;
    }

    public void run() throws ExecutionException {
        log.info("Starting application");
        Collection<String> jobList = jobProvider.get();
        if (jobList.isEmpty()) {
            throw new IllegalArgumentException("Got empty job list");
        }

        log.info("Got " + jobList.size() + " jobs:");
        jobList.forEach(log::info);

        SparkConf conf = createConfig();
        log.info("Application name: " + conf.get("spark.app.name"));
        log.info("Execution environment: " + conf.get("spark.master"));

        // in case, we did not specified any preferences of containers, will use job count
        if (!conf.contains("spark.executor.instances")) {
            log.info("Explicit executor count was not specified, making same as job count");
            conf.set("spark.executor.instances", String.valueOf(jobList.size()));
            conf.remove("spark.executor.cores");
        }

        runJobs(conf, jobList);
    }

    private void runJobs(SparkConf conf, Collection<String> jobsCollection) throws ExecutionException {
        List<String> jobs = jobsCollection.stream().collect(Collectors.toList());
        log.info("Initializing Spark");
        JavaSparkContext context = new JavaSparkContext(conf);
        log.info("Initialization completed, starting jobs");
        long pureTimeStart = System.currentTimeMillis();
        JavaRDD<String> rdd = context.parallelize(jobs, jobs.size());
        JobExecutor jobExecutor = new JobExecutor();
        jobExecutor.setLoggingReConfig("true".equals(System.getProperty("SPARK_SUBMIT")));
        context.sc().setJobDescription("Invoking commands");

        Listener listener = new Listener();
        for (int i = 0; i < jobs.size(); i++) {
            listener.map.put(i, jobs.get(i));
        }

        context.sc().addSparkListener(listener);
        JavaFutureAction<Void> future = rdd.foreachAsync(jobExecutor);
        while (!future.isDone()) {
            ThreadUtil.sleepAtLeastIgnoreInterrupts(1_000L);
        }
        try {
            future.get();
        } catch (Exception err) {

            // try to find original exception from remote executor
            Throwable custom = err;
            while (null != (custom = custom.getCause())) {
                if (custom instanceof InstantiationException) {
                    throw new ExecutionException("Failed to execute all the jobs", custom);
                }
            }
            throw new ExecutionException("Failed to execute all the jobs", err);
        }
        long end = System.currentTimeMillis() - pureTimeStart;

        log.info("All the jobs completed in " + DurationFormatUtils.formatDurationHMS(end));
    }

    private static class Listener extends JavaSparkListener {

        public Map<Integer, String> map = new HashMap<>();

        @Override
        public void onTaskEnd(SparkListenerTaskEnd taskEnd) {
            TaskInfo info = taskEnd.taskInfo();

            String command = map.getOrDefault(info.index(), "UNKNOWN COMMAND");

            String state = info.successful() ? "finished" : "failed";
            String message = "Command '" + command + "' " + state + " on host " + info.host();
            if (info.failed()) {
                message += ", " + (info.attemptNumber() + 1) + " times";
                log.error(message);
            } else {
                log.info(message);
            }
        }
    }
}
