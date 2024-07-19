document.addEventListener("DOMContentLoaded", function (event) {
    AJS.$(function (b) {
      var a = function (e) {
        b(".timepicker").timepicker({
          timeFormat: "h:mm p",
          interval: 10,
          defaultTime: AJS.$("#remindertime").attr("value") != null && AJS.$("#remindertime").attr("value") !== "${remindertime}" ? AJS.$("#remindertime").val() : "10:00 AM",
          startTime: "10:00",
          dynamic: false,
          dropdown: true,
          scrollbar: true
        });
        new AJS.MultiSelect({
          element: AJS.$("#groups"),
          itemAttrDisplayed: "label",
          showDropdownButton: false,
          ajaxOptions: {
            url: contextPath + "/rest/api/2/groups/picker",
            query: true,
            formatResponse: JIRA.GroupPickerUtil.formatResponse
          }
        });
        AJS.$("#privateReminder").on("change", function () {
          if (AJS.$(this).is(":checked")) {
            AJS.$(this).attr("value", "true")
          } else {
            AJS.$(this).attr("value", "false")
          }
        });
        var d = "reminderJqlcond";
        if (jQuery("#" + d).length) {
          initRemJQLField(d, "jqlerrormsg-" + d)
        }
        AJS.$(".reminder-trigger-dialog", e).each(function () {
          jQuery(this).attr("href",
              c(jQuery(this).attr("href"), window.location.href));
          new JIRA.FormDialog({
            trigger: this,
            id: this.id + "-dialog",
            ajaxOptions: {
              url: this.href,
              data: {decorator: "dialog", inline: "true"}
            }
          })
        })
      };
      var c = function (d, e) {
        if (d) {
          d += d.indexOf("?") == -1 ? "?" : "&";
          d += "returnUrl=" + encodeURIComponent(e);
          return d
        }
      };
      JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (e, d, f) {
        a(d)
      });
      a(null);
      if (typeof GH == "object" && typeof GH.DetailsView == "object"
          && typeof GH.DetailsView.API_EVENT_DETAIL_VIEW_UPDATED != "undefined") {
        JIRA.bind(GH.DetailsView.API_EVENT_DETAIL_VIEW_UPDATED, function (d) {
          a(null)
        })
      }
      AJS.toInit(function () {
        if (!AJS.$(".reminder-block").is(":visible")) {
          AJS.$(".no-reminders-message").show()
        }
        var e;

        function d(f) {
          e = new AJS.MultiSelect({
            element: b("#additionalrecipients"),
            submitInputVal: true,
            multiple: true,
            showDropdownButton: false,
            errorMessage: AJS.format("There is no such user ''{0}''.", "'{0}'"),
            ajaxOptions: {
              url: AJS.contextPath() + "/rest/api/1.0/users/picker",
              query: true,
              data: {showAvatar: true},
              formatResponse: JIRA.UserPickerUtil.formatResponse
            }
          })
        }

        JIRA.bind(JIRA.Events.NEW_CONTENT_ADDED, function (h, f, g) {
          if (g !== JIRA.CONTENT_ADDED_REASON.panelRefreshed) {
            d(f)
          }
        });
        if (AJS.$(".shortcut-links #reminder-operation").length) {
          AJS.$(".shortcut-links #reminder-operation").detach()
        }
        AJS.$("body").on("click", "a#delete-reminder", function (h) {
          h.preventDefault();
          var g = AJS.$(this).attr("reminderId");
          var f = AJS.$(this).attr("operation");
          var i = new AJS.Dialog(
              {width: 400, height: 200, id: "delete-reminder-dialog"});
          i.addHeader(AJS.I18n.getText(
              "reminder.jira.reminder.delete.dialog.header")).addPanel(
              "Panel 1", "<p><strong>" + AJS.I18n.getText(
                  "reminder.jira.reminder.delete.dialog.content")
              + "</strong></p>", "panel-body").addButtonPanel().addButton(
              AJS.I18n.getText("common.words.yes"), function (j) {
                AJS.$.ajax({
                  url: AJS.contextPath() + "/plugins/servlet/myreminders",
                  type: "POST",
                  data: ({reminderId: g, operation: f}),
                  dataType: "json",
                  success: function (k) {
                    AJS.$("#aui-message-bar").html("");
                    AJS.messages.success("#aui-message-bar", {
                      title: "Reminder",
                      body: "<p>Reminder(s) deleted successfully.</p>",
                      delay: 10000,
                      duration: 1000,
                      fadeout: true
                    });
                    if ("deleteAll" === f) {
                      AJS.$(".reminder-block").hide()
                    } else {
                      AJS.$("#reminder" + g).hide()
                    }
                    if (!AJS.$(".reminder-block").is(":visible")) {
                      AJS.$(".no-reminders-message").show()
                    }
                  }
                });
                j.hide()
              }, "delete-reminder-submit").addButton(
              AJS.I18n.getText("common.words.no"), function (j) {
                j.hide()
              }, "delete-reminder-cancel");
          i.show();
          return false
        })
      })
    });

    function initRemJQLField(e, d, h) {
      var a = jQuery("#" + e);
      var c = JSON.parse(jQuery("#visibleFieldNamesJson").text());
      var f = JSON.parse(jQuery("#visibleFunctionNamesJson").text());
      var g = JSON.parse(jQuery("#jqlReservedWordsJson").text());
      var b = JIRA.JQLAutoComplete({
        fieldID: a.attr("id"),
        parser: JIRA.JQLAutoComplete.MyParser(g),
        queryDelay: 0.65,
        jqlFieldNames: c,
        jqlFunctionNames: f,
        minQueryLength: 0,
        allowArrowCarousel: true,
        autoSelectFirst: false,
        errorID: d
      });
      jQuery("#" + d).show();
      a.expandOnInput();
      b.buildResponseContainer();
      b.parse(a.text());
      b.updateColumnLineCount();
      a.keypress(function (i) {
        if (b.dropdownController === null || !b.dropdownController.displayed
            || b.selectedIndex < 0) {
          if (i.keyCode == 13 && !i.ctrlKey && !i.shiftKey) {
            i.preventDefault();
            b.dropdownController.hideDropdown()
          }
        }
      }).bind("expandedOnInput", function () {
        b.positionResponseContainer()
      }).click(function () {
        b.dropdownController.hideDropdown()
      }).bind("webhooks.valueChanged", function () {
        b.parse(a.val())
      })
    };
  })