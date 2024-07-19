package com.reminder.jira.plugin.job;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.bc.issue.search.SearchService.ParseResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.DateFieldFormat;
import com.atlassian.query.Query;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import com.reminder.jira.plugin.mail.JiraNativeMailBuilder;
import com.reminder.jira.plugin.model.Reminder;
import com.reminder.jira.plugin.service.AOReminderService;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.velocity.exception.VelocityException;

public class CustomJobRunner implements JobRunner {
   private static final Logger LOG;
   private final AOReminderService aoReminderService;
   private final IssueManager issueManager;
   private final UserManager userManager;
   private final DateTimeFormatterFactory dateTimeFormatterFactory;
   private final SearchService searchService;
   private final GroupManager groupManager;

   public CustomJobRunner(
      SearchService searchService, AOReminderService aoReminderService, DateTimeFormatterFactory dateTimeFormatterFactory
   ) {
      this.aoReminderService = aoReminderService;
      this.searchService = searchService;
      this.dateTimeFormatterFactory = dateTimeFormatterFactory;
      this.issueManager = ComponentAccessor.getIssueManager();
      this.userManager = ComponentAccessor.getUserManager();
      this.groupManager = ComponentAccessor.getGroupManager();
   }

   public Map<String, Object> sendReminders(Date date) {
      Map<ApplicationUser, HashMap<Issue, String>> remindersMap = new HashMap<>();
      Collection<Reminder> reminders = this.aoReminderService.getRemindersToSend();

      for(Reminder reminder : reminders) {
         Set<ApplicationUser> recipients = new HashSet();
         ApplicationUser creator = this.userManager.getUserByKey(reminder.getUserKey());
         recipients.add(creator);
         if (reminder.getAdditionalRecipients() != null) {
            Arrays.stream(reminder.getAdditionalRecipients().split(",")).map(this.userManager::getUserByKey).forEach(recipients::add);
         }

         if (StringUtils.isNotEmpty(reminder.getGroups())) {
            try {
               Arrays.stream(reminder.getGroups().split(";")).map(this.groupManager::getUsersInGroup).forEach(recipients::addAll);
            } catch (Exception var10) {
               LOG.error("Cannot retrieve users from group");
            }
         }

         Issue issue = this.issueManager.getIssueObject(reminder.getIssueId());
         if (this.checkCond(reminder.getCondition(), issue, creator)) {
            recipients.forEach(recipient -> {
               if (recipient != null && issue != null) {
                  if (!remindersMap.containsKey(recipient)) {
                     remindersMap.put(recipient, new HashMap<>());
                  }

                  remindersMap.get(recipient).put(issue, reminder.getText());
               }
            });
         }

         reminder.setWasSent(Boolean.TRUE);
         reminder.save();
      }

      Integer mailsSent = 0;
      JiraNativeMailBuilder emailBuilder = new JiraNativeMailBuilder(this.dateTimeFormatterFactory);

      for(Entry<ApplicationUser, HashMap<Issue, String>> entry : remindersMap.entrySet()) {
         try {
            emailBuilder.buildAndSendMail(date, (ApplicationUser)entry.getKey(), entry.getValue());
            mailsSent = mailsSent + 1;
         } catch (VelocityException var9) {
            LOG.error(var9.getMessage());
            var9.printStackTrace();
         }
      }

      Map<String, Object> stats = new HashMap<>();
      stats.put("mailCount", mailsSent);
      stats.put("userCount", remindersMap.size());
      stats.put("reminderCount", reminders.size());
      LOG.debug(
         ComponentAccessor.getI18nHelperFactory()
            .getInstance(Locale.ENGLISH)
            .getText(
               "reminder.jira.admin.reminders.success",
               ((DateFieldFormat)ComponentAccessor.getComponentOfType(DateFieldFormat.class)).format(date),
               mailsSent,
               remindersMap.size(),
               reminders.size()
            )
      );
      return stats;
   }

   private boolean checkCond(String cond, Issue issue, ApplicationUser muser) {
      if (org.apache.commons.lang3.StringUtils.isEmpty(cond)) {
         return true;
      } else {
         ParseResult parseResult = this.searchService.parseQuery(muser, " ( " + cond + " ) and key = " + issue.getKey());
         if (parseResult.isValid()) {
            try {
               Query mainQuery = parseResult.getQuery();
               long results = this.searchService.searchCount(muser, mainQuery);
               return results > 0L;
            } catch (Exception var8) {
               return true;
            }
         } else {
            return true;
         }
      }
   }

   private int deleteOldReminders(AOReminderService aoReminderService) {
      Calendar cutOffDate = Calendar.getInstance();
      cutOffDate.add(5, -30);
      int deleteCount = aoReminderService.deleteRemindersOlderThan(cutOffDate.getTime());
      LOG.debug(
         "Deleted "
            + deleteCount
            + " Reminders older than "
            + ((DateFieldFormat)ComponentAccessor.getComponentOfType(DateFieldFormat.class)).format(cutOffDate.getTime())
            + "."
      );
      return deleteCount;
   }

   @Nullable
   public JobRunnerResponse runJob(JobRunnerRequest jobRunnerRequest) {
      LOG.info("running Reminder Job");

      this.sendReminders(new Date());
      this.deleteOldReminders(this.aoReminderService);

      return null;
   }

   static {
      (LOG = Logger.getLogger(CustomJobRunner.class)).setLevel(Level.INFO);
   }
}
