AJS.namespace("reminders");
AJS.$(function () {
    window.RemindersGroupPickerMulti = AJS.MultiSelect.extend({
        init: function (a) {
            function b(c) {
                if (!c.sections) {
                    return []
                }
                var d = [];
                AJS.$(c.sections).each(function (e, g) {
                    var f = new AJS.GroupDescriptor({label: g.label});
                    AJS.$(g.filters).each(function (h, j) {
                        f.addItem(new AJS.ItemDescriptor({value: j.name, label: j.name, html: j.name}))
                    });
                    d.push(f)
                });
                return d
            }

            AJS.$.extend(a, {
                errorMessage: AJS.I18n.getText("com.deniz.jira.reminders.group.missing"),
                ajaxOptions: {
                    url: contextPath + "/rest/com.deniz.jira.reminders/1.0/groups",
                    query: true,
                    formatResponse: b
                }
            });
            this._super(a)
        }
    })
});