package com.reminder.jira.plugin.mail.fields;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.fields.renderer.JiraRendererPlugin;
import java.util.Map;

public class ReminderTextColumnLayoutItem extends DefaultColumnLayoutItem {
   private final Map<Issue, String> issuesWithreminderText;
   private final JiraRendererPlugin renderer;

   public ReminderTextColumnLayoutItem(Map<Issue, String> issuesWithreminderText, NavigableField reminderTextField, int position) {
      super(reminderTextField, position);
      this.issuesWithreminderText = issuesWithreminderText;
      this.renderer = ComponentAccessor.getRendererManager().getRendererForType("atlassian-wiki-renderer");
   }

   @Override
   public String getHtml(Map displayParams, Issue issue) {
      String wikiText = this.issuesWithreminderText.get(issue);
      return this.renderer.render(wikiText, issue.getIssueRenderContext());
   }
}
