package com.reminder.jira.plugin.service.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.google.common.base.Preconditions;
import com.reminder.jira.plugin.model.Reminder;
import com.reminder.jira.plugin.service.AOReminderService;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.java.ao.DBParam;
import net.java.ao.RawEntity;
import org.apache.commons.lang.StringUtils;

public class AOReminderServiceImpl implements AOReminderService {
   private final ActiveObjects ao;

   public AOReminderServiceImpl(ActiveObjects ao) {
      this.ao = (ActiveObjects)Preconditions.checkNotNull(ao);
   }

   @Override
   public Collection<Reminder> getAllReminders() {
      Reminder[] remindersFromDb = this.ao.find(Reminder.class);
      return Arrays.asList(remindersFromDb);
   }

   @Override
   public Collection<Reminder> getReminders(@Nullable ApplicationUser user) {
      return (Collection<Reminder>)(user == null ? Collections.emptyList() : this.getReminders(user, null));
   }

   @Override
   public Collection<Reminder> getReminders(@Nullable ApplicationUser user, @Nullable Issue issue) {
      Reminder[] remindersFromDb;
      if (user != null && issue != null) {
         remindersFromDb = (Reminder[])this.ao.find(Reminder.class, "USER_KEY = ? AND ISSUE_ID = ?", new Object[]{user.getKey(), issue.getId()});
      } else if (user != null) {
         remindersFromDb = (Reminder[])this.ao.find(Reminder.class, "USER_KEY = ?", new Object[]{user.getKey()});
      } else if (issue != null) {
         remindersFromDb = (Reminder[])this.ao.find(Reminder.class, "ISSUE_ID = ?", new Object[]{issue.getId()});
      } else {
         remindersFromDb = (Reminder[])this.ao.find(Reminder.class);
      }

      return Arrays.asList(remindersFromDb);
   }

   @Override
   public Collection<Reminder> getRemindersToSend() {
      Date date = new Date();
      Reminder[] remindersFromDb = (Reminder[])this.ao.find(Reminder.class, "WAS_SENT = ? AND REMINDER_DATE <= ?", new Object[]{Boolean.FALSE, date});
      return Arrays.asList(remindersFromDb);
   }

   @Override
   public Reminder getReminder(@Nonnull ApplicationUser user, @Nonnull Issue issue) {
      Reminder[] remindersFromDb = (Reminder[])this.ao.find(Reminder.class, "USER_KEY = ? AND ISSUE_ID = ?", new Object[]{user.getKey(), issue.getId()});
      return remindersFromDb.length > 0 ? remindersFromDb[0] : null;
   }

   @Override
   public Reminder getReminder(@Nonnull ApplicationUser user, @Nonnull Issue issue, int reminderId) {
      Reminder[] remindersFromDb = (Reminder[])this.ao
         .find(Reminder.class, "USER_KEY = ? AND ISSUE_ID = ? AND ID = ?", new Object[]{user.getKey(), issue.getId(), reminderId});
      return remindersFromDb.length > 0 ? remindersFromDb[0] : null;
   }

   @Override
   public Reminder getReminder(@Nonnull ApplicationUser user, int reminderId) {
      Reminder[] remindersFromDb = (Reminder[])this.ao.find(Reminder.class, "USER_KEY = ? AND ID = ?", new Object[]{user.getKey(), reminderId});
      return remindersFromDb.length > 0 ? remindersFromDb[0] : null;
   }

   @Override
   public Reminder[] getAllRemindersForIssue(@Nonnull ApplicationUser user, @Nonnull Issue issue) {
      return (Reminder[])this.ao.find(Reminder.class, "ISSUE_ID = ?", new Object[]{issue.getId()});
   }

   @Override
   public void setReminder(
      @Nonnull ApplicationUser user,
      @Nonnull Issue issue,
      Date date,
      String text,
      String time,
      Integer reminderId,
      String additionalRecipients,
      String condition,
      Boolean privateReminder,
      String groups
   ) {
      Reminder reminder;
      if (reminderId == null) {
         reminder = (Reminder)this.ao.create(Reminder.class, new DBParam[0]);
         reminder.setUserKey(user.getKey());
         reminder.setIssueId(issue.getId());
         reminder.setIssueKey(issue.getKey());
         reminder.setWasSent(Boolean.FALSE);
      } else {
         reminder = this.getReminder(user, issue, reminderId);
         if (date.getTime() > new Date().getTime()) {
            reminder.setWasSent(Boolean.FALSE);
         }
      }

      if (StringUtils.isNotEmpty(groups)) {
         reminder.setGroups(groups);
      }

      reminder.setPublicReminder(!privateReminder);
      reminder.setDate(date);
      reminder.setTime(time);
      reminder.setText(text);
      reminder.setCondition(condition);
      reminder.setAdditionalRecipients(additionalRecipients);
      reminder.save();
   }

   @Override
   public void deleteReminder(ApplicationUser user, Issue issue, Integer id) {
      Reminder reminder = this.getReminder(user, issue, id);
      if (reminder != null) {
         this.ao.delete(new RawEntity[]{reminder});
      }
   }

   @Override
   public void deleteReminder(ApplicationUser user, Integer id) {
      Reminder reminder = this.getReminder(user, id);
      if (reminder != null) {
         this.ao.delete(new RawEntity[]{reminder});
      }
   }

   @Override
   public void deleteAllReminders(ApplicationUser user) {
      Collection<Reminder> reminders = this.getReminders(user);
      reminders.forEach(reminder -> this.ao.delete(new RawEntity[]{reminder}));
   }

   @Override
   public int deleteRemindersOlderThan(Date date) {
      Calendar cutOffDate = Calendar.getInstance();
      cutOffDate.setTime(date);
      cutOffDate.set(11, 0);
      cutOffDate.set(12, 0);
      cutOffDate.set(13, 0);
      cutOffDate.set(14, 0);
      int deletedCount = this.ao.deleteWithSQL(Reminder.class, "REMINDER_DATE < ?", new Object[]{cutOffDate.getTime()});
      this.ao.flushAll();
      return deletedCount;
   }
}
