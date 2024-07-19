package com.reminder.jira.plugin.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import com.reminder.jira.plugin.dto.ReminderDTO;
import com.reminder.jira.plugin.model.Reminder;
import com.reminder.jira.plugin.service.AOReminderService;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class MyRemindersServlet extends HttpServlet {

   private static final Logger LOG = Logger.getLogger(MyRemindersServlet.class);
   private final AOReminderService aoReminderService;

   public MyRemindersServlet(AOReminderService aoReminderService) {
      this.aoReminderService = aoReminderService;
   }

   @Override
   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
      ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getUser();
      if (user == null) {
         resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
         return;
      }

      String query = req.getParameter("query");
      String searchType = req.getParameter("searchType");

      try {
         Collection<Reminder> reminders = aoReminderService.getReminders(user);
         if (query != null && searchType != null) {
            reminders = filterReminders(reminders, query, searchType);
         }

         List<ReminderDTO> reminderDTOs = reminders.stream().map(this::toReminderDTO).collect(Collectors.toList());

         req.setAttribute("reminderDTOs", reminderDTOs);
         req.getRequestDispatcher("/templates/manage-reminders.vm").forward(req, resp);
      } catch (Exception e) {
         LOG.error("Error fetching reminders", e);
         resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }
   }

   private Collection<Reminder> filterReminders(Collection<Reminder> reminders, String query, String searchType) {
      switch (searchType) {
         case "task":
            return reminders.stream()
                    .filter(reminder -> reminder.getIssueKey().contains(query))
                    .collect(Collectors.toList());
         case "creator":
            return reminders.stream()
                    .filter(reminder -> reminder.getUserKey().contains(query))
                    .collect(Collectors.toList());
         case "date":
            return reminders.stream()
                    .filter(reminder -> reminder.getDate().toString().contains(query))
                    .collect(Collectors.toList());
         case "participant":
            return reminders.stream()
                    .filter(reminder -> reminder.getAdditionalRecipients() != null && reminder.getAdditionalRecipients().contains(query))
                    .collect(Collectors.toList());
         case "group":
            return reminders.stream()
                    .filter(reminder -> reminder.getGroups() != null && reminder.getGroups().contains(query))
                    .collect(Collectors.toList());
         default:
            return reminders;
      }
   }

   private ReminderDTO toReminderDTO(Reminder reminder) {
      ReminderDTO reminderDTO = new ReminderDTO();
      reminderDTO.setId(reminder.getID());
      reminderDTO.setIssueId(reminder.getIssueId());
      reminderDTO.setIssueKey(reminder.getIssueKey());
      reminderDTO.setCreateDisplayName(reminder.getUserKey());
      reminderDTO.setReminderdate(reminder.getDate());
      reminderDTO.setreminderText(reminder.getText());
      reminderDTO.setAdditionalRecipients(reminder.getAdditionalRecipients());
      reminderDTO.setGroups(reminder.getGroups());
      reminderDTO.setCondition(reminder.getCondition());
      reminderDTO.setAllowEdit(true);
      reminderDTO.setreminderdateIso8601(reminder.getDate().toString());
      reminderDTO.setreminderdateHtml(reminder.getDate().toString());
      return reminderDTO;
   }
}
