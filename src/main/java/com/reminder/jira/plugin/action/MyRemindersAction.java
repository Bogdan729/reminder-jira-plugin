package com.reminder.jira.plugin.action;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.datetime.DateTimeFormatter;
import com.atlassian.jira.datetime.DateTimeFormatterFactory;
import com.atlassian.jira.datetime.DateTimeStyle;
import com.atlassian.jira.issue.RendererManager;
import com.atlassian.jira.issue.fields.renderer.JiraRendererPlugin;
import com.atlassian.jira.security.request.RequestMethod;
import com.atlassian.jira.security.request.SupportedMethods;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.permission.AuthorisationException;
import com.reminder.jira.plugin.dto.ReminderDTO;
import com.reminder.jira.plugin.model.Reminder;
import com.reminder.jira.plugin.service.AOReminderService;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

@SupportedMethods({RequestMethod.GET, RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
public class MyRemindersAction extends AbstractReminderAction {
    private static final Logger LOG = Logger.getLogger(MyRemindersAction.class);
   private final AOReminderService aoReminderService;
   private final JiraRendererPlugin renderer;
   private final DateTimeFormatter dateTimeFormater;
   final DateTimeFormatterFactory dateTimeFormatterFactory;
   final RendererManager rendererManager;

   public MyRemindersAction(
      AOReminderService aoReminderService, DateTimeFormatterFactory dateTimeFormatterFactory, RendererManager rendererManager
   ) {
      this.aoReminderService = aoReminderService;
      this.dateTimeFormatterFactory = dateTimeFormatterFactory;
      this.rendererManager = rendererManager;
      this.renderer = this.rendererManager.getRendererForType("atlassian-wiki-renderer");
      this.dateTimeFormater = this.dateTimeFormatterFactory.formatter().forLoggedInUser();
   }

   protected String doExecute() throws Exception {
      return super.doExecute();
   }

   protected void doValidation() {
      if (this.getLoggedInUser() == null) {
         this.addError("access", this.getText("reminder.jira.error.login.required"));
         throw new AuthorisationException("Not authorized");
      }
   }

   public List<ReminderDTO> getReminders() {
      return this.toReminderDTOs(this.aoReminderService.getReminders(ComponentAccessor.getJiraAuthenticationContext().getUser()));
   }

   public String getBaseUrl() {
      return ComponentAccessor.getApplicationProperties().getString("jira.baseurl");
   }

   private List<ReminderDTO> toReminderDTOs(Collection<Reminder> reminders) {
      return reminders.stream()
            .map(
               reminder -> {
                  ReminderDTO reminderDTO = new ReminderDTO();
                  Date reminderdate = reminder != null ? reminder.getDate() : null;
                  String reminderText = reminder != null ? reminder.getText() : "";
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
                  reminderDTO.setCondition(reminder.getCondition());
                  reminderDTO.setIssueId(reminder.getIssueId());
                  reminderDTO.setIssueKey(reminder.getIssueKey());
                  reminderDTO.setreminderTextHtml(reminderText);
                  reminderDTO.setreminderdateIso8601(this.formatDateForInput(reminderdate));
                  reminderDTO.setreminderdateHtml(
                     StringEscapeUtils.escapeHtml(this.dateTimeFormater.withStyle(DateTimeStyle.RELATIVE_ALWAYS_WITH_TIME).format(reminderdate))
                  );
                   ApplicationUser creator = ComponentAccessor.getUserManager().getUserByKey(reminder.getUserKey());
                   if (creator != null) {
                       reminderDTO.setCreateDisplayName(creator.getDisplayName());
                   }
                  return reminderDTO;
               }
            )
            .sorted(Comparator.comparing(ReminderDTO::getReminderdate).reversed())
            .collect(Collectors.toList());
   }

   private String formatDateForInput(Date date) {
      return this.dateTimeFormater.withSystemZone().withStyle(DateTimeStyle.DATE).format(date);
   }
}
