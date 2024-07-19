package com.reminder.jira.plugin;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.issue.fields.renderer.JiraRendererPlugin;
import com.atlassian.jira.plugin.webfragment.contextproviders.AbstractJiraContextProvider;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.VelocityParamFactory;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.web.component.jql.AutoCompleteJsonGenerator;
import com.reminder.jira.plugin.dto.ReminderDTO;
import com.reminder.jira.plugin.model.Reminder;
import com.reminder.jira.plugin.service.AOReminderService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

public class ReminderIssueBlockContextProvider extends AbstractJiraContextProvider {
   private final DateTimeFormatter dateTimeFormater;
   private final AOReminderService aoReminderService;
   private final VelocityParamFactory velocityParamFactory;
   private final JiraRendererPlugin renderer;
   private final AutoCompleteJsonGenerator autoCompleteJsonGenerator;
   private final GroupManager groupManager;
   private final UserManager userManager;

   public ReminderIssueBlockContextProvider(
      AOReminderService aoReminderService,
      RendererManager rendererManager,
      DateTimeFormatterFactory dateTimeFormatterFactory,
      VelocityParamFactory velocityParamFactory,
      AutoCompleteJsonGenerator autoCompleteJsonGenerator
   ) {
      this.aoReminderService = aoReminderService;
      this.velocityParamFactory = velocityParamFactory;
      this.renderer = rendererManager.getRendererForType("atlassian-wiki-renderer");
      this.dateTimeFormater = dateTimeFormatterFactory.formatter().forLoggedInUser();
      this.autoCompleteJsonGenerator = autoCompleteJsonGenerator;
      this.groupManager = ComponentAccessor.getGroupManager();
      this.userManager = ComponentAccessor.getUserManager();
   }

   public void init(Map params) {
   }

   public Map getContextMap(ApplicationUser user, JiraHelper jiraHelper) {
      Map<String, Object> contextMap = this.velocityParamFactory.getDefaultVelocityParams();
      contextMap.putAll(jiraHelper.getContextParams());
      Issue issue = (Issue)contextMap.get("issue");
      Reminder[] reminders = this.aoReminderService.getAllRemindersForIssue(user, issue);
      contextMap.put("issue", issue);
      List<ReminderDTO> reminderDTOs = this.toReminderDTOs(this.filterReminders(reminders, user), issue, user);
      reminderDTOs.sort(Comparator.comparing(ReminderDTO::getReminderdate).reversed());
      contextMap.put("reminders", reminderDTOs);

      try {
         contextMap.put("visibleFieldNamesJson", this.getVisibleFieldNamesJson());
         contextMap.put("visibleFunctionNamesJson", this.getVisibleFunctionNamesJson());
         contextMap.put("jqlReservedWordsJson", this.getJqlReservedWordsJson());
      } catch (JSONException var8) {
      }

      return contextMap;
   }

   private List<Reminder> filterReminders(Reminder[] reminders, ApplicationUser user) {
      List<Reminder> filteredReminders = new ArrayList<>();
      Arrays.asList(reminders).forEach(reminder -> {
         if (reminder.getPublicReminder()) {
            filteredReminders.add(reminder);
         } else {
            boolean isCurrentUserOwner = reminder.getUserKey().equals(user.getKey());
            boolean isCurrentUserInAdditionalRecipients = false;
            boolean isCurrentUserInGroup = false;
            if (StringUtils.isNotEmpty(reminder.getAdditionalRecipients())) {
               isCurrentUserInAdditionalRecipients = reminder.getAdditionalRecipients().contains(user.getUsername());
            }

            if (StringUtils.isNotEmpty(reminder.getGroups())) {
               isCurrentUserInGroup = Arrays.stream(reminder.getGroups().split(";")).anyMatch(group -> this.groupManager.isUserInGroup(user, group));
            }

            if (isCurrentUserOwner || isCurrentUserInAdditionalRecipients || isCurrentUserInGroup) {
               filteredReminders.add(reminder);
            }
         }
      });
      return filteredReminders;
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

   private List<ReminderDTO> toReminderDTOs(List<Reminder> reminders, Issue issue, ApplicationUser user) {
      return reminders.stream()
         .map(
            reminder -> {
               ReminderDTO reminderDTO = new ReminderDTO();
               Date reminderdate = reminder != null ? reminder.getDate() : null;
               String reminderText = reminder != null ? reminder.getText() : "";
               String condition =  reminder != null ? reminder.getCondition() : "";
               String additionalRecipients = reminder != null ? reminder.getAdditionalRecipients() : "";
               String groups = reminder != null ? reminder.getGroups() : "";
               if (groups != null) {
                  reminderDTO.setGroups(groups);
               }
               if (additionalRecipients != null) {
                  reminderDTO.setAdditionalRecipients(additionalRecipients);
               }
               reminderDTO.setReminderdate(reminderdate);
               reminderDTO.setreminderText(reminderText);
               reminderDTO.setId(reminder.getID());
               reminderDTO.setreminderTextHtml(this.renderer.render(reminderText, issue.getIssueRenderContext()));
               reminderDTO.setreminderdateIso8601(this.formatDateForInput(reminderdate));
               reminderDTO.setCondition(condition);
               reminderDTO.setreminderdateHtml(
                  StringEscapeUtils.escapeHtml(this.dateTimeFormater.withStyle(DateTimeStyle.RELATIVE_ALWAYS_WITH_TIME).format(reminderdate))
               );
               ApplicationUser creator = this.userManager.getUserByKey(reminder.getUserKey());
               if (creator != null) {
                  reminderDTO.setCreateDisplayName(creator.getDisplayName());
               }
               reminderDTO.setAllowEdit(user.getKey().equals(reminder.getUserKey()));
               return reminderDTO;
            }
         )
         .collect(Collectors.toList());
   }

   private String formatDateForInput(Date date) {
      return this.dateTimeFormater.withSystemZone().withStyle(DateTimeStyle.DATE).format(date);
   }
}
