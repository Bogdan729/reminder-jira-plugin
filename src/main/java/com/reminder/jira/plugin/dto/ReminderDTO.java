package com.reminder.jira.plugin.dto;

import java.util.Date;

public class ReminderDTO {
   int id;
   private Date reminderdate;
   private String reminderText;
   private String reminderdateIso8601;
   private String reminderdateHtml;
   private String reminderTextHtml;
   private String condition;
   private Long issueId;
   private String issueKey;
   private String createDisplayName;
   private boolean allowEdit;
   private String groups;
   private String additionalRecipients;

   public void setAdditionalRecipients(String additionalRecipients) {
      this.additionalRecipients = additionalRecipients;
   }

   public String getAdditionalRecipients() {
      return this.additionalRecipients;
   }

   public void setGroups(String groups) {
      this.groups = groups;
   }
   public String getGroups() {
      return this.groups;
   }

   public boolean isAllowEdit() {
      return this.allowEdit;
   }

   public void setAllowEdit(boolean allowEdit) {
      this.allowEdit = allowEdit;
   }

   public String getCreateDisplayName() {
      return this.createDisplayName;
   }

   public void setCreateDisplayName(String createDisplayName) {
      this.createDisplayName = createDisplayName;
   }

   public Long getIssueId() {
      return this.issueId;
   }

   public void setIssueId(Long issueId) {
      this.issueId = issueId;
   }

   public String getIssueKey() {
      return this.issueKey;
   }

   public void setIssueKey(String issueKey) {
      this.issueKey = issueKey;
   }

   public String getCondition() {
      return this.condition;
   }

   public void setCondition(String condition) {
      this.condition = condition;
   }

   public int getId() {
      return this.id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public Date getReminderdate() {
      return this.reminderdate;
   }

   public void setReminderdate(Date reminderdate) {
      this.reminderdate = reminderdate;
   }

   public String getreminderText() {
      return this.reminderText;
   }

   public void setreminderText(String reminderText) {
      this.reminderText = reminderText;
   }

   public String getreminderdateIso8601() {
      return this.reminderdateIso8601;
   }

   public void setreminderdateIso8601(String reminderdateIso8601) {
      this.reminderdateIso8601 = reminderdateIso8601;
   }

   public String getreminderdateHtml() {
      return this.reminderdateHtml;
   }

   public void setreminderdateHtml(String reminderdateHtml) {
      this.reminderdateHtml = reminderdateHtml;
   }

   public String getreminderTextHtml() {
      return this.reminderTextHtml;
   }

   public void setreminderTextHtml(String reminderTextHtml) {
      this.reminderTextHtml = reminderTextHtml;
   }
}
