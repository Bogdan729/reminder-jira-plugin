package com.reminder.jira.plugin.mail;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.NavigableField;
import com.atlassian.jira.issue.fields.layout.column.ColumnLayoutItem;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.mail.builder.EmailBuilder;
import com.atlassian.jira.notification.NotificationRecipient;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.VelocityParamFactory;
import com.atlassian.jira.util.collect.CompositeMap;
import com.atlassian.jira.web.component.IssueTableLayoutBean;
import com.atlassian.mail.queue.MailQueue;
import com.atlassian.mail.queue.SingleMailQueueItem;
import com.atlassian.plugin.webresource.UrlMode;
import com.opensymphony.util.TextUtils;
import com.reminder.jira.plugin.mail.fields.DefaultReminderField;
import com.reminder.jira.plugin.mail.fields.ReminderTextColumnLayoutItem;
import com.reminder.jira.plugin.utils.ResourceReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.mail.MessagingException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.exception.VelocityException;

public class JiraNativeMailBuilder {
   private static final Logger LOG = Logger.getLogger(JiraNativeMailBuilder.class);
   private static final String SUBJECT = "mail-subject.vm";
   private static final String HTML_VM_JIRA = "mail-body-html-jira.vm";
   private final JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
   private final NavigableField reminderTextField;
   private final VelocityParamFactory velocityParamFactory = ComponentAccessor.getVelocityParamFactory();
   private final MailQueue mailQueue;
   private final DateTimeFormatter dateTimeFormater;
   private final ResourceReader resourceReader;
   private String bodyContent;
   private String subjectContent;

   public JiraNativeMailBuilder(DateTimeFormatterFactory dateTimeFormatterFactory) {
      this.reminderTextField = new DefaultReminderField("reminder-text", "reminder.jira.action.text.column.label");
      this.mailQueue = ComponentAccessor.getMailQueue();
      this.dateTimeFormater = dateTimeFormatterFactory.formatter().forLoggedInUser();
      this.resourceReader = new ResourceReader();
      this.bodyContent = this.resourceReader.readResourceAsString("/templates/mail/mail-body-html-jira.vm");
      this.subjectContent = this.resourceReader.readResourceAsString("/templates/mail/mail-subject.vm");
   }

   public void buildAndSendMail(Date date, ApplicationUser user, Map<Issue, String> textMap) throws VelocityException {
      ApplicationUser origUser = this.authenticationContext.getUser();
      this.authenticationContext.setLoggedInUser(user);
      Set<Issue> issues = textMap.keySet();
      NotificationRecipient recipient = new NotificationRecipient(user);
      Map<String, Object> params = this.getDefaultVelocityParams();
      params.put("date", this.dateTimeFormater.forUser(user).format(date));
      params.put("user", user);
      params.put("issues", issues);
      params.put("issuecount", textMap.size());
      params.put("layout", this.getTableLayout(user, textMap));
      Email email = new Email(recipient.getEmail());
      if (this.wantsHtmlMails(recipient)) {
         try {
            email = this.createEmail(email, recipient, params);
         } catch (Exception var10) {
            LOG.error("cannot create html mail, message " + var10.getMessage(), var10);
         }
      }

      this.mailQueue.addItem(new SingleMailQueueItem(email));
      this.authenticationContext.setLoggedInUser(origUser);
   }

   private Email createEmail(Email email, NotificationRecipient recipient, Map<String, Object> params) {
      try {
         return new EmailBuilder(email, recipient).withBody(this.bodyContent).withSubject(this.subjectContent).addParameters(params).renderNow();
      } catch (MessagingException var5) {
         throw new RuntimeException(var5);
      }
   }

   private boolean wantsHtmlMails(NotificationRecipient recipient) {
      return recipient.getFormat().equals("html");
   }

   private IssueTableLayoutBean getTableLayout(ApplicationUser user, Map<Issue, String> issuesWithreminderText) {
      IssueTableLayoutBean helperTableLayout = new IssueTableLayoutBean(user, null);
      List<ColumnLayoutItem> columns = helperTableLayout.getColumns();
      columns.add(new ReminderTextColumnLayoutItem(issuesWithreminderText, this.reminderTextField, columns.size()));
      IssueTableLayoutBean tableLayout = new IssueTableLayoutBean(columns);
      tableLayout.setSortingEnabled(false);
      tableLayout.addCellDisplayParam("email_view", Boolean.TRUE);
      return tableLayout;
   }

   private Map<String, Object> getDefaultVelocityParams() {
      Map<String, Object> params = new HashMap<>();
      params.put("applicationProperties", ComponentAccessor.getApplicationProperties());
      params.put("baseurl", ComponentAccessor.getApplicationProperties().getString("jira.baseurl"));
      params.put("textutils", new TextUtils());
      params.put("stringutils", new StringUtils());
      params.put("webResourceManager", ComponentAccessor.getWebResourceManager());
      params.put("urlModeAbsolute", UrlMode.ABSOLUTE);
      params.put("i18n", this.authenticationContext.getI18nHelper());
      return CompositeMap.of(params, this.velocityParamFactory.getDefaultVelocityParams(this.authenticationContext));
   }
}
