package com.reminder.jira.plugin.model;

import java.util.Date;
import net.java.ao.Accessor;
import net.java.ao.Entity;
import net.java.ao.Mutator;
import net.java.ao.schema.StringLength;

public interface Reminder extends Entity {
   String getCondition();

   void setCondition(String var1);

   String getAdditionalRecipients();

   void setAdditionalRecipients(String var1);

   String getUserKey();

   void setUserKey(String var1);

   long getIssueId();

   void setIssueId(long var1);

   String getIssueKey();

   void setIssueKey(String var1);

   boolean getPublicReminder();

   void setPublicReminder(boolean var1);

   String getGroups();

   void setGroups(String var1);

   @Accessor("WAS_SENT")
   Boolean getWasSent();

   @Mutator("WAS_SENT")
   void setWasSent(Boolean var1);

   @Accessor("REMINDER_DATE")
   Date getDate();

   @Mutator("REMINDER_DATE")
   void setDate(Date var1);

   @Mutator("TIME")
   void setTime(String var1);

   @Mutator("TIME")
   String getTime();

   @StringLength(-1)
   String getText();

   void setText(String var1);
}
