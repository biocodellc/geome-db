/* ====== General Utility Functions ======= */
var appRoot = "/dipnet/";
var RestRoot = "/dipnet/rest/v1.1/";

$.ajaxSetup({
    beforeSend: function(jqxhr, config) {
        jqxhr.config = config;
        var dipnetSessionStorage = JSON.parse(window.sessionStorage.dipnet);
        var accessToken = dipnetSessionStorage.accessToken;
        if (accessToken && config.url.indexOf("access_token") == -1) {
            if (config.url.indexOf('?') > -1) {
                config.url += "&access_token=" + accessToken;
            } else {
                config.url += "?access_token=" + accessToken;
            }
        }
    }
});

$.ajaxPrefilter(function(opts, originalOpts, jqXHR) {
    // you could pass this option in on a "retry" so that it doesn't
    // get all recursive on you.
    if (opts.refreshRequest) {
        return;
    }

    if (opts.url.indexOf('/validate') > -1) {
        return;
    }

    if (typeof(originalOpts) != "object") {
        originalOpts = opts;
    }

    // our own deferred object to handle done/fail callbacks
    var dfd = $.Deferred();

    // if the request works, return normally
    jqXHR.done(dfd.resolve);

    // if the request fails, do something else
    // yet still resolve
    jqXHR.fail(function() {
        var args = Array.prototype.slice.call(arguments);
        var dipnetSessionStorage = JSON.parse(window.sessionStorage.dipnet);
        var refreshToken = dipnetSessionStorage.refreshToken;
        if ((jqXHR.status === 401 || (jqXHR.status === 400 && jqXHR.responseJSON.usrMessage == "invalid_grant"))
                && !isTokenExpired() && refreshToken) {
            $.ajax({
                url: RestRoot + 'authenticationService/oauth/refresh',
                method: 'POST',
                refreshRequest: true,
                data: $.param({
                    client_id: client_id,
                    refresh_token: refreshToken
                }),
                error: function() {
                    window.sessionStorage.dipnet = JSON.stringify({});

                    // reject with the original 401 data
                    dfd.rejectWith(jqXHR, args);

                    if (!window.location.pathname == appRoot)
                        window.location = appRoot + "login";
                },
                success: function(data) {
                    var dipnetSessionStorage = {
                        accessToken: data.access_token,
                        refreshToken: data.refresh_token,
                        oAuthTimestamp: new Date().getTime()
                    };

                    window.sessionStorage.dipnet = JSON.stringify(dipnetSessionStorage);

                    // retry with a copied originalOpts with refreshRequest.
                    var newOpts = $.extend({}, originalOpts, {
                        refreshRequest: true,
                        url: originalOpts.url.replace(/access_token=.{20}/, "access_token=" + data.access_token)
                    });
                    // pass this one on to our deferred pass or fail.
                    $.ajax(newOpts).then(dfd.resolve, dfd.reject);
                }
            });

        } else {
            dfd.rejectWith(jqXHR, args);
        }
    });

    // NOW override the jqXHR's promise functions with our deferred
    return dfd.promise(jqXHR);
});

function isTokenExpired() {
    var dipnetSessionStorage = JSON.parse(window.sessionStorage.dipnet);
    var oAuthTimestamp = dipnetSessionStorage.oAuthTimestamp;
    var now = new Date().getTime();

    if (now - oAuthTimestamp > 1000 * 60 * 60 * 4)
        return true;

    return false;
}

// function for displaying a loading dialog while waiting for a response from the server
function loadingDialog(promise) {
    var dialogContainer = $("#dialogContainer");
    var msg = "Loading ...";
    dialog(msg, "", null);

    // close the dialog when the ajax call has returned only if the html is the same
    promise.always(function(){
        if (dialogContainer.html() == msg) {
            dialogContainer.dialog("close");
        }
    });
}

// function to retrieve a user's projects and populate the page
function listProjects(username, url, expedition) {
    var jqxhr = $.getJSON(url
    ).done(function(data) {
        if (!expedition) {
            var html = '<h3>Project Manager (' + username + ')</h3>\n';
        } else {
            var html = '<h3>Expedition Manager (' + username + ')</h3>\n';
        }
        var expandTemplate = '<br>\n<a class="expand-content" id="{project}-{section}" href="javascript:void(0);">\n'
            + '\t <img src="' + appRoot + 'images/right-arrow.png" id="arrow" class="img-arrow">{text}'
            + '</a>\n';
        $.each(data, function(index, element) {
            key=element.projectId;
            val=element.projectTitle;
            var project = val.replace(new RegExp('[#. ()]', 'g'), '_') + '_' + key;

            html += expandTemplate.replace('{text}', element.projectTitle).replace('-{section}', '');
            html += '<div id="{project}" class="toggle-content">';
            if (!expedition) {
                html += expandTemplate.replace('{text}', 'Project Metadata').replace('{section}', 'metadata').replace('<br>\n', '');
                html += '<div id="{project}-metadata" class="toggle-content">Loading Project Metadata...</div>';
                html += expandTemplate.replace('{text}', 'Project Expeditions').replace('{section}', 'expeditions');
                html += '<div id="{project}-expeditions" class="toggle-content">Loading Project Expeditions...</div>';
                html +=  expandTemplate.replace('{text}', 'Project Users').replace('{section}', 'users');
                html += '<div id="{project}-users" class="toggle-content">Loading Project Users...</div>';
            } else {
                html += 'Loading...';
            }
            html += '</div>\n';

            // add current project to element id
            html = html.replace(new RegExp('{project}', 'g'), project);
        });
        if (html.indexOf("expand-content") == -1) {
            if (!expedition) {
                html += 'You are not an admin for any project.';
            } else {
                html += 'You do not belong to any projects.'
            }
        }
        $(".sectioncontent").html(html);

        // store project id with element, so we don't have to retrieve project id later with an ajax call
        $.each(data, function(index, element) {
            key=element.projectId;
            val=element.projectTitle;
            var project = val.replace(new RegExp('[#. ()]', 'g'), '_') + '_' + key;

            if (!expedition) {
                $('div#' + project +'-metadata').data('projectId', key);
                $('div#' + project +'-users').data('projectId', key);
                $('div#' + project + '-expeditions').data('projectId', key);
            } else {
                $('div#' + project).data('projectId', key);
            }
        });
    }).fail(function(jqxhr) {
        $(".sectioncontent").html(jqxhr.responseText);
    });
    return jqxhr;

}

function getQueryParam(sParam) {
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    for (var i = 0; i < sURLVariables.length; i++) {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == sParam) {
            if (sParam == "return_to") {
                // if we want the return_to query param, we need to return everything after "return_to="
                // this is assuming that "return_to" is the last query param, which it should be
                return decodeURIComponent(sPageURL.slice(sPageURL.indexOf(sParameterName[1])));
            } else {
                return decodeURIComponent(sParameterName[1]);
            }
        }
    }
}

// Populate Div element from a REST service with HTML
function populateDivFromService(url,elementID,failMessage)  {
    if (elementID.indexOf('#') == -1) {
        elementID = '#' + elementID
    }
    return jqxhr = $.ajax(url, function() {})
        .done(function(data) {
            $(elementID).html(data);
        })
        .fail(function() {
            $(elementID).html(failMessage);
        });
}

function failError(jqxhr) {
    var buttons = {
        "OK": function(){
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
        }
    }
    $("#dialogContainer").addClass("error");

    var message;
    if (jqxhr.responseJSON) {
        message = jqxhr.responseJSON.usrMessage;
    } else {
        message = "Server Error!";
    }
    dialog(message, "Error", buttons);
}

// function to open a new or update an already open jquery ui dialog box
function dialog(msg, title, buttons) {
    var dialogContainer = $("#dialogContainer");
    if (dialogContainer.html() != msg) {
        dialogContainer.html(msg);
    }

    if (!$(".ui-dialog").is(":visible") || (dialogContainer.dialog("option", "title") != title ||
        dialogContainer.dialog("option" , "buttons") != buttons)) {
        dialogContainer.dialog({
            modal: true,
            autoOpen: true,
            title: title,
            resizable: false,
            width: 'auto',
            draggable: false,
            buttons: buttons,
            position: { my: "center top", at: "top", of: window}
        });
    }

    return;
}

// A short message
function showMessage(message) {
$('#alerts').append(
        '<div class="fims-alert">' +
            '<button type="button" class="close" data-dismiss="alert">' +
            '&times;</button>' + message + '</div>');
}

// A big message
function showBigMessage(message) {
$('#alerts').append(
        '<div class="fims-alert" style="height:400px">' +
            '<button type="button" class="close" data-dismiss="alert">' +
            '&times;</button>' + message + '</div>');
}

// Get the projectID
function getProjectID() {
    return $('#project').val();
}

/* ====== profile.jsp Functions ======= */

// function to submit the user's profile editor form
function profileSubmit(divId) {
    if ($("input.pwcheck", divId).val().length > 0 && $(".label", "#pwindicator").text() == "weak") {
        $(".error", divId).html("password too weak");
    } else if ($("input[name='newPassword']").val().length > 0 &&
        ($("input[name='oldPassword']").length > 0 && $("input[name='oldPassword']").val().length == 0)) {
        $(".error", divId).html("Old Password field required to change your Password");
    } else {
        var postURL = RestRoot + "users/profile/update/";
        var return_to = getQueryParam("return_to");
        if (return_to != null) {
            postURL += "?return_to=" + encodeURIComponent(return_to);
        }
        var jqxhr = $.post(postURL, $("form", divId).serialize(), 'json'
        ).done(function (data) {
            // if adminAccess == true, an admin updated the user's password, so no need to redirect
            if (data.adminAccess == true) {
                populateProjectSubsections(divId);
            } else {
                if (data.returnTo) {
                    $(location).attr("href", data.returnTo);
                } else {
                    var jqxhr2 = populateDivFromService(
                        RestRoot + "users/profile/listAsTable",
                        "listUserProfile",
                        "Unable to load this user's profile from the Server")
                        .done(function () {
                            $("a", "#profile").click(function () {
                                getProfileEditor();
                            });
                        });
                }
            }
        }).fail(function (jqxhr) {
            var json = $.parseJSON(jqxhr.responseText);
            $(".error", divId).html(json.usrMessage);
        });
    }
}

// get profile editor
function getProfileEditor(username) {
    var jqxhr = populateDivFromService(
        RestRoot + "users/profile/listEditorAsTable",
        "listUserProfile",
        "Unable to load this user's profile editor from the Server"
    ).done(function () {
        $(".error").text(getQueryParam("error"));
        $("#cancelButton").click(function () {
            var jqxhr2 = populateDivFromService(
                RestRoot + "users/profile/listAsTable",
                "listUserProfile",
                "Unable to load this user's profile from the Server")
                .done(function () {
                    $("a", "#profile").click(function () {
                        getProfileEditor();
                    });
                });
        });
        $("#profile_submit").click(function () {
            profileSubmit('div#listUserProfile');
        });
    });
}

/* ====== templates.html Functions ======= */

function showTemplateInfo() {
    if ($('#abstract').is(':hidden')) {
        $('#abstract').show(400);
    }
    if ($('#definition').is(':hidden')) {
        $('#definition').show(400);
    }
    if ($('#cat1').is(':hidden')) {
        $('#cat1').show(400);
    }
}

function populate_bottom(){
    var selected = new Array();
    var listElement = document.createElement("ul");
    listElement.className = 'picked_tags';
    $("#checked_list").html(listElement);
    $("input:checked").each(function() {
        var listItem = document.createElement("li");
        listItem.className = 'picked_tags_li';
        listItem.innerHTML = ($(this).val());
        listElement.appendChild(listItem);
    });
}

function download_file(){
    var url = RestRoot + 'projects/createExcel/';
    var input_string = '';
    // Loop through CheckBoxes and find ones that are checked
    $(".check_boxes").each(function(index) {
        if ($(this).is(':checked'))
            input_string+='<input type="hidden" name="fields" value="' + $(this).val() + '" />';
    });
    input_string+='<input type="hidden" name="projectId" value="' + getProjectID() + '" />';

    // Pass the form to the server and submit
    $('<form action="'+ url +'" method="post">'+input_string+'</form>').appendTo('body').submit().remove();
}

// for template generator, get the definitions when the user clicks on DEF
function populateDefinitions(column) {
    var projectId = getProjectID();

    theUrl = RestRoot + "projects/" + projectId + "/getDefinition/" + column;

    var jqxhr = $.ajax({
        type: "GET",
        url: theUrl,
        dataType: "html",
    })
    .done(function(data) {
        $("#definition").html(data);
    });
}

function populateColumns(targetDivId) {
    var projectId = getProjectID();

    if (projectId != 0) {
        theUrl = RestRoot + "projects/" + projectId + "/attributes/";

        var jqxhr = $.ajax( {
            url: theUrl,
            async: false,
            dataType : 'html'
        }).done(function(data) {
            $(targetDivId).html(data);
        }).fail(function(jqXHR,textStatus) {
            if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response!");
            } else {
                showMessage ("Error completing request!" );
            }
        });

        $(".def_link").click(function () {
            populateDefinitions($(this).attr('name'));
        });
        return jqxhr;
    }
}

var savedConfig;
function saveTemplateConfig() {
    var message = "<table><tr><td>Configuration Name:</td><td><input type='text' name='configName' /></td></tr></table>";
    var title = "Save Template Generator Configuration";
    var buttons = {
        "Save": function() {
            var checked = [];
            var configName = $("input[name='configName']").val();

            if (configName.toUpperCase() == "Default".toUpperCase()) {
                $("#dialogContainer").addClass("error");
                dialog("Talk to the project admin to change the default configuration.<br><br>" + message, title, buttons);
                return;
            }

            $("#cat1 input[type='checkbox']:checked").each(function() {
                checked.push($(this).data().uri);
            });

            savedConfig = configName;
            $.post(RestRoot + "projects/" + $("#project").val() + "/saveTemplateConfig", $.param(
                {"configName": configName,
                    "checkedOptions": checked,
                    "projectId": $("#project").val()
                }, true)
            ).done(function(data) {
                if (data.error != null) {
                    $("#dialogContainer").addClass("error");
                    var m = data.error + "<br><br>" + message;
                    dialog(m, title, buttons);
                } else {
                    $("#dialogContainer").removeClass("error");
                    populateConfigs();
                    var b = {
                        "Ok": function() {
                            $(this).dialog("close");
                        }
                    }
                    dialog(data.success + "<br><br>", "Success!", b);
                }
            }).fail(function(jqXHR) {
                failError(jqXHR);
            });
        },
        "Cancel": function() {
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
        }
    }

    dialog(message, title, buttons);
}

function populateConfigs() {
    var projectId = $("#project").val();
    if (projectId == 0) {
        $("#configs").html("<option value=0>Select a Project</option>");
    } else {
        var el = $("#configs");
        var jqxhr = $.getJSON(RestRoot + "projects/" + projectId + "/getTemplateConfigs").done(function(data) {
            var listItems = "";

            el.empty();
            data.forEach(function(configName) {
                el.append($("<option></option>").
                attr("value", configName).text(configName));
            });

            if (savedConfig != null) {
                $("#configs").val(savedConfig);
            }

            // if there are more then the default config, show the remove link
            if (data.length > 1) {
                $("#removeConfig").removeClass("hidden");
                $("#removeConfig").click(removeConfig);
            } else {
                $("#removeConfig").addClass("hidden");
            }

        }).fail(function(jqXHR,textStatus) {
            if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
            } else {
                showMessage ("Error fetching template configurations!");
            }
        });
    }
}

function updateCheckedBoxes() {
    var configName = $("#configs").val();
    if (configName == "Default") {
        populateColumns("#cat1");
    } else {
        $.getJSON(RestRoot + "projects/" + $("#project").val() + "/getTemplateConfig/" + configName.replace(/\//g, "%2F")).done(function(data) {
            if (data.error != null) {
                showMessage(data.error);
                return;
            }
            // deselect all unrequired columns
            $(':checkbox').not(":disabled").each(function() {
                this.checked = false;
            });

            data.checkedOptions.forEach(function(uri) {
                $(':checkbox[data-uri="' + uri + '"]')[0].checked = true;
            });
        }).fail(function(jqXHR, textStatus) {
            if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
            } else {
                showMessage ("Error fetching template configuration!");
            }
        });
    }
}

function removeConfig() {
    var configName = $("#configs").val();
    if (configName == "Default") {
        var buttons = {
            "Ok": function() {
                $(this).dialog("close");
            }
        }
        dialog("You can not remove the Default configuration", title, buttons);
        return;
    }

    var message = "Are you sure you want to remove "+ configName + " configuration?";
    var title = "Warning";
    var buttons = {
        "OK": function() {
            var buttons = {
                "Ok": function() {
                    $(this).dialog("close");
                }
            }
            var title = "Remove Template Generator Configuration";

            $.getJSON(RestRoot + "projects/" + $("#project").val() + "/removeTemplateConfig/" + configName.replace("/\//g", "%2F")).done(function(data) {
                if (data.error != null) {
                    showMessage(data.error);
                    return;
                }

                savedConfig = null;
                populateConfigs();
                dialog(data.success, title, buttons);
            }).fail(function(jqXHR) {
                failError(jqXHR);
            });
        },
        "Cancel": function() {
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
        }
    }

    dialog(message, title, buttons);
}

/* ====== validation.html Functions ======= */
// Function to display a list in a message
function list(listName, columnName) {
    var projectId = $("#project").val();
    $.getJSON(RestRoot + "projects/" + projectId + "/getListFields/" + listName)
    .done(function(data) {
        if (data.length == 0) {
            var msg = "No list has been defined for \"" + columnName + "\" but there is a rule saying it exists.  " +
                                          "Please talk to your FIMS data manager to fix this.";
            showBigMessage(msg);
        } else {
            var msg = '';
            if (columnName != null && columnName.length > 0) {
                msg += "<b>Acceptable values for " + columnName + "</b><br>\n";
            } else {
                    msg += "<b>Acceptable values for " + listName + "</b><br>\n";
            }

            $.each(data, function(index, value) {
                msg += "<li>" + value + "</li>\n";
            });
            showBigMessage(msg);
        }
    });
}

/* ====== query.html Functions ======= */

// create a form and then submit that form in order to download files
function download(url, data) {
    //url and data options are required
    if (url && data) {
        var form = $('<form />', { action: url, method: 'POST'});
        $.each(data, function(key, value) {
            // if the value is an array, we need to create an input element for each value
            if (value instanceof Array) {
                $.each(value, function(i, v) {
                    var input = $('<input />', {
                        type: 'hidden',
                        name: key,
                        value: v
                    }).appendTo(form);
                });
            } else {
                var input = $('<input />', {
                    type: 'hidden',
                    name: key,
                    value: value
                }).appendTo(form);
            }
        });

        return form.appendTo('body').submit().remove();
    }
    throw new Error("url and data required");
}
