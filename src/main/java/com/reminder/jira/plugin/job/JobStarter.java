package com.reminder.jira.plugin.job;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.sal.api.lifecycle.LifecycleAware;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.JobId;
import com.atlassian.scheduler.config.JobRunnerKey;
import com.atlassian.scheduler.config.RunMode;
import com.atlassian.scheduler.config.Schedule;
import com.atlassian.scheduler.status.JobDetails;
import com.reminder.jira.plugin.service.AOReminderService;
import java.util.Date;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

public class JobStarter implements LifecycleAware, DisposableBean {
   private static final Logger LOG;
   private final AOReminderService aoReminderService;
   private final SchedulerService schedulerService;
   private final DateTimeFormatterFactory dateTimeFormatterFactory;
   private final SearchService searchService;

   public JobStarter(
      AOReminderService aoReminderService,
      SchedulerService schedulerService,
      DateTimeFormatterFactory dateTimeFormatterFactory,
      SearchService searchService
   ) {
      this.aoReminderService = aoReminderService;
      this.schedulerService = schedulerService;
      this.dateTimeFormatterFactory = dateTimeFormatterFactory;
      this.searchService = searchService;
   }

   public void onStart() {
      JobRunnerKey jobRunnerKey = JobRunnerKey.of("Reminder Job Runner");
      this.schedulerService
         .registerJobRunner(jobRunnerKey,
                 new CustomJobRunner(this.searchService, this.aoReminderService, this.dateTimeFormatterFactory));
      LOG.info("registered Reminder Job Runner");

      try {
         this.schedulerService
            .scheduleJob(
               JobId.of("Reminder Job"),
               JobConfig.forJobRunnerKey(jobRunnerKey).withRunMode(RunMode.RUN_ONCE_PER_CLUSTER).withSchedule(Schedule.forInterval(120000L, new Date()))
            );
         LOG.info("started Reminder Job");
      } catch (SchedulerServiceException var11) {
         this.schedulerService.unregisterJobRunner(jobRunnerKey);
         LOG.info("scheduleJob failed", var11);
      } finally {
         JobRunnerKey unwantedJobRunnerKey = JobRunnerKey.of("com.jira.plugins.service.impl.AOReminderServiceImpl");

         for(JobDetails jobDetails : this.schedulerService.getJobsByJobRunnerKey(unwantedJobRunnerKey)) {
            this.schedulerService.unscheduleJob(jobDetails.getJobId());
            LOG.debug("removed old job '" + jobDetails.getJobId() + "'");
         }

         this.schedulerService.unregisterJobRunner(unwantedJobRunnerKey);
      }
   }

   public void onStop() {
      try {
         this.destroy();
      } catch (Exception var2) {
         LOG.error("Error stopping the job.");
      }
   }

   public void destroy() throws Exception {
      try {
         this.schedulerService.unscheduleJob(JobId.of("Reminder Job"));
         this.schedulerService.unregisterJobRunner(JobRunnerKey.of("Reminder Job Runner"));
         LOG.info("stopped Reminder Job");
      } catch (IllegalArgumentException var2) {
         LOG.error("failed to stop Reminder Job");
      }
   }

   static {
      (LOG = Logger.getLogger(JobStarter.class)).setLevel(Level.INFO);
   }
}
