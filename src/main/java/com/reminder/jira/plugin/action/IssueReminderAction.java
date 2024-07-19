package com.reminder.jira.plugin.action;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.IssueService.IssueResult;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.issue.fields.renderer.JiraRendererPlugin;
import com.atlassian.jira.mention.MentionService;
import com.atlassian.jira.plugin.renderer.JiraRendererModuleDescriptor;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.timezone.TimeZoneInfo;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.DateFieldFormat;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.UrlBuilder;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.web.component.jql.AutoCompleteJsonGenerator;
import com.google.common.base.Joiner;
import com.opensymphony.util.TextUtils;
import com.reminder.jira.plugin.model.Reminder;
import com.reminder.jira.plugin.service.AOReminderService;
import com.reminder.jira.plugin.service.JiraUserService;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;

@SupportedMethods({RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class IssueReminderAction extends AbstractReminderAction {
   private final AOReminderService aoReminderService;
   private final IssueService issueService;
   private final DateFieldFormat dateFieldFormat;
   private final MentionService mentionService;
   private final JiraRendererPlugin renderer;
   private final JiraUserService jiraUserService;
   private Issue issue;
   private String reminderDateString;
   private String reminderTimeString;
   private String additionalRecipients;
   private String condition;
   private Date reminderdate;
   private String groups;
   private String reminderText;
   private String privateReminder = "true";
   private String successMessage;
   private final AutoCompleteJsonGenerator autoCompleteJsonGenerator;

   public IssueReminderAction(
      AOReminderService aoReminderService,
      IssueService issueService,
      DateFieldFormat dateFieldFormat,
      RendererManager rendererManager,
      MentionService mentionService,
      JiraUserService jiraUserService,
      AutoCompleteJsonGenerator autoCompleteJsonGenerator) {
      this.aoReminderService = aoReminderService;
      this.issueService = issueService;
      this.dateFieldFormat = dateFieldFormat;
      this.mentionService = mentionService;
      this.renderer = rendererManager.getRendererForType("atlassian-wiki-renderer");
      this.jiraUserService = jiraUserService;
      this.autoCompleteJsonGenerator = autoCompleteJsonGenerator;
   }

   protected void doValidation() {
      if (this.getLoggedInUser() == null) {
         this.addError("access", this.getText("reminder.jira.error.login.required"));
      } else {
         try {
            Long issueId = Long.parseLong(this.getHttpRequest().getParameter("id"));
            IssueResult issueResult = this.issueService.getIssue(this.getLoggedInUser(), issueId);
            if (issueResult.isValid()) {
               this.issue = issueResult.getIssue();
               if (this.isSubmitRequest() && TextUtils.stringSet(this.reminderDateString)) {
                  try {
                     this.reminderdate = this.dateFieldFormat.parseDatePicker(this.reminderDateString);
                  } catch (IllegalArgumentException var5) {
                     this.addError(
                        "reminderdate",
                        this.getText(
                           "reminder.jira.error.invalid.date",
                           this.getApplicationProperties().getDefaultBackedString("jira.date.picker.java.format"),
                           this.dateFieldFormat.formatDatePicker(new Date())
                        )
                     );
                  }
               }
            } else {
               Collection<String> errors = issueResult.getErrorCollection().getErrorMessages();
               String errorMsg = errors.isEmpty()
                  ? this.getText("reminder.jira.error.issue.access")
                  : this.getText("reminder.jira.error.issue.access.specific", Joiner.on("; ").join(errors));
               this.addError("access", errorMsg);
            }
         } catch (NumberFormatException var6) {
            this.addError("access", this.getText("reminder.jira.error.bad.issue.id"));
         }
      }
   }

   public int getHours(String timeValue) {
      String[] splitByColon = timeValue.split(":");
      int hoursValue = Integer.parseInt(splitByColon[0]);
      String[] splitForMins = splitByColon[1].split(" ");
      if (splitForMins[1].equals("PM") && hoursValue != 12) {
         hoursValue += 12;
      }

      if (splitForMins[1].equals("AM") && hoursValue == 12) {
         hoursValue = 0;
      }

      return hoursValue;
   }

   public int getMinutes(String timeValue) {
      String[] splitByColon = timeValue.split(":");
      String[] splitForMins = splitByColon[1].split(" ");
      return Integer.parseInt(splitForMins[0]);
   }

   protected String doExecute() {
      if (this.isSubmitRequest()) {
         if (this.reminderdate != null && this.reminderTimeString != null) {
            this.reminderdate.setHours(this.getHours(this.reminderTimeString));
            this.reminderdate.setMinutes(this.getMinutes(this.reminderTimeString));
            TimeZoneInfo timezoneInfo = this.jiraUserService.getUserTimezone(this.getLoggedInUser());
            int timezoneDiff = TimeZone.getTimeZone(timezoneInfo.getTimeZoneId()).getRawOffset() - TimeZone.getTimeZone(ZoneId.systemDefault()).getRawOffset();
            if (timezoneDiff != 0) {
               int hours = (int)TimeUnit.MILLISECONDS.toHours((long)timezoneDiff);
               this.reminderdate.setHours(this.reminderdate.getHours() + hours);
            }

            String reminderId = this.getHttpRequest().getParameter("reminderId");
            this.aoReminderService
               .setReminder(
                  this.getLoggedInUser(),
                  this.issue,
                  this.reminderdate,
                  this.reminderText,
                  this.reminderTimeString,
                  this.isIdString(reminderId) ? Integer.valueOf(reminderId) : null,
                  this.getAdditionalRecipientsFromRequestParams(),
                  this.condition,
                  this.isPrivateReminder(),
                  this.getGroupsFromRequestParams()
               );
         }

         return this.returnToIssue();
      } else {
         return "success";
      }
   }

   private boolean isPrivateReminder() {
      return Boolean.TRUE.toString().equals(this.getHttpRequest().getParameter("privateReminder"));
   }

   private boolean isIdString(String reminderId) {
      return reminderId != null && !reminderId.contains("{");
   }

   public String doDeleteReminder() {
      if (this.getLoggedInUser() == null) {
         return "permissionviolation";
      } else {
         try {
            Long issueId = Long.parseLong(this.getHttpRequest().getParameter("id"));
            String reminderId = this.getHttpRequest().getParameter("reminderId");
            IssueResult issueResult = this.issueService.getIssue(this.getLoggedInUser(), issueId);
            if (issueResult.isValid()) {
               this.issue = issueResult.getIssue();
               Reminder reminder = this.aoReminderService.getReminder(this.getLoggedInUser(), this.issue);
               if (reminder != null) {
                  this.aoReminderService.deleteReminder(this.getLoggedInUser(), this.issue, Integer.valueOf(reminderId));
               }

               return this.returnToIssue();
            } else {
               return "issuenotfound";
            }
         } catch (NumberFormatException var5) {
            return "issuenotfound";
         }
      }
   }

   protected boolean isSubmitRequest() {
      String action = this.getHttpRequest().getParameter("action");
      return action != null && action.equalsIgnoreCase("submit");
   }

   protected String returnToIssue() {
      this.successMessage = this.getText("reminder.jira.action.success");
      String url = StringUtils.isNotBlank(this.getReturnUrl()) ? this.getReturnUrl() : this.getIssueUrl();
      return this.returnMsgToUser(url, this.successMessage, MessageType.SUCCESS, true, null);
   }

   public String getReminderdate() {
      if (!this.isSubmitRequest() && this.getHttpRequest().getParameter("reminderId") != null) {
         int reminderId = Integer.valueOf(this.getHttpRequest().getParameter("reminderId"));
         Reminder reminder = this.aoReminderService.getReminder(this.getLoggedInUser(), this.issue, reminderId);
         if (reminder != null) {
            this.reminderDateString = this.dateFieldFormat.formatDatePicker(reminder.getDate());
         }
      }

      return this.reminderDateString;
   }

   public String getPrivateReminder() {
      if (!this.isSubmitRequest() && this.getHttpRequest().getParameter("reminderId") != null) {
         int reminderId = Integer.valueOf(this.getHttpRequest().getParameter("reminderId"));
         Reminder reminder = this.aoReminderService.getReminder(this.getLoggedInUser(), this.issue, reminderId);
         if (reminder != null) {
            if (reminder.getPublicReminder()) {
               this.privateReminder = Boolean.FALSE.toString();
            } else {
               this.privateReminder = Boolean.TRUE.toString();
            }
         }
      }

      return this.privateReminder;
   }

   public String[] getGroups() {
      if (!this.isSubmitRequest() && this.getHttpRequest().getParameter("reminderId") != null) {
         int reminderId = Integer.valueOf(this.getHttpRequest().getParameter("reminderId"));
         Reminder reminder = this.aoReminderService.getReminder(this.getLoggedInUser(), this.issue, reminderId);
         if (reminder != null) {
            this.groups = reminder.getGroups();
         }
      }

      return this.groups != null ? this.groups.split(";") : null;
   }

   public String getRemindertime() {
      if (!this.isSubmitRequest() && this.getHttpRequest().getParameter("reminderId") != null) {
         int reminderId = Integer.valueOf(this.getHttpRequest().getParameter("reminderId"));
         Reminder reminder = this.aoReminderService.getReminder(this.getLoggedInUser(), this.issue, reminderId);
         if (reminder != null) {
            this.reminderTimeString = reminder.getTime();
         }
      }

      return this.reminderTimeString;
   }

   public String getVisibleFieldNamesJson() throws JSONException {
      ApplicationUser paramUser = this.getLoggedInUser();
      Locale paramLocale = this.getLocale();
      return this.autoCompleteJsonGenerator.getVisibleFieldNamesJson(paramUser, paramLocale);
   }

   public String getVisibleFunctionNamesJson() throws JSONException {
      ApplicationUser paramUser = this.getLoggedInUser();
      Locale paramLocale = this.getLocale();
      return this.autoCompleteJsonGenerator.getVisibleFunctionNamesJson(paramUser, paramLocale);
   }

   public String getJqlReservedWordsJson() throws JSONException {
      return this.autoCompleteJsonGenerator.getJqlReservedWordsJson();
   }

   public ApplicationUser getLoggedInUser() {
      return ComponentAccessor.getJiraAuthenticationContext().getUser();
   }

   public Locale getLocale() {
      return this.getI18nHelper().getLocale();
   }

   protected I18nHelper getI18nHelper() {
      return ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
   }

   public String getReminderId() {
      if (!this.isSubmitRequest()) {
         return this.getHttpRequest().getParameter("reminderId") != null ? this.getHttpRequest().getParameter("reminderId") : null;
      } else {
         return null;
      }
   }

   public void setReminderdate(String reminderdate) {
      this.reminderDateString = reminderdate;
   }

   public void setprivateReminder(String privateReminder) {
      this.privateReminder = privateReminder;
   }

   public void setRemindertime(String remindertime) {
      this.reminderTimeString = remindertime;
   }

   public void setgroups(String groups) {
      this.groups = this.getGroupsFromRequestParams();
   }

   private String getGroupsFromRequestParams() {
      String[] groups = this.getHttpRequest().getParameterValues("groups");
      return groups != null ? StringUtils.join(groups, ";") : null;
   }

   public void setreminderJqlcond(String reminderJqlcond) {
      this.condition = reminderJqlcond;
   }

   public void setAdditionalrecipients(String additionalrecipients) {
      this.additionalRecipients = this.getAdditionalRecipientsFromRequestParams();
   }

   private String getAdditionalRecipientsFromRequestParams() {
      String[] additionalrecipientsArray = this.getHttpRequest().getParameterValues("additionalrecipients");
      return additionalrecipientsArray != null ? StringUtils.join(additionalrecipientsArray, ",") : null;
   }

   public String[] getAdditionalrecipients() {
      if (!this.isSubmitRequest()
         && this.getHttpRequest().getParameter("reminderId") != null
         && !this.getHttpRequest().getParameter("reminderId").contains("{")) {
         int reminderId = Integer.valueOf(this.getHttpRequest().getParameter("reminderId"));
         Reminder reminder = this.aoReminderService.getReminder(this.getLoggedInUser(), this.issue, reminderId);
         if (reminder != null) {
            this.additionalRecipients = reminder.getAdditionalRecipients();
         }
      }

      return this.additionalRecipients != null ? this.additionalRecipients.split(",") : null;
   }

   public String getreminderJqlcond() {
      if (!this.isSubmitRequest()
         && this.getHttpRequest().getParameter("reminderId") != null
         && !this.getHttpRequest().getParameter("reminderId").contains("{")) {
         int reminderId = Integer.valueOf(this.getHttpRequest().getParameter("reminderId"));
         Reminder reminder = this.aoReminderService.getReminder(this.getLoggedInUser(), this.issue, reminderId);
         if (reminder != null) {
            this.condition = reminder.getCondition();
         }
      }

      return this.condition;
   }

   public String getreminderText() {
      if (!this.isSubmitRequest()
         && this.getHttpRequest().getParameter("reminderId") != null
         && !this.getHttpRequest().getParameter("reminderId").contains("{")) {
         int reminderId = Integer.valueOf(this.getHttpRequest().getParameter("reminderId"));
         Reminder reminder = this.aoReminderService.getReminder(this.getLoggedInUser(), this.issue, reminderId);
         if (reminder != null) {
            this.reminderText = reminder.getText();
         }
      }

      return this.reminderText;
   }

   public void setreminderText(String reminderText) {
      this.reminderText = reminderText;
   }

   public boolean isMentionable() {
      return this.mentionService.isUserAbleToMention(this.getLoggedInUser());
   }

   public Issue getIssue() {
      return this.issue;
   }

   public String getTitle() {
      if (this.getHttpRequest().getParameter("reminderId") != null && !this.getHttpRequest().getParameter("reminderId").contains("{")) {
         return this.issue != null
            ? this.getText("reminder.jira.update.action.label.specific", this.issue.getSummary())
            : this.getText("reminder.jira.update.action.label");
      } else {
         return this.issue != null
            ? this.getText("reminder.jira.create.action.label.specific", this.issue.getSummary())
            : this.getText("reminder.jira.create.action.label");
      }
   }

   public String getIssueUrl() {
      UrlBuilder urlBuilder = new UrlBuilder(this.getApplicationProperties().getString("jira.baseurl"));
      if (this.issue != null) {
         urlBuilder.addPathUnsafe("browse/" + this.issue.getKey());
      }

      return urlBuilder.asUrlString();
   }

   public String getCancelUrl() {
      String returnUrl = this.getReturnUrlForCancelLink();
      String url;
      if (StringUtils.isNotBlank(returnUrl)) {
         if (returnUrl.matches("https?://.*")) {
            url = returnUrl;
         } else {
            UrlBuilder urlBuilder = new UrlBuilder(this.getApplicationProperties().getString("jira.baseurl"));
            urlBuilder.addPathUnsafe(returnUrl);
            url = urlBuilder.asUrlString();
         }
      } else {
         url = this.getIssueUrl();
      }

      return url;
   }

   public String getSuccessMessage() {
      return this.successMessage;
   }

   public JiraRendererModuleDescriptor getRendererDescriptor() {
      return this.renderer.getDescriptor();
   }

   public String getRendererType() {
      return "atlassian-wiki-renderer";
   }
}
