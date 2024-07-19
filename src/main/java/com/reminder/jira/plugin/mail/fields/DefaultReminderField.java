package com.reminder.jira.plugin.mail.fields;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.Field;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.lucene.search.FieldComparatorSource;
import org.apache.lucene.search.SortField;

public class DefaultReminderField implements NavigableField {
   private final JiraAuthenticationContext authContext;
   private final String id;
   private final String nameKey;

   public DefaultReminderField(String id, String nameKey) {
      this.id = id;
      this.nameKey = nameKey;
      this.authContext = ComponentAccessor.getJiraAuthenticationContext();
   }

   public String getColumnHeadingKey() {
      return this.getNameKey();
   }

   public String getColumnCssClass() {
      return this.getId();
   }

   public String getDefaultSortOrder() {
      return "ASC";
   }

   public FieldComparatorSource getSortComparatorSource() {
      return null;
   }

   public List<SortField> getSortFields(boolean sortOrder) {
      return new ArrayList();
   }

   public LuceneFieldSorter getSorter() {
      return null;
   }

   public String getColumnViewHtml(FieldLayoutItem fieldLayoutItem, Map displayParams, Issue issue) {
      throw new UnsupportedOperationException("getColumnViewHtml not implemented");
   }

   public String getHiddenFieldId() {
      return this.getId();
   }

   public String prettyPrintChangeHistory(String changeHistory) {
      return changeHistory;
   }

   public String prettyPrintChangeHistory(String changeHistory, I18nHelper i18nHelper) {
      return changeHistory;
   }

   public String getId() {
      return this.id;
   }

   public String getNameKey() {
      return this.nameKey;
   }

   public String getName() {
      return this.authContext.getI18nHelper().getText(this.getNameKey());
   }

   public int compareTo(Object o) {
      if (o == null) {
         return 1;
      } else if (o instanceof Field) {
         Field field = (Field)o;
         return this.getName() == null ? (field.getName() == null ? 0 : -1) : (field.getName() == null ? 1 : this.getName().compareTo(field.getName()));
      } else {
         throw new IllegalArgumentException("Can only compare Field objects.");
      }
   }
}
