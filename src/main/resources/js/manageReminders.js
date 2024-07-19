document.addEventListener("DOMContentLoaded", function(event) {
    AJS.$(function($) {
      var initializeTimePicker = function() {
        $(".timepicker").timepicker({
          timeFormat: "h:mm p",
          interval: 10,
          defaultTime: $("#remindertime").attr("value") != null && $("#remindertime").attr("value") !== "${remindertime}" ? $("#remindertime").val() : "10:00 AM",
          startTime: "10:00",
          dynamic: false,
          dropdown: true,
          scrollbar: true
        });
      };

      var initializeGroupMultiSelect = function() {
        new AJS.MultiSelect({
          element: $("#groups"),
          itemAttrDisplayed: "label",
          showDropdownButton: false,
          ajaxOptions: {
            url: contextPath + "/rest/api/2/groups/picker",
            query: true,
            formatResponse: JIRA.GroupPickerUtil.formatResponse
          }
        });
      };

      var handlePrivateReminderCheckbox = function() {
        $("#privateReminder").on("change", function() {
          if ($(this).is(":checked")) {
            $(this).attr("value", "true");
          } else {
            $(this).attr("value", "false");
          }
        });
      };

      var initializeJQLField = function() {
        var fieldId = "reminderJqlcond";
        if ($("#" + fieldId).length) {
          initRemJQLField(fieldId, "jqlerrormsg-" + fieldId);
        }
      };

      var initializeDialogTriggers = function() {
        $(".reminder-trigger-dialog").each(function() {
          $(this).attr("href", appendReturnUrl($(this).attr("href"), window.location.href));
          new JIRA.FormDialog({
            trigger: this,
            id: this.id + "-dialog",
            ajaxOptions: {
              url: this.href,
              data: { decorator: "dialog", inline: "true" }
            }
          });
        });
      };

      var appendReturnUrl = function(url, returnUrl) {
        if (url) {
          url += url.indexOf("?") == -1 ? "?" : "&";
          url += "returnUrl=" + encodeURIComponent(returnUrl);
          return url;
        }
      };

      JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function(e, context, reason) {
        initializeTimePicker();
        initializeGroupMultiSelect();
        handlePrivateReminderCheckbox();
        initializeJQLField();
        initializeDialogTriggers();
      });

      AJS.toInit(function() {
        initializeTimePicker();
        initializeGroupMultiSelect();
        handlePrivateReminderCheckbox();
        initializeJQLField();
        initializeDialogTriggers();

        if (!$(".reminder-block").is(":visible")) {
          $(".no-reminders-message").show();
        }

        $("body").on("click", "a#delete-reminder", function(e) {
          e.preventDefault();
          var reminderId = $(this).attr("reminderId");
          var operation = $(this).attr("operation");
          var dialog = new AJS.Dialog({
            width: 400,
            height: 200,
            id: "delete-reminder-dialog"
          });
          dialog.addHeader(AJS.I18n.getText("reminder.jira.reminder.delete.dialog.header"))
                .addPanel("Panel 1", "<p><strong>" + AJS.I18n.getText("reminder.jira.reminder.delete.dialog.content") + "</strong></p>", "panel-body")
                .addButtonPanel()
                .addButton(AJS.I18n.getText("common.words.yes"), function(dialog) {
                  $.ajax({
                    url: AJS.contextPath() + "/plugins/servlet/manageReminders",
                    type: "POST",
                    data: { reminderId: reminderId, operation: operation },
                    dataType: "json",
                    success: function(response) {
                      $("#aui-message-bar").html("");
                      AJS.messages.success("#aui-message-bar", {
                        title: "Reminder",
                        body: "<p>Reminder(s) deleted successfully.</p>",
                        delay: 10000,
                        duration: 1000,
                        fadeout: true
                      });
                      if ("deleteAll" === operation) {
                        $(".reminder-block").hide();
                      } else {
                        $("#reminder" + reminderId).hide();
                      }
                      if (!$(".reminder-block").is(":visible")) {
                        $(".no-reminders-message").show();
                      }
                    }
                  });
                  dialog.hide();
                }, "delete-reminder-submit")
                .addButton(AJS.I18n.getText("common.words.no"), function(dialog) {
                  dialog.hide();
                }, "delete-reminder-cancel");
          dialog.show();
          return false;
        });
      });
    });

    function initRemJQLField(fieldId, errorId) {
      var field = $("#" + fieldId);
      var visibleFieldNamesJson = JSON.parse($("#visibleFieldNamesJson").text());
      var visibleFunctionNamesJson = JSON.parse($("#visibleFunctionNamesJson").text());
      var jqlReservedWordsJson = JSON.parse($("#jqlReservedWordsJson").text());
      var autoComplete = JIRA.JQLAutoComplete({
        fieldID: field.attr("id"),
        parser: JIRA.JQLAutoComplete.MyParser(jqlReservedWordsJson),
        queryDelay: 0.65,
        jqlFieldNames: visibleFieldNamesJson,
        jqlFunctionNames: visibleFunctionNamesJson,
        minQueryLength: 0,
        allowArrowCarousel: true,
        autoSelectFirst: false,
        errorID: errorId
      });
      $("#" + errorId).show();
      field.expandOnInput();
      autoComplete.buildResponseContainer();
      autoComplete.parse(field.text());
      autoComplete.updateColumnLineCount();
      field.keypress(function(event) {
        if (autoComplete.dropdownController === null || !autoComplete.dropdownController.displayed || autoComplete.selectedIndex < 0) {
          if (event.keyCode == 13 && !event.ctrlKey && !event.shiftKey) {
            event.preventDefault();
            autoComplete.dropdownController.hideDropdown();
          }
        }
      }).bind("expandedOnInput", function() {
        autoComplete.positionResponseContainer();
      }).click(function() {
        autoComplete.dropdownController.hideDropdown();
      }).bind("webhooks.valueChanged", function() {
        autoComplete.parse(field.val());
      });
    }
  });
