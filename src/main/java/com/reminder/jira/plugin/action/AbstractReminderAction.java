package com.reminder.jira.plugin.action;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.jira.web.action.util.CalendarResourceIncluder;
import com.atlassian.jira.web.action.util.FieldsResourceIncluder;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractReminderAction extends JiraWebActionSupport {

   public CalendarResourceIncluder getCalendarIncluder() {
      return new CalendarResourceIncluder();
   }

   public FieldsResourceIncluder getFieldsResourceIncluder() {
      return (FieldsResourceIncluder)ComponentAccessor.getComponentOfType(FieldsResourceIncluder.class);
   }

   public Map<String, String> getDisplayParameters() {
      Map<String, String> params = new HashMap();
      params.put("theme", "aui");
      return params;
   }
}
