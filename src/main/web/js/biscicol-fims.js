/* ====== General Utility Functions ======= */
var appRoot = "/";
var biocodeFimsRestRoot = "/biocode-fims/rest/v2/";

$.ajaxSetup({
    headers: { 'Fims-App': 'Biscicol-Fims' },
    beforeSend: function (jqxhr, config) {
        jqxhr.config = config;
        var biscicolSessionStorage = JSON.parse(window.sessionStorage.biscicol);
        var accessToken = biscicolSessionStorage.accessToken;
        if (accessToken && config.url.indexOf("access_token") == -1) {
            if (config.url.indexOf('?') > -1) {
                config.url += "&access_token=" + accessToken;
            } else {
                config.url += "?access_token=" + accessToken;
            }
        }
    }
});

$.ajaxPrefilter(function (opts, originalOpts, jqXHR) {
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
    jqXHR.fail(function () {
        var args = Array.prototype.slice.call(arguments);
        var biscicolSessionStorage = JSON.parse(window.sessionStorage.biscicol);
        var refreshToken = biscicolSessionStorage.refreshToken;
        if ((jqXHR.status === 401 || (jqXHR.status === 400 && jqXHR.responseJSON.usrMessage == "invalid_grant"))
            && !isTokenExpired() && refreshToken) {
            $.ajax({
                url: biocodeFimsRestRoot + 'authenticationService/oauth/refresh',
                method: 'POST',
                refreshRequest: true,
                data: $.param({
                    client_id: client_id,
                    refresh_token: refreshToken
                }),
                error: function () {
                    window.sessionStorage.biscicol = JSON.stringify({});

                    // reject with the original 401 data
                    dfd.rejectWith(jqXHR, args);

                    if (!window.location.pathname == appRoot)
                        window.location = appRoot + "login";
                },
                success: function (data) {
                    var biscicolSessionStorage = {
                        accessToken: data.access_token,
                        refreshToken: data.refresh_token,
                        oAuthTimestamp: new Date().getTime()
                    };

                    window.sessionStorage.biscicol = JSON.stringify(biscicolSessionStorage);

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
    var biscicolSessionStorage = JSON.parse(window.sessionStorage.biscicol);
    var oAuthTimestamp = biscicolSessionStorage.oAuthTimestamp;
    var now = new Date().getTime();

    if (now - oAuthTimestamp > 1000 * 60 * 60 * 4)
        return true;

    return false;
}

// function to retrieve a user's projects and populate the page
function listProjects(username, url, expedition) {
    var jqxhr = $.getJSON(url
    ).done(function (data) {
        if (!expedition) {
            var html = '<h1>Project Manager (' + username + ')</h2>\n';
        } else {
            var html = '<h1>Expedition Manager (' + username + ')</h2>\n';
        }
        var expandTemplate = '<br>\n<a class="expand-content" id="{project}-{section}" href="javascript:void(0);">\n'
            + '\t <img src="' + appRoot + 'images/right-arrow.png" id="arrow" class="img-arrow">{text}'
            + '</a>\n';
        $.each(data, function (index, element) {
            key = element.projectId;
            val = element.projectTitle;
            var project = val.replace(new RegExp('[#. ()]', 'g'), '_') + '_' + key;

            html += expandTemplate.replace('{text}', element.projectTitle).replace('-{section}', '');
            html += '<div id="{project}" class="toggle-content">';
            if (!expedition) {
                html += expandTemplate.replace('{text}', 'Project Metadata').replace('{section}', 'metadata').replace('<br>\n', '');
                html += '<div id="{project}-metadata" class="toggle-content">Loading Project Metadata...</div>';
                html += expandTemplate.replace('{text}', 'Project Expeditions').replace('{section}', 'expeditions');
                html += '<div id="{project}-expeditions" class="toggle-content">Loading Project Expeditions...</div>';
                html += expandTemplate.replace('{text}', 'Project Users').replace('{section}', 'users');
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
        $.each(data, function (index, element) {
            key = element.projectId;
            val = element.projectTitle;
            var project = val.replace(new RegExp('[#. ()]', 'g'), '_') + '_' + key;

            if (!expedition) {
                $('div#' + project + '-metadata').data('projectId', key);
                $('div#' + project + '-users').data('projectId', key);
                $('div#' + project + '-expeditions').data('projectId', key);
            } else {
                $('div#' + project).data('projectId', key);
            }
        });
    }).fail(function (jqxhr) {
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
function populateDivFromService(url, elementID, failMessage) {
    if (elementID.indexOf('#') == -1) {
        elementID = '#' + elementID
    }
    return jqxhr = $.ajax(url, function () {
    })
        .done(function (data) {
            $(elementID).html(data);
        })
        .fail(function () {
            $(elementID).html(failMessage);
        });
}

function failError(jqxhr) {
    var buttons = {
        "OK": function () {
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
        dialogContainer.dialog("option", "buttons") != buttons)) {
        dialogContainer.dialog({
            modal: true,
            autoOpen: true,
            title: title,
            resizable: false,
            width: 'auto',
            draggable: false,
            buttons: buttons,
            position: {my: "center top", at: "top", of: window}
        });
    }

    return;
}

// A short message
function showMessage(message) {
    $('#alerts').append(
        '<div class="fims-alert alert-dismissable">' +
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
    var e = document.getElementById('projects');
    return e.options[e.selectedIndex].value;
}

/* ====== bcidCreator.jsp Functions ======= */

// Populate the SELECT box with resourceTypes from the server
function getResourceTypesMinusDataset(id) {
    var url = biocodeFimsRestRoot + "resourceTypes/minusDataset/";

    // get JSON from server and loop results
    var jqxhr = $.getJSON(url, function () {
    })
        .done(function (data) {
            var options = '<option value=0>Select a Concept</option>';
            $.each(data, function (key, val) {
                options += '<option value="' + val.resourceType + '">' + val.string + '</option>';
            });
            $("#" + id).html(options);
            if (id == "adminProjects") {
                $("." + id).html(options);
            }
        });
    return jqxhr;
}

/** Process submit button for Data Group Creator **/
function bcidCreatorSubmit() {
    /* Send the data using post */
    var posting = $.post(biocodeFimsRestRoot + "bcids", $("#bcidForm").serialize())
        .done(function (data) {
            var b = {
                "Ok": function () {
                    $('#bcidForm')[0].reset();
                    $(this).dialog("close");
                }
            }
            var msg = "Successfully created the bcid with identifier: " + data.identifier;
            dialog(msg, "Success creating bcid!", b);
        }).fail(function (jqxhr) {
            failError(jqxhr);
        });
}

/* ====== expeditions.html Functions ======= */

// function to populate the expeditions.html page
function populateExpeditionPage(username) {
    var jqxhr = listProjects(username, biocodeFimsRestRoot + 'projects/user/list', true
    ).done(function () {
        // attach toggle function to each project
        $(".expand-content").click(function () {
            loadExpeditions(this.id)
        });
    }).fail(function (jqxhr) {
        $("#sectioncontent").html(jqxhr.responseText);
    });
}

// function to load the expeditions.html subsections
function loadExpeditions(id) {
    if ($('.toggle-content#' + id).is(':hidden')) {
        $('.img-arrow', '#' + id).attr("src", appRoot + "images/down-arrow.png");
    } else {
        $('.img-arrow', '#' + id).attr("src", appRoot + "images/right-arrow.png");
    }
    // check if we've loaded this section, if not, load from service
    var divId = 'div#' + id
    if ((id.indexOf("resources") != -1 || id.indexOf("datasets") != -1 || id.indexOf("configuration") != -1) &&
        ($(divId).children().length == 0)) {
        populateExpeditionSubsections(divId);
    } else if ($(divId).children().length == 0) {
        listExpeditions(divId);
    }
    $('.toggle-content#' + id).slideToggle('slow');
}

// retrieve the expeditions for a project and display them on the page
function listExpeditions(divId) {
    var projectId = $(divId).data('projectId');
    var jqxhr = $.getJSON(biocodeFimsRestRoot + 'projects/' + projectId + '/expeditions?user&includePrivate=true')
        .done(function (data) {
            var html = '';
            var expandTemplate = '<br>\n<a class="expand-content" id="{expedition}-{section}" href="javascript:void(0);">\n'
                + '\t <img src="' + appRoot + 'images/right-arrow.png" id="arrow" class="img-arrow">{text}'
                + '</a>\n';
            $.each(data, function (index, e) {
                var expedition = e.expeditionTitle.replace(new RegExp('[#. ():]', 'g'), '_') + '_' + e.expeditionId;
                // html id's must start with a letter
                while (!expedition.match(/^[a-zA-Z](.)*$/)) {
                    expedition = expedition.substring(1);
                }

                html += expandTemplate.replace('{text}', e.expeditionTitle).replace('-{section}', '');
                html += '<div id="{expedition}" class="toggle-content">';
                html += expandTemplate.replace('{text}', 'Expedition Metadata').replace('{section}', 'configuration').replace('<br>\n', '');
                html += '<div id="{expedition}-configuration" class="toggle-content">Loading Expedition Metadata...</div>';
                html += expandTemplate.replace('{text}', 'Expedition Resources').replace('{section}', 'resources');
                html += '<div id="{expedition}-resources" class="toggle-content">Loading Expedition Resources...</div>';
                html += expandTemplate.replace('{text}', 'Datasets associated with this expedition').replace('{section}', 'datasets');
                html += '<div id="{expedition}-datasets" class="toggle-content">Loading Datasets associated wih this expedition...</div>';
                html += '</div>\n';

                // add current project to element id
                html = html.replace(new RegExp('{expedition}', 'g'), expedition);
            });
            html = html.replace('<br>\n', '');
            if (html.indexOf("expand-content") == -1) {
                html += 'You have no datasets in this project.';
            }
            $(divId).html(html);
            $.each(data, function (index, e) {
                var expedition = e.expeditionTitle.replace(new RegExp('[#. ():]', 'g'), '_') + '_' + e.expeditionId;
                while (!expedition.match(/^[a-zA-Z](.)*$/)) {
                    expedition = expedition.substring(1);
                }

                $('div#' + expedition + '-configuration').data('expeditionId', e.expeditionId);
                $('div#' + expedition + '-resources').data('expeditionId', e.expeditionId);
                $('div#' + expedition + '-datasets').data('expeditionId', e.expeditionId);
            });

            // remove previous click event and attach toggle function to each project
            $(".expand-content").off("click");
            $(".expand-content").click(function () {
                loadExpeditions(this.id);
            });
        }).fail(function (jqxhr) {
            $(divId).html(jqxhr.responseText);
        });
}

// function to populate the expedition resources, datasets, or configuration subsection of expeditions.html
function populateExpeditionSubsections(divId) {
    // load config table from REST service
    var expeditionId = $(divId).data('expeditionId');
    if (divId.indexOf("resources") != -1) {
        var jqxhr = populateDivFromService(
            biocodeFimsRestRoot + 'expeditions/' + expeditionId + '/resourcesAsTable/',
            divId,
            'Unable to load this expedition\'s resources from server.');
    } else if (divId.indexOf("datasets") != -1) {
        var jqxhr = populateDivFromService(
            biocodeFimsRestRoot + 'expeditions/' + expeditionId + '/datasetsAsTable/',
            divId,
            'Unable to load this expedition\'s datasets from server.');
    } else {
        var jqxhr = populateDivFromService(
            biocodeFimsRestRoot + 'expeditions/' + expeditionId + '/metadataAsTable/',
            divId,
            'Unable to load this expedition\'s configuration from server.');
    }
}

// function to edit an expedition
function editExpedition(projectId, expeditionCode, e) {
    var currentPublic;
    var searchId = $(e).closest("div")[0].id.replace("-configuration", "");
    var title = "Editing " + $("a#" + searchId)[0].textContent.trim();

    if (e.parentElement.textContent.startsWith("yes")) {
        currentPublic = true;
    } else {
        currentPublic = false;
    }

    var message = "<table><tr><td>Public:</td><td><input type='checkbox' name='public'";
    if (currentPublic) {
        message += " checked='checked'";
    }
    message += "></td></tr></table>";

    var buttons = {
        "Update": function () {
            var isPublic = $("[name='public']")[0].checked;

            $.get(biocodeFimsRestRoot + "expeditions/updateStatus/" + projectId + "/" + expeditionCode + "/" + isPublic
            ).done(function () {
                var b = {
                    "Ok": function () {
                        $(this).dialog("close");
                        location.reload();
                    }
                }
                dialog("Successfully updated the public status.", "Success!", b);
            }).fail(function (jqXHR) {
                $("#dialogContainer").addClass("error");
                var b = {
                    "Ok": function () {
                        $("#dialogContainer").removeClass("error");
                        $(this).dialog("close");
                    }
                }
                dialog("Error updating expedition's public status!<br><br>" + JSON.stringify($.parseJSON(jqxhr.responseText).usrMessage), "Error!", buttons)
            });
        },
        "Cancel": function () {
            $(this).dialog("close");
        }
    }
    dialog(message, title, buttons);
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
        var postURL = biocodeFimsRestRoot + "users/profile/update/";
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
                        biocodeFimsRestRoot + "users/profile/listAsTable",
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
        biocodeFimsRestRoot + "users/profile/listEditorAsTable",
        "listUserProfile",
        "Unable to load this user's profile editor from the Server"
    ).done(function () {
        $(".error").text(getQueryParam("error"));
        $("#cancelButton").click(function () {
            var jqxhr2 = populateDivFromService(
                biocodeFimsRestRoot + "users/profile/listAsTable",
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

function populate_bottom() {
    var selected = new Array();
    var listElement = document.createElement("ul");
    listElement.className = 'picked_tags';
    $("#checked_list").html(listElement);
    $("input:checked").each(function () {
        var listItem = document.createElement("li");
        listItem.className = 'picked_tags_li';
        listItem.innerHTML = ($(this).val());
        listElement.appendChild(listItem);
    });
}

function download_file() {
    var url = biocodeFimsRestRoot + 'projects/createExcel/';
    var input_string = '';
    // Loop through CheckBoxes and find ones that are checked
    $(".check_boxes").each(function (index) {
        if ($(this).is(':checked'))
            input_string += '<input type="hidden" name="fields" value="' + $(this).val() + '" />';
    });
    input_string += '<input type="hidden" name="projectId" value="' + getProjectID() + '" />';

    // Pass the form to the server and submit
    $('<form action="' + url + '" method="post">' + input_string + '</form>').appendTo('body').submit().remove();
}

// for template generator, get the definitions when the user clicks on DEF
function populateDefinitions(column) {
    var e = document.getElementById('projects');
    var projectId = e.options[e.selectedIndex].value;

    theUrl = biocodeFimsRestRoot + "projects/" + projectId + "/getDefinition/" + column;

    var jqxhr = $.ajax({
        type: "GET",
        url: theUrl,
        dataType: "html"
    })
        .done(function (data) {
            $("#definition").html(data);
        }).fail(function (jqXHR, textStatus) {
            hideTemplateInfo();
            failError(jqXHR);
        });
}

function populateColumns(targetDivId) {
    var e = document.getElementById('projects');
    var projectId = e.options[e.selectedIndex].value;

    if (projectId != 0) {
        theUrl = biocodeFimsRestRoot + "projects/" + projectId + "/attributes/";

        var jqxhr = $.ajax({
            url: theUrl,
            async: false,
            dataType: 'html'
        }).done(function (data) {
            $(targetDivId).html(data);
        }).fail(function (jqXHR, textStatus) {
            if (textStatus == "timeout") {
                showMessage("Timed out waiting for response!");
            } else {
                hideTemplateInfo();
                failError(jqXHR);
            }
        });

        $(".def_link").click(function () {
            populateDefinitions($(this).attr('name'));
        });
    }
}

function populateAbstract(targetDivId) {
    $(targetDivId).html("Loading ...");

    var e = document.getElementById('projects');
    var projectId = e.options[e.selectedIndex].value;

    theUrl = biocodeFimsRestRoot + "projects/" + projectId + "/abstract/";

    var jqxhr = $.ajax({
        url: theUrl,
        async: false,
        dataType: 'json'
    }).done(function (data) {
        $(targetDivId).html(unescape(data.abstract) + "<p>");
    }).fail(function (jqXHR, textStatus) {
        if (textStatus == "timeout") {
            showMessage("Timed out waiting for response!");
        } else {
            hideTemplateInfo();
            failError(jqXHR);
        }
    });
}

var savedConfig;
function saveTemplateConfig() {
    var message = "<table><tr><td>Configuration Name:</td><td><input type='text' name='configName' /></td></tr></table>";
    var title = "Save Template Generator Configuration";
    var buttons = {
        "Save": function () {
            var checked = [];
            var configName = $("input[name='configName']").val();

            if (configName.toUpperCase() == "Default".toUpperCase()) {
                $("#dialogContainer").addClass("error");
                dialog("Talk to the project admin to change the default configuration.<br><br>" + message, title, buttons);
                return;
            }

            $("#cat1 input[type='checkbox']:checked").each(function () {
                checked.push($(this).data().uri);
            });

            savedConfig = configName;
            $.post(biocodeFimsRestRoot + "projects/" + $("#projects").val() + "/saveTemplateConfig", $.param(
                {
                    "configName": configName,
                    "checkedOptions": checked,
                    "projectId": $("#projects").val()
                }, true)
            ).done(function (data) {
                if (data.error != null) {
                    $("#dialogContainer").addClass("error");
                    var m = data.error + "<br><br>" + message;
                    dialog(m, title, buttons);
                } else {
                    $("#dialogContainer").removeClass("error");
                    populateConfigs();
                    var b = {
                        "Ok": function () {
                            $(this).dialog("close");
                        }
                    }
                    dialog(data.success + "<br><br>", "Success!", b);
                }
            }).fail(function (jqXHR) {
                failError(jqXHR);
            });
        },
        "Cancel": function () {
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
        }
    }

    dialog(message, title, buttons);
}

function populateConfigs() {
    var projectId = $("#projects").val();
    if (projectId == 0) {
        $("#configs").html("<option value=0>Select a Project</option>");
    } else {
        var el = $("#configs");
        el.empty();
        el.append($("<option></option>").attr("value", 0).text("Loading configs..."));
        var jqxhr = $.getJSON(biocodeFimsRestRoot + "projects/" + projectId + "/getTemplateConfigs").done(function (data) {
            var listItems = "";

            el.empty();
            data.forEach(function (configName) {
                el.append($("<option></option>").attr("value", configName).text(configName));
            });

            if (savedConfig != null) {
                $("#configs").val(savedConfig);
            }

            // if there are more then the default config, show the remove link
            if (data.length > 1) {
                if ($('.toggle-content#remove_config_toggle').is(':hidden')) {
                    $('.toggle-content#remove_config_toggle').show(400);
                }
            } else {
                if (!$('.toggle-content#remove_config_toggle').is(':hidden')) {
                    $('.toggle-content#remove_config_toggle').hide();
                }
            }

        }).fail(function (jqXHR, textStatus) {
            if (textStatus == "timeout") {
                showMessage("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
            } else {
                showMessage("Error fetching template configurations!");
            }
        });
    }
}

function updateCheckedBoxes() {
    var configName = $("#configs").val();
    if (configName == "Default") {
        populateColumns("#cat1");
    } else {
        $.getJSON(biocodeFimsRestRoot + "projects/" + $("#projects").val() + "/getTemplateConfig/" + configName.replace(/\//g, "%2F")).done(function (data) {
            if (data.error != null) {
                showMessage(data.error);
                return;
            }
            // deselect all unrequired columns
            $(':checkbox').not(":disabled").each(function () {
                this.checked = false;
            });

            data.checkedOptions.forEach(function (uri) {
                $(':checkbox[data-uri="' + uri + '"]')[0].checked = true;
            });
        }).fail(function (jqXHR, textStatus) {
            if (textStatus == "timeout") {
                showMessage("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
            } else {
                showMessage("Error fetching template configuration!");
            }
        });
    }
}

function removeConfig() {
    var configName = $("#configs").val();
    if (configName == "Default") {
        var buttons = {
            "Ok": function () {
                $(this).dialog("close");
            }
        }
        dialog("You can not remove the Default configuration", title, buttons);
        return;
    }

    var message = "Are you sure you want to remove " + configName + " configuration?";
    var title = "Warning";
    var buttons = {
        "OK": function () {
            var buttons = {
                "Ok": function () {
                    $(this).dialog("close");
                }
            }
            var title = "Remove Template Generator Configuration";

            $.getJSON(biocodeFimsRestRoot + "projects/" + $("#projects").val() + "/removeTemplateConfig/" + configName.replace("/\//g", "%2F")).done(function (data) {
                if (data.error != null) {
                    showMessage(data.error);
                    return;
                }

                savedConfig = null;
                populateConfigs();
                dialog(data.success, title, buttons);
            }).fail(function (jqXHR) {
                failError(jqXHR);
            });
        },
        "Cancel": function () {
            $("#dialogContainer").removeClass("error");
            $(this).dialog("close");
        }
    }

    dialog(message, title, buttons);
}

/* ====== validation.html Functions ======= */
// Function to display a list in a message
function list(listName, columnName) {
    var projectId = $("#projects").val();
    $.getJSON(biocodeFimsRestRoot + "projects/" + projectId + "/getListFields/" + listName)
        .done(function (data) {
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

                $.each(data, function (index, value) {
                    msg += "<li>" + value + "</li>\n";
                });
                showBigMessage(msg);
            }
        });
}
// Get the projectId for a key/value expression
function getProjectKeyValue() {
    return "projectId=" + getProjectID();
}



// loop through the messages
function loopMessages(level, groupMessage, messageArray) {
    var message = "<div id=\"expand\">";
    message += "<dl>";
    message += "<dt><div id='groupMessage' class='" + level + "'>" + level + ": " + groupMessage + "</div><dt>";
    $.each(messageArray, function (key, val) {
        message += "<dd>" + val + "</dd>";
    });
    message += "</dl></div>";
    return message;
}

// parse the JSONArray of validation messages
function parseResults(messages) {
    var message = "";

    if (messages.config) {
        message += "<br>\t<b>Invalid Project Configuration.</b>";
        message += "<br>\t<b>Please talk to your project administrator to fix the following error(s):</b><br><br>";

        $.each(messages.config.errors, function (groupMessage, messageArray) {
            message += loopMessages("Error", groupMessage, messageArray);
        })
    } else {
        // loop through the messages for each sheet
        $.each(messages.worksheets, function (sheetName, sheetMessages) {
            if (!$.isEmptyObject(sheetMessages.errors)) {
                message += "<br>\t<b>Validation results on \"" + sheetName + "\" worksheet.</b>";
                message += "<br><b>1 or more errors found.  Must fix to continue. Click each message for details</b><br>";
            } else if (!$.isEmptyObject(sheetMessages.warnings)) {
                message += "<br>\t<b>Validation results on \"" + sheetName + "\" worksheet.</b>";
                message += "<br><b>1 or more warnings found. Click each message for details</b><br>";
            } else {
                return false;
            }

            $.each(sheetMessages.errors, function (groupMessage, messageArray) {
                message += loopMessages("Error", groupMessage, messageArray);
            });

            $.each(sheetMessages.warnings, function (groupMessage, messageArray) {
                message += loopMessages("Warning", groupMessage, messageArray);
            });
        });
    }

    if (message.length == 0) {
        message = "<span style='color:green;'>Successfully Validated!</span>";
    }

    return message;
}

// write results to the resultsContainer
function writeResults(message) {
    $("#resultsContainer").show();
    // Add some nice coloring
    message = message.replace(/Warning:/g, "<span style='color:orange;'>Warning:</span>");
    message = message.replace(/Error:/g, "<span style='color:red;'>Error:</span>");
    // set the project key for any projectId expressions... these come from the validator to call REST services w/ extra data
    message = message.replace(/projectId=/g, getProjectKeyValue());

    //$("#resultsContainer").html("<table><tr><td>" + message + "</td></tr></table>");
    $("#resultsContainer").html($("#resultsContainer").html() + message);
}

/* ====== resourceTypes.jsp Functions ======= */

// Populate a table of data showing resourceTypes
function getResourceTypesTable(a) {
    var url = biocodeFimsRestRoot + "resourceTypes";
    var jqxhr = $.getJSON(url, function () {
    })
        .done(function (data) {
            var html = "<table><tr><td><b>Name/URI</b></td><td><b>Description</b></td></tr>";
            $.each(data, function (key, val) {
                if (!(val.string == "---")) {
                    html += "<tr><td><a href='" + val.uri + "'>" + val.string + "</a></td>";
                    html += "<td>" + val.description + "</td></tr>";
                }
            })
            $("#" + a).html(html);
        })
        .fail(function (jqxhr) {
            failError(jqxhr);
        });
}

/* ====== query.html Functions ======= */

// handle displaying messages/results in the graphs(spreadsheets) select list
function graphsMessage(message) {
    $('#graphs').empty();
    $('#graphs').append('<option data-qrepeat="g data" data-qattr="value g.graph; text g.expeditionTitle"></option>');
    $('#graphs').find('option').first().text(message);
}

// Get the graphs for a given projectId
function populateGraphs(projectId) {
    $("#resultsContainer").hide();
    // Don't let this progress if this is the first option, then reset graphs message
    if ($("#projects").val() == 0) {
        graphsMessage('Choose an project to see loaded spreadsheets');
        return;
    }
    theUrl = biocodeFimsRestRoot + "projects/" + projectId + "/graphs";
    var jqxhr = $.getJSON(theUrl, function (data) {
        // Check for empty object in response
        if (data.length == 0) {
            graphsMessage('No datasets found for this project');
        } else {
            var listItems = "";
            $.each(data, function (index, graph) {
                listItems += "<option value='" + graph.graph + "'>" + graph.expeditionTitle + "</option>";
            });
            $("#graphs").html(listItems);
        }
    }).fail(function (jqXHR, textStatus) {
        if (textStatus == "timeout") {
            showMessage("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
        } else {
            showMessage("Error completing request!");
        }
    });
}

// Get the query graph URIs
function getGraphURIs() {
    var graphs = [];
    $("select#graphs option:selected").each(function () {
        graphs.push($(this).val());
    });
    return graphs;
}

// Get results as Excel
function queryExcel(params) {
    showMessage("Downloading results as an Excel document<br>this will appear in your browsers download folder.");
    download(biocodeFimsRestRoot + "projects/query/excel/", params);
}

// Get results as Excel
function queryKml(params) {
    showMessage("Downloading results as an KML document<br>If Google Earth does not open you can point to it directly");
    download(biocodeFimsRestRoot + "projects/query/kml/", params);
}

// create a form and then submit that form in order to download files
function download(url, data) {
    //url and data options are required
    if (url && data) {
        var form = $('<form />', {action: url, method: 'POST'});
        $.each(data, function (key, value) {
            // if the value is an array, we need to create an input element for each value
            if (value instanceof Array) {
                $.each(value, function (i, v) {
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

// a select element with all of the filterable options. Used to add additional filter statements
var filterSelect = null;

// populate a select with the filter values of a given project
function getFilterOptions(projectId) {
    var jqxhr = $.getJSON(biocodeFimsRestRoot + "projects/" + projectId + "/filterOptions/")
        .done(function (data) {
            filterSelect = "<select id='uri' style='max-width:100px;'>";
            $.each(data, function (k, v) {
                filterSelect += "<option value=" + v.uri + ">" + v.column + "</option>";
            });

            filterSelect += "</select>";
        });
    return jqxhr;
}

// add additional filters to the query
function addFilter() {
    // change the method to post
    $("form").attr("method", "POST");

    var tr = "<tr>\n<td align='right'>AND</td>\n<td>\n";
    tr += filterSelect;
    tr += "<p style='display:inline;'>=</p>\n";
    tr += "<input type='text' name='filter_value' style='width:285px;' />\n";
    tr += "</td>\n";

    // insert another tr after the last filter option, before the submit buttons
    $("#uri").parent().parent().siblings(":last").before(tr);
}

// prepare a json object with the query POST params by combining the text and select inputs for each filter statement
function getQueryPostParams() {
    var params = {
        graphs: getGraphURIs(),
        project_id: getProjectID()
    }

    var filterKeys = $("select[id=uri]");
    var filterValues = $("input[name=filter_value]");

    // parse the filter keys and values and add them to the post params
    $.each(filterKeys, function (index, e) {
        if (filterValues[index].value != "") {
            params[e.value] = filterValues[index].value;
        }
    });

    return params;
}
