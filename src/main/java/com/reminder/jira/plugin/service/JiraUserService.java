package com.reminder.jira.plugin.service;

import com.atlassian.jira.timezone.TimeZoneInfo;
import com.atlassian.jira.user.ApplicationUser;

public interface JiraUserService {
   TimeZoneInfo getUserTimezone(ApplicationUser var1);
}
