package com.reminder.jira.plugin.servlet;

import com.atlassian.jira.component.ComponentAccessor;
import com.reminder.jira.plugin.service.AOReminderService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

public class ManageRemindersServlet extends HttpServlet {

   private static final Logger LOG = Logger.getLogger(ManageRemindersServlet.class);

   private final AOReminderService aoReminderService;

   public ManageRemindersServlet(AOReminderService aoReminderService) {
      this.aoReminderService = aoReminderService;
   }

   protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
      String operation = req.getParameter("operation");
      String reminderId = req.getParameter("reminderId");
      try {
         if (operation.equals("deleteAll")) {
            this.aoReminderService.deleteAllReminders(ComponentAccessor.getJiraAuthenticationContext().getUser());
         } else if (operation.equals("delete")) {
            this.aoReminderService.deleteReminder(ComponentAccessor.getJiraAuthenticationContext().getUser(), Integer.valueOf(reminderId));
         }
         resp.setStatus(HttpServletResponse.SC_OK);
      } catch (Exception e) {
         LOG.error("Error processing request", e);
         resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      }

      resp.setStatus(200);
   }
}
