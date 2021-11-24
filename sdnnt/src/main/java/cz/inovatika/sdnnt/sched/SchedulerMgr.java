/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.sched;

import cz.inovatika.sdnnt.Options;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.TriggerBuilder;
import org.quartz.core.jmx.JobDataMapSupport;
import org.quartz.impl.StdSchedulerFactory;

/**
 *
 * @author alberto
 */
public class SchedulerMgr {
  
  static final Logger LOGGER = Logger.getLogger(SchedulerMgr.class.getName());
  private static SchedulerMgr _sharedInstance = null;
  private org.quartz.Scheduler scheduler;
  boolean paused;
  boolean indexerPaused;

  public synchronized static SchedulerMgr getInstance() {
    if (_sharedInstance == null) {
      _sharedInstance = new SchedulerMgr();
    }
    return _sharedInstance;
  }

  public SchedulerMgr() {
    try {
      Properties quartzProperties = new Properties();     
      quartzProperties.put("org.quartz.threadPool.threadCount", "10");
      SchedulerFactory sf = new StdSchedulerFactory(quartzProperties); 
      scheduler = sf.getScheduler();
    } catch (SchedulerException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  /**
   * @return the scheduler
   */
  public org.quartz.Scheduler getScheduler() {
    return scheduler;
  }
  
  public static void initJobs() throws SchedulerException {
    try {
      JSONObject jobs = Options.getInstance().getJSONObject("jobs");
      for (Object key : jobs.keySet()) {
        addJob((String) key, jobs.getJSONObject((String) key));
      }
      
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }

  }



  public static JobDataMap setData(String jobName, JSONObject jobdata) throws Exception {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("jobname", jobName);
    map.put("jobdata", jobdata);
    return JobDataMapSupport.newJobDataMap(map);
  }

  public static void addJob(String jobName, JSONObject js) throws Exception {
    org.quartz.Scheduler sched = SchedulerMgr.getInstance().getScheduler();

    JobDataMap data = setData(jobName, js);
    JobDetail job = JobBuilder.newJob(Job.class)
            .withIdentity(jobName)
            .setJobData(data)
            .build();
    if (sched.checkExists(job.getKey())) {
      sched.deleteJob(job.getKey());
    }
    String cronVal = js.optString("cron", ""); 
    if (cronVal.equals("")) {
      sched.addJob(job, true, true);
      LOGGER.log(Level.INFO, "Job {0} added to scheduler", jobName);

    } else {
      CronTrigger trigger = TriggerBuilder.newTrigger()
              .withIdentity("trigger_" + jobName)
              .withSchedule(CronScheduleBuilder.cronSchedule(cronVal))
              .build();
      sched.scheduleJob(job, trigger);
      LOGGER.log(Level.INFO, "Job {0} added to scheduler with {1}", new Object[]{jobName, cronVal});
    }
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  public boolean isPaused() {
    return this.paused;
  }

  public void setIndexerPaused(boolean paused) {
    this.indexerPaused = paused;
  }

  public boolean isIndexerPaused() {
    return this.indexerPaused;
  }
  
}
