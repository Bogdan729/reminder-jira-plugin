package com.reminder.jira.plugin.service.impl;

import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.timezone.TimeZoneInfo;
import com.atlassian.jira.timezone.TimeZoneService;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.reminder.jira.plugin.service.JiraUserService;

public class JiraUserServiceImpl implements JiraUserService {
   private final TimeZoneService timeZoneService;

   public JiraUserServiceImpl(TimeZoneService timeZoneService) {
      this.timeZoneService = timeZoneService;
   }

   @Override
   public TimeZoneInfo getUserTimezone(ApplicationUser user) {
      JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(user, new SimpleErrorCollection());
      return this.timeZoneService.getUserTimeZoneInfo(jiraServiceContext);
   }
}
