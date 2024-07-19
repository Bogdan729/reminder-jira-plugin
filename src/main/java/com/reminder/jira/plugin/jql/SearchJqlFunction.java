package com.reminder.jira.plugin.jql;

import com.atlassian.jira.JiraDataType;
import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.plugin.jql.function.AbstractJqlFunction;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;
import com.opensymphony.util.TextUtils;
import com.reminder.jira.plugin.model.Reminder;
import com.reminder.jira.plugin.service.AOReminderService;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;

public class SearchJqlFunction extends AbstractJqlFunction {
   private static final Logger LOGGER = Logger.getLogger(SearchJqlFunction.class);
   private static final int EXPECTED_ARGS_COUNT = 1;
   private static final String ALL_FILTER_ARGUMENT = "all";
   private static final String DATE_RANGE_REGEX = "(\\-?)(\\d+)([wdhmM])((\\s+)(\\-?)(\\d+)([wdhmM]))*$";
   private static final String DATE_REGEX = "^(19|20)\\d\\d([-/])(0[1-9]|1[012])([-/])(0[1-9]|[12]\\d|3[01])?";
   private final AOReminderService aoReminderService;

   public SearchJqlFunction(AOReminderService aoReminderService) {
      this.aoReminderService = aoReminderService;
   }

   @Nonnull
   public MessageSet validate(ApplicationUser user, @Nonnull FunctionOperand functionOperand, @Nonnull TerminalClause terminalClause) {
      MessageSet messages = new MessageSetImpl();
      List<String> args = functionOperand.getArgs();
      if (args.isEmpty() || args.size() > 1) {
         messages.addErrorMessage(this.getI18n().getText("reminder.jira.search.jql.error.bad.num.arguments", functionOperand.getName()));
         return messages;
      } else if (StringUtils.isBlank(args.get(0))) {
         messages.addErrorMessage(this.getI18n().getText("reminder.jira.search.jql.error.empty.argument", functionOperand.getName()));
         return messages;
      } else if (!args.get(0).matches("(\\-?)(\\d+)([wdhmM])((\\s+)(\\-?)(\\d+)([wdhmM]))*$")
         && !args.get(0).equals("all")
         && !args.get(0).matches("^(19|20)\\d\\d([-/])(0[1-9]|1[012])([-/])(0[1-9]|[12]\\d|3[01])?")) {
         messages.addErrorMessage(this.getI18n().getText("reminder.jira.search.jql.error.invalid.argument", functionOperand.getName()));
         return messages;
      } else {
         return messages;
      }
   }

   @Nonnull
   public List<QueryLiteral> getValues(
      @Nonnull QueryCreationContext queryCreationContext, @Nonnull FunctionOperand functionOperand, @Nonnull TerminalClause terminalClause
   ) {
      List<String> args = functionOperand.getArgs();
      ApplicationUser user = queryCreationContext.getApplicationUser();
      Collection<Reminder> reminders = this.aoReminderService.getReminders(user);
      List<QueryLiteral> literals = new LinkedList();
      if (!args.isEmpty() && args.size() <= 1) {
         if (reminders != null && reminders.size() > 0) {
            if (args.get(0).equals("all")) {
               for(Reminder reminder : reminders) {
                  literals.add(new QueryLiteral(functionOperand, reminder.getIssueId()));
               }
            } else {
               String argument = args.get(0);
               if (argument.matches("(\\-?)(\\d+)([wdhmM])((\\s+)(\\-?)(\\d+)([wdhmM]))*$")) {
                  String[] ranges = StringUtils.split(argument, " ");
                  Map<String, Integer> rangeArgs = new HashMap<>();

                  for(String s : ranges) {
                     String unit = s.substring(s.length() - 1);
                     int value = Integer.parseInt(s.substring(0, s.length() - 1));
                     if (!rangeArgs.containsKey(unit)) {
                        rangeArgs.put(unit, value);
                     }
                  }

                  Date calculatedDate = this.calculateDateRange(rangeArgs);
                  literals = this.getRemindersForDateRange(calculatedDate, reminders, functionOperand);
               } else if (TextUtils.stringSet(argument)) {
                  try {
                     Date dateArg = DateUtils.parseDate(argument, new String[]{"yyyy/MM/dd", "yyyy-MM-dd"});
                     literals = this.getRemindersForDate(dateArg, reminders, functionOperand);
                  } catch (Throwable var17) {
                     LOGGER.error(this.getI18n().getText("reminder.jira.search.jql.error.parse.date"), var17);
                  }
               }
            }
         }

         return literals;
      } else {
         return Collections.emptyList();
      }
   }

   private List<QueryLiteral> getRemindersForDate(Date parsedDate, Collection<Reminder> reminders, FunctionOperand operand) {
      List<QueryLiteral> literals = new LinkedList();
      if (parsedDate != null) {
         for(Reminder reminder : reminders) {
            if (reminder.getDate().compareTo(parsedDate) == 0) {
               literals.add(new QueryLiteral(operand, reminder.getIssueId()));
            }
         }
      }

      return literals;
   }

   private List<QueryLiteral> getRemindersForDateRange(Date parsedDate, Collection<Reminder> reminders, FunctionOperand operand) {
      Date now = Calendar.getInstance().getTime();
      List<QueryLiteral> literals = new LinkedList();
      boolean isPastDate = false;
      if (parsedDate != null) {
         for(Reminder reminder : reminders) {
            if (parsedDate.compareTo(now) > 0) {
               if (reminder.getDate().after(now) && reminder.getDate().before(parsedDate)) {
                  literals.add(new QueryLiteral(operand, reminder.getIssueId()));
               }
            } else if (parsedDate.compareTo(now) < 0) {
               if (reminder.getDate().after(parsedDate) && reminder.getDate().before(now)) {
                  literals.add(new QueryLiteral(operand, reminder.getIssueId()));
               }

               isPastDate = true;
            }
         }

         literals.addAll(this.getRemindersForDate(this.formatCurrentDate(now), reminders, operand));
         if (isPastDate) {
            literals.addAll(this.getRemindersForDate(this.formatCurrentDate(parsedDate), reminders, operand));
         }
      }

      return literals;
   }

   private Date formatCurrentDate(Date date) {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd");

      try {
         return simpleDateFormat.parse(simpleDateFormat.format(date));
      } catch (Throwable var4) {
         LOGGER.error(this.getI18n().getText("reminder.jira.search.jql.error.parse.date"), var4);
         return null;
      }
   }

   private Date calculateDateRange(Map<String, Integer> rangeArgs) {
      Date date = Calendar.getInstance().getTime();

      for(Entry<String, Integer> entry : rangeArgs.entrySet()) {
         String s = entry.getKey();
         switch(s) {
            case "w":
               date = DateUtils.addWeeks(date, entry.getValue());
               break;
            case "d":
               date = DateUtils.addDays(date, entry.getValue());
               break;
            case "h":
               date = DateUtils.addHours(date, entry.getValue());
               break;
            case "M":
               date = DateUtils.addMonths(date, entry.getValue());
               break;
            default:
               date = DateUtils.addMinutes(date, entry.getValue());
         }
      }

      return date;
   }

   public int getMinimumNumberOfExpectedArguments() {
      return 1;
   }

   @Nonnull
   public JiraDataType getDataType() {
      return JiraDataTypes.ISSUE;
   }
}
