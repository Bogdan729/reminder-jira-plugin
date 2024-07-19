package com.reminder.jira.plugin.mail.fields;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.fields.layout.column.ColumnLayoutItem;
import com.atlassian.jira.user.ApplicationUser;
import java.util.Map;

public class DefaultColumnLayoutItem implements ColumnLayoutItem {
   private final NavigableField field;
   private final int position;

   public DefaultColumnLayoutItem(NavigableField field, int position) {
      this.field = field;
      this.position = position;
   }

   public NavigableField getNavigableField() {
      return this.field;
   }

   public String getId() {
      return this.getNavigableField().getId();
   }

   public boolean isAliasForField(ApplicationUser user, String sortField) {
      return false;
   }

   public int getPosition() {
      return this.position;
   }

   public String getHtml(Map displayParams, Issue issue) {
      return this.getNavigableField().getColumnViewHtml(null, displayParams, issue);
   }

   public String getColumnHeadingKey() {
      return this.getNavigableField().getColumnHeadingKey();
   }

   public int compareTo(Object o) {
      if (o instanceof ColumnLayoutItem) {
         return this.getPosition() - ((ColumnLayoutItem)o).getPosition();
      } else {
         throw new IllegalArgumentException(o + " is not of type ColumnLayoutItem.");
      }
   }
}
