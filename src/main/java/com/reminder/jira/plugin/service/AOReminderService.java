package com.reminder.jira.plugin.service;

import com.atlassian.activeobjects.tx.Transactional;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.reminder.jira.plugin.model.Reminder;

import java.util.Collection;
import java.util.Date;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface AOReminderService {

   @Transactional
   Collection<Reminder> getAllReminders();
   @Transactional
   Collection<Reminder> getReminders(@Nullable ApplicationUser var1);

   @Transactional
   Collection<Reminder> getReminders(@Nullable ApplicationUser var1, @Nullable Issue var2);

   Collection<Reminder> getRemindersToSend();

   Reminder getReminder(@Nonnull ApplicationUser var1, @Nonnull Issue var2);

   Reminder getReminder(@Nonnull ApplicationUser var1, @Nonnull Issue var2, int var3);

   Reminder getReminder(@Nonnull ApplicationUser var1, int var2);

   Reminder[] getAllRemindersForIssue(@Nonnull ApplicationUser var1, @Nonnull Issue var2);

   @Transactional
   void setReminder(
      @Nonnull ApplicationUser var1,
      @Nonnull Issue var2,
      Date var3,
      String var4,
      String var5,
      Integer var6,
      String var7,
      String var8,
      Boolean var9,
      String var10
   );

   @Transactional
   void deleteReminder(ApplicationUser var1, Issue var2, Integer var3);

   void deleteReminder(ApplicationUser var1, Integer var2);

   void deleteAllReminders(ApplicationUser var1);

   @Transactional
   int deleteRemindersOlderThan(Date var1);
}
