/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.inovatika.sdnnt.sched;

import cz.inovatika.sdnnt.InitServlet;
import cz.inovatika.sdnnt.Options;
import cz.inovatika.sdnnt.index.Indexer;
import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.UnableToInterruptJobException;

/**
 *
 * @author alberto
 */
public class Job implements InterruptableJob {

  private static final Logger LOGGER = Logger.getLogger(Job.class.getName());
  private Thread thread;
  // JSONObject jobdata;

  public JSONObject fire(String jobName) {
      return fire(Options.getInstance().getJSONObject("jobs").getJSONObject(jobName));
  }

  public JSONObject fire(JSONObject jobData) {
    thread = Thread.currentThread();
    // LOGGER.log(Level.INFO, "fire job {0}", jobData);
    jobData.put("interrupted", false);
    String action = jobData.getString("type");  
    Actions actionToDo = Actions.valueOf(action.toUpperCase());
    JSONObject ret = actionToDo.doPerform(jobData);
    thread.interrupt();
    return ret;
  }

  @Override
  public void interrupt() throws UnableToInterruptJobException {
    thread.interrupt();
        try {
            thread.join(1000);
        } catch (InterruptedException e) {
            throw new UnableToInterruptJobException(e);
        } finally {
            // ... do cleanup
        }
  }

  @Override
  public void execute(JobExecutionContext jec) throws JobExecutionException {
    String jobName = "unnamed"; 
    try {
      JobDataMap data = jec.getMergedJobDataMap();
      jobName = (String) data.get("jobname");
      String jobKey = jec.getJobDetail().getKey().toString();
      String path = InitServlet.CONFIG_DIR + File.separator + "sched.state";
      String state = "";
      File f = new File(path);
      if (f.exists() && f.canRead()) {
        state = FileUtils.readFileToString(f, "UTF-8");
      }
      if ("paused".equals(state) || SchedulerMgr.getInstance().isPaused()) {
        LOGGER.log(Level.INFO, "Scheduler is paused. Nothing to do.", jobKey);
        return;
      }
      int i = 0;
      for (JobExecutionContext j : jec.getScheduler().getCurrentlyExecutingJobs()) {
        if (jobKey.equals(j.getJobDetail().getKey().toString())) {
          i++;
        }
      }
      if (i > 1) {
        LOGGER.log(Level.INFO, "jobKey {0} is still running. Nothing to do.", jobKey);
        return;
      }

      fire(jobName);
      LOGGER.log(Level.FINE, "job {0} finished", jobKey);
    } catch (SchedulerException | IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      LOGGER.log(Level.SEVERE, "Can't execute job " + jobName, ex);
    }
  }
  
  enum Actions {

    TEST {
      @Override
      JSONObject doPerform(JSONObject jobData) {
        JSONObject json = new JSONObject();
        try {

          Date start = new Date();
          String from = start.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DateTimeFormatter.ISO_DATE_TIME);
          json.put("from", from);
          json.put("calendar", start.toInstant().atOffset(ZoneOffset.UTC).toLocalDateTime().format(DateTimeFormatter.ISO_DATE_TIME));
          json.put("zone", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT));
        } catch (JSONException ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          json.put("error", ex.toString());
        }

        return json;
      }
    },
    // Zpracovava navhry VVS a NZN. Controluje lhuty, a meni stav zaznamu
    CHECK_STAV {
      @Override
      JSONObject doPerform(JSONObject jobData) {
        JSONObject ret = new JSONObject();
        try {
          ret = Indexer.checkStav();
          LOGGER.log(Level.INFO, "CHECK_STAV finished");
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, null, ex);
          ret.put("error", ex.toString());
        }

        return ret;
      }
    };
    abstract JSONObject doPerform(JSONObject jobData);
  }
  
}
