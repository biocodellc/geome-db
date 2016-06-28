/* ====== General Utility Functions ======= */
var appRoot = "/";
var biocodeFimsRestRoot = "/biocode-fims/rest/";

$.ajaxSetup({
    beforeSend: function(jqxhr, config) {
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
                error: function() {
                    window.sessionStorage.biscicol = JSON.stringify({});

                    // reject with the original 401 data
                    dfd.rejectWith(jqXHR, args);

                    if (!window.location.pathname == appRoot)
                        window.location = appRoot + "login";
                },
                success: function(data) {
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
            var html = '<h1>Project Manager (' + username + ')</h2>\n';
        } else {
            var html = '<h1>Expedition Manager (' + username + ')</h2>\n';
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

function populateProjects() {
    theUrl = biocodeFimsRestRoot + "projects/list?includePublic=true";
    var jqxhr = $.getJSON( theUrl, function(data) {
        var listItems = "";
        listItems+= "<option value='0'>Select a project ...</option>";
        $.each(data, function(index, project) {
            listItems+= "<option value='" + project.projectId + "'>" + project.projectTitle + "</option>";
        });
        $("#projects").html(listItems);
        // Set to the first value in the list which should be "select one..."
        $("#projects").val($("#projects option:first").val());

    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
            showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
        } else {
            showMessage ("Error completing request!");
        }
    });
    return jqxhr;
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
    var e = document.getElementById('projects');
    return  e.options[e.selectedIndex].value;
}

/* ====== reset.jsp Functions ======= */

function resetSubmit() {
    var jqxhr = $.get(biocodeFimsRestRoot + "users/" + $("#username").val() + "/sendResetToken")
        .done(function(data) {
            if (data.success) {
                var buttons = {
                    "Ok": function() {
                        window.location.replace(appRoot);
                        $(this).dialog("close");
                    }
                }
                dialog(data.success, "Password Reset Sent", buttons);
                return;
            } else {
                failError(null);
            }
        }).fail(function(jqxhr) {
            failError(jqxhr);
        });
}

/* ====== resetPass.jsp Functions ======= */

// function to submit the reset password form
function resetPassSubmit() {
    var jqxhr = $.post(biocodeFimsRestRoot + "users/resetPassword/", $("#resetForm").serialize())
        .done(function(data) {
            if (data.success) {
                var buttons = {
                    "Ok": function() {
                        window.location.replace(appRoot);
                        $(this).dialog("close");
                    }
                }
                dialog(data.success, "Password Reset", buttons);
                return;
            } else {
                failError(null);
            }
        }).fail(function(jqxhr) {
            failError(jqxhr);
        });
}

/* ====== bcidCreator.jsp Functions ======= */

// Populate the SELECT box with resourceTypes from the server
function getResourceTypesMinusDataset(id) {
    var url = biocodeFimsRestRoot + "resourceTypes/minusDataset/";

    // get JSON from server and loop results
    var jqxhr = $.getJSON(url, function() {})
        .done(function(data) {
            var options = '<option value=0>Select a Concept</option>';
            $.each(data, function(key, val) {
                options+='<option value="' + val.resourceType + '">' + val.string + '</option>';
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
        .done(function(data) {
            var b = {
                "Ok": function() {
                    $('#bcidForm')[0].reset();
                    $(this).dialog("close");
                }
            }
            var msg = "Successfully created the bcid with identifier: " + data.identifier;
            dialog(msg, "Success creating bcid!", b);
        }).fail(function(jqxhr) {
            failError(jqxhr);
        });
}

/* ====== expeditions.html Functions ======= */

// function to populate the expeditions.html page
function populateExpeditionPage(username) {
    var jqxhr = listProjects(username, biocodeFimsRestRoot + 'projects/user/list', true
    ).done(function() {
        // attach toggle function to each project
        $(".expand-content").click(function() {
            loadExpeditions(this.id)
        });
    }).fail(function(jqxhr) {
        $("#sectioncontent").html(jqxhr.responseText);
    });
}

// function to load the expeditions.html subsections
function loadExpeditions(id) {
    if ($('.toggle-content#'+id).is(':hidden')) {
        $('.img-arrow', '#'+id).attr("src", appRoot + "images/down-arrow.png");
    } else {
        $('.img-arrow', '#'+id).attr("src", appRoot + "images/right-arrow.png");
    }
    // check if we've loaded this section, if not, load from service
    var divId = 'div#' + id
    if ((id.indexOf("resources") != -1 || id.indexOf("datasets") != -1 || id.indexOf("configuration") != -1) &&
        ($(divId).children().length == 0)) {
        populateExpeditionSubsections(divId);
    } else if ($(divId).children().length == 0) {
        listExpeditions(divId);
    }
    $('.toggle-content#'+id).slideToggle('slow');
}

// retrieve the expeditions for a project and display them on the page
function listExpeditions(divId) {
    var projectId = $(divId).data('projectId');
    var jqxhr = $.getJSON(biocodeFimsRestRoot + 'projects/' + projectId + '/expeditions/')
        .done(function(data) {
            var html = '';
            var expandTemplate = '<br>\n<a class="expand-content" id="{expedition}-{section}" href="javascript:void(0);">\n'
                + '\t <img src="' + appRoot + 'images/right-arrow.png" id="arrow" class="img-arrow">{text}'
                + '</a>\n';
            $.each(data, function(index, e) {
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
                html +=  expandTemplate.replace('{text}', 'Datasets associated with this expedition').replace('{section}', 'datasets');
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
            $.each(data, function(index, e) {
                var expedition = e.expeditionTitle.replace(new RegExp('[#. ():]', 'g'), '_') + '_' + e.expeditionId;
                while (!expedition.match(/^[a-zA-Z](.)*$/)) {
                    expedition = expedition.substring(1);
                }

                $('div#' + expedition +'-configuration').data('expeditionId', e.expeditionId);
                $('div#' + expedition +'-resources').data('expeditionId', e.expeditionId);
                $('div#' + expedition +'-datasets').data('expeditionId', e.expeditionId);
            });

            // remove previous click event and attach toggle function to each project
            $(".expand-content").off("click");
            $(".expand-content").click(function() {
                loadExpeditions(this.id);
            });
        }).fail(function(jqxhr) {
            $(divId).html(jqxhr.responseText);
        });
}

// function to populate the expedition resources, datasets, or configuration subsection of expeditions.html
function populateExpeditionSubsections(divId) {
    // load config table from REST service
    var expeditionId= $(divId).data('expeditionId');
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
        "Update": function() {
            var public = $("[name='public']")[0].checked;

            $.get(biocodeFimsRestRoot + "expeditions/updateStatus/" + projectId + "/" + expeditionCode + "/" + public
            ).done(function() {
                var b = {
                    "Ok": function() {
                        $(this).dialog("close");
                        location.reload();
                    }
                }
                dialog("Successfully updated the public status.", "Success!", b);
            }).fail(function(jqXHR) {
                $("#dialogContainer").addClass("error");
                var b= {
                    "Ok": function() {
                        $("#dialogContainer").removeClass("error");
                        $(this).dialog("close");
                    }
                }
                dialog("Error updating expedition's public status!<br><br>" + JSON.stringify($.parseJSON(jqxhr.responseText).usrMessage), "Error!", buttons)
            });
        },
        "Cancel": function() {
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
        ).done (function(data) {
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
                        .done(function() {
                            $("a", "#profile").click( function() {
                                getProfileEditor();
                            });
                        });
                }
            }
        }).fail(function(jqxhr) {
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
    ).done(function() {
        $(".error").text(getQueryParam("error"));
        $("#cancelButton").click(function() {
            var jqxhr2 = populateDivFromService(
                biocodeFimsRestRoot + "users/profile/listAsTable",
                "listUserProfile",
                "Unable to load this user's profile from the Server")
                .done(function() {
                    $("a", "#profile").click( function() {
                        getProfileEditor();
                    });
                });
        });
        $("#profile_submit").click(function() {
            profileSubmit('div#listUserProfile');
        });
    });
}

/* ====== projects.jsp Functions ======= */

// populate the metadata subsection of projects.jsp from REST service
function populateMetadata(id, projectID) {
    var jqxhr = populateDivFromService(
        biocodeFimsRestRoot + 'projects/' + projectID + '/metadata',
        id,
        'Unable to load this project\'s metadata from server.'
    ).done(function() {
        $("#edit_metadata", id).click(function() {
            var jqxhr2 = populateDivFromService(
                biocodeFimsRestRoot + 'projects/' + projectID + '/metadataEditor',
                id,
                'Unable to load this project\'s metadata editor.'
            ).done(function() {
                $('#metadataSubmit', id).click(function() {
                    projectMetadataSubmit(projectID, id);
                });
            });
        });
    });
    return jqxhr;
}

// show a confirmation dialog before removing a user from a project
function confirmRemoveUserDialog(element) {
    var username = $(element).data('username');
    var title = "Remove User";
    var msg = "Are you sure you wish to remove " + username + "?";
    var buttons = {
        "Yes": function() {
            projectRemoveUser(element);
            $(this).dialog("close");
        }, "Cancel": function() {
            $(this).dialog("close");
        }
    };
    dialog(msg, title, buttons);
}

// populate the users subsection of projects.jsp from REST service
function populateUsers(id, projectID) {
    var jqxhr = populateDivFromService(
        biocodeFimsRestRoot + 'projects/' + projectID + '/users',
        id,
        'Unable to load this project\'s users from server.'
    ).done(function() {
        $.each($('a#remove_user', id), function(key, e) {
            $(e).click(function() {
                confirmRemoveUserDialog(e);
            });
        });
        $.each($('a#edit_profile', id), function(key, e) {
            $(e).click(function() {
                var username = $(e).data('username');
                var divId = 'div#' + $(e).closest('div').attr('id');

                var jqxhr2 = populateDivFromService(
                    biocodeFimsRestRoot + "users/admin/profile/listEditorAsTable/" + username,
                    divId,
                    "error loading profile editor"
                ).done(function() {
                    $("#profile_submit", divId).click(function() {
                        profileSubmit(divId);
                    })
                    $("#cancelButton").click(function() {
                        populateProjectSubsections(divId);
                    })
                });


            });
        });
    });
    return jqxhr;
}

// function to populate the subsections of the projects.jsp page. Populates the metadata, expeditions, and users
// subsections
function populateProjectSubsections(id) {
    var projectID = $(id).data('projectId');
    var jqxhr;
    if (id.indexOf("metadata") != -1) {
        // load project metadata table from REST service
        jqxhr = populateMetadata(id, projectID);
    } else if (id.indexOf("users") != -1) {
        // load the project users table from REST service
        jqxhr = populateUsers(id, projectID);
    } else {
        // load the project expeditions table from REST service
        jqxhr = populateDivFromService(
            biocodeFimsRestRoot + 'projects/' + projectID + '/admin/expeditions/',
            id,
            'Unable to load this project\'s expeditions from server.'
        ).done(function() {
            $('#expeditionForm', id).click(function() {
                expeditionsPublicSubmit(id);
            });
        });
    }
    return jqxhr;
}

// function to submit the project's expeditions form. used to update the expedition public attribute.
function expeditionsPublicSubmit(divId) {
    var inputs = $('form input[name]', divId);
    var data = '';
    inputs.each( function(index, element) {
        if (element.name == 'projectId') {
            data += '&projectId=' + element.value;
            return true;
        }
        var expedition = '&' + element.name + '=' + element.checked;
        data += expedition;
    });
    var jqxhr = $.post(biocodeFimsRestRoot + 'expeditions/admin/updateStatus', data.replace('&', '')
    ).done(function() {
        populateProjectSubsections(divId);
    }).fail(function(jqxhr) {
        $(divId).html(jqxhr.responseText);
    });
}

// function to add an existing user to a project or retrieve the create user form.
function projectUserSubmit(id) {
    var divId = 'div#' + id + "-users";
    var projectId = $("input[name='projectId']", divId).val()
    if ($('select option:selected', divId).val() == 0) {
        var jqxhr = populateDivFromService(
            biocodeFimsRestRoot + 'users/admin/createUserForm',
            divId,
            'error fetching create user form'
        ).done(function() {
            $("input[name=projectId]", divId).val(projectId);
            $("#createFormButton", divId).click(function() {
                createUserSubmit(projectId, divId);
            });
            $("#createFormCancelButton", divId).click(function() {
                populateProjectSubsections(divId);
            });
        });
    } else {
        var jqxhr = $.post(biocodeFimsRestRoot + "projects/" + projectId + "/admin/addUser", $('form', divId).serialize()
        ).done(function(data) {
            var jqxhr2 = populateProjectSubsections(divId);
        }).fail(function(jqxhr) {
            var jqxhr2 = populateProjectSubsections(divId);
            $(".error", divId).html($.parseJSON(jqxhr.responseText).usrMessage);
        });
    }
}

// function to submit the create user form.
function createUserSubmit(projectId, divId) {
    if ($(".label", "#pwindicator").text() == "weak") {
        $(".error", divId).html("password too weak");
    } else {
        var jqxhr = $.post(biocodeFimsRestRoot + "users/admin/create", $('form', divId).serialize()
        ).done(function() {
            populateProjectSubsections(divId);
        }).fail(function(jqxhr) {
            $(".error", divId).html($.parseJSON(jqxhr.responseText).usrMessage);
        });
    }
}

// function to remove the user as a member of a project.
function projectRemoveUser(e) {
    var userId = $(e).data('userid');
    var projectId = $(e).closest('table').data('projectid');
    var divId = 'div#' + $(e).closest('div').attr('id');

    var jqxhr = $.getJSON(biocodeFimsRestRoot + "projects/" + projectId + "/admin/removeUser/" + userId
    ).done (function(data) {
        var jqxhr2 = populateProjectSubsections(divId);
    }).fail(function(jqxhr) {
        var jqxhr2 = populateProjectSubsections(divId)
            .done(function() {
                $(".error", divId).html($.parseJSON(jqxhr.responseText).usrMessage);
            });
    });
}

// function to submit the project metadata editor form
function projectMetadataSubmit(projectId, divId) {
    var jqxhr = $.post(biocodeFimsRestRoot + "projects/" + projectId + "/metadata/update", $('form', divId).serialize()
    ).done(function(data) {
        populateProjectSubsections(divId);
    }).fail(function(jqxhr) {
        $(".error", divId).html($.parseJSON(jqxhr.responseText).usrMessage);
    });
}

// function to populate the bcid projects.jsp page
function populateProjectPage(username) {
    var jqxhr = listProjects(username, biocodeFimsRestRoot + 'projects/admin/list', false
    ).done(function() {
        // attach toggle function to each project
        $(".expand-content").click(function() {
            projectToggle(this.id)
        });
    });
}

// function to apply the jquery slideToggle effect.
function projectToggle(id) {
    // escape special characters in id field
    id = id.replace(/([!@#$%^&*()+=\[\]\\';,./{}|":<>?~_-])/g, "\\$1");
    // store the element value in a field
    var idElement = $('.toggle-content#'+id);
    if (idElement.is(':hidden')) {
        $('.img-arrow', 'a#'+id).attr("src", appRoot + "images/down-arrow.png");
    } else {
        $('.img-arrow', 'a#'+id).attr("src", appRoot + "images/right-arrow.png");
    }
    // check if we've loaded this section, if not, load from service
    var divId = 'div#' + id
    if ((id.indexOf("metadata") != -1 || id.indexOf("users") != -1 || id.indexOf("expeditions") != -1) &&
        ($(divId).children().length == 0 || $('#submitForm', divId).length !== 0)) {
        populateProjectSubsections(divId);
    }
    $(idElement).slideToggle('slow');
}

/* ====== templates.html Functions ======= */
function hideTemplateInfo() {
    if (!$('#abstract').is(':hidden')) {
        $('#abstract').hide(400);
    }
    if (!$('#definition').is(':hidden')) {
        $('#definition').hide(400);
    }
    if (!$('#cat1').is(':hidden')) {
        $('#cat1').hide(400);
    }
}

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
    var url = biocodeFimsRestRoot + 'projects/createExcel/';
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
    var e = document.getElementById('projects');
    var projectId = e.options[e.selectedIndex].value;

    theUrl = biocodeFimsRestRoot + "projects/" + projectId + "/getDefinition/" + column;

    var jqxhr = $.ajax({
        type: "GET",
        url: theUrl,
        dataType: "html",
    })
    .done(function(data) {
        $("#definition").html(data);
    }).fail(function(jqXHR, textStatus) {
        hideTemplateInfo();
        failError(jqXHR);
    });
}

function populateColumns(targetDivId) {
    var e = document.getElementById('projects');
    var projectId = e.options[e.selectedIndex].value;

    if (projectId != 0) {
        theUrl = biocodeFimsRestRoot + "projects/" + projectId + "/attributes/";

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

    var jqxhr = $.ajax( {
        url: theUrl,
        async: false,
        dataType : 'json'
    }).done(function(data) {
        $(targetDivId).html(unescape(data.abstract) +"<p>");
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
            showMessage ("Timed out waiting for response!");
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
            $.post(biocodeFimsRestRoot + "projects/" + $("#projects").val() + "/saveTemplateConfig", $.param(
                {"configName": configName,
                    "checkedOptions": checked,
                    "projectId": $("#projects").val()
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
    var projectId = $("#projects").val();
    if (projectId == 0) {
        $("#configs").html("<option value=0>Select a Project</option>");
    } else {
        var el = $("#configs");
        el.empty();
        el.append($("<option></option>").attr("value", 0).text("Loading configs..."));
        var jqxhr = $.getJSON(biocodeFimsRestRoot + "projects/" + projectId + "/getTemplateConfigs").done(function(data) {
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
                if ($('.toggle-content#remove_config_toggle').is(':hidden')) {
                    $('.toggle-content#remove_config_toggle').show(400);
                }
            } else {
                if (!$('.toggle-content#remove_config_toggle').is(':hidden')) {
                    $('.toggle-content#remove_config_toggle').hide();
                }
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
        $.getJSON(biocodeFimsRestRoot + "projects/" + $("#projects").val() + "/getTemplateConfig/" + configName.replace(/\//g, "%2F")).done(function(data) {
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

            $.getJSON(biocodeFimsRestRoot + "projects/" + $("#projects").val() + "/removeTemplateConfig/" + configName.replace("/\//g", "%2F")).done(function(data) {
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
var datasetId;
var fastaId;

// Function to display a list in a message
function list(listName, columnName) {
    var projectId = $("#projects").val();
    $.getJSON(biocodeFimsRestRoot + "projects/" + projectId + "/getListFields/" + listName)
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

function parseSpreadsheet(regExpression, sheetName) {
    try {
        f = new FileReader();
    } catch(err) {
        return null;
    }
    var deferred = new $.Deferred();
    // older browsers don't have a FileReader
    if (f != null) {
        var inputFile= $('#' + datasetId)[0].files[0];

        var splitFileName = $('#' + datasetId).val().split('.');
        if ($.inArray(splitFileName[splitFileName.length - 1], XLSXReader.exts) > -1) {
            $.when(XLSXReader.utils.findCell(inputFile, regExpression, sheetName)).done(function(match) {
                if (match) {
                    deferred.resolve(match.toString().split('=')[1].slice(0, -1));
                } else {
                    deferred.resolve(null);
                }
            });
            return deferred.promise();
        }
    }
    setTimeout(function(){deferred.resolve(null)}, 100);
    return deferred.promise();

}

// Get the projectId for a key/value expression
function getProjectKeyValue() {
    return "projectId=" + getProjectID();
}

// Check that the validation form has a project id and if uploading, has an expedition code
function validForm(expeditionCode) {
    var message;
    var error = false;
    var dRE = /^[a-zA-Z0-9_-]{4,50}$/


    if (!$("#" + datasetId).val() && !$("#" + fastaId).val()) {
        message = "Please provide a dataset or fasta file";
        error = true;
    } else if ($('#projects').val() == 0) {
        message = "Please select a project.";
        error = true;
    } else if ($("#upload").is(":checked")) {
        // check if expeditionCode is a select and val = 0
        if (!$("#" + datasetId).val() && $("#" + fastaId).val() &&
            $("#expeditionCode").is("select") && $("#expeditionCode").val() == 0) {
            message = "You must select an existing expedition code if you are not uploading a new dataset.";
            error = true;
            // if it doesn't pass the regexp test, then set error message and set error to true
        } else if (!dRE.test(expeditionCode)) {
            message = "<b>Expedition Code</b> must contain only numbers, letters, or underscores and be 4 to 50 characters long";
            error = true;
        }
    } else if ($("#" + fastaId).val().length > 1 && $("#" + datasetId).val().length < 1 && !$("#upload").is(":checked")) {
        // only a fasta file is present, no dataset, and upload isn't selected
        message = "You can only upload fasta files";
        error = true;
    }

    if (error) {
        $('#resultsContainer').html(message);
        var buttons = {
            "OK": function(){
                $(this).dialog("close");
            }
        }
        dialog(message, "Validation Results", buttons);
        return false;
    }

    return true;
}

// submit dataset to be validated/uploaded
function validatorSubmit() {
    // User wants to create a new expedition
    if ($("#upload").is(":checked") && $("#expeditionCode").val() == 0) {
        createExpedition().done(function (e) {

            if (validForm(e)) {
                $("#" + datasetId).attr("name","dataset");
                $("#" + fastaId).attr("name","fasta");

                // if the form is valid, update the dataset code value
                $("#expeditionCode").replaceWith("<input name='expeditionCode' id='expeditionCode' type='text' value=" + e + " />");

                // Submit form
                submitForm().done(function(data) {
                    validationResults(data);
                }).fail(function(jqxhr) {
                    failError(jqxhr);
                });
            }
        })
    } else if (validForm($("#expeditionCode").val())) {

        // change the input names to the appropriate file type
        $("#" + datasetId).attr("name","dataset");
        $("#" + fastaId).attr("name","fasta");

        submitForm().done(function(data) {
            validationResults(data);
        }).fail(function(jqxhr) {
            failError(jqxhr);
        });
    }
}

// keep looping pollStatus every second until results are returned
function loopStatus(promise) {
    setTimeout( function() {
        pollStatus()
            .done(function(data) {
                if (promise.state() == "pending") {
                    if (data.error != null) {
                        dialog(data.error, "Validation Results");
                    } else {
                        dialog(data.status, "Validation Results");
                    }
                    loopStatus(promise);
                }
            });
    }, 1000);
}

// poll the server to get the validation/upload status
function pollStatus() {
    var def = new $.Deferred();
    $.getJSON(biocodeFimsRestRoot + "validate/status")
        .done(function(data) {
            def.resolve(data);
        }).fail(function() {
            def.reject();
        });
    return def.promise();
}

// Continue the upload process after getting user consent if there were warnings during validation or if we are creating
// a new expedition
function continueUpload(createExpedition) {
    var d = new $.Deferred();
    var url = biocodeFimsRestRoot + "validate/continue";
    if (createExpedition) {
        url += "?createExpedition=true";
    }
    $.getJSON(url)
        .done(function(data) {
            d.resolve();
            uploadResults(data);
        }).fail(function(jqxhr) {
            d.reject();
            failError(jqxhr);
        });
    loopStatus(d.promise());
}

// function to verify naan's
function checkNAAN(spreadsheetNaan, naan) {
    if (spreadsheetNaan != naan) {
        var buttons = {
            "Ok": function() {
                $("#dialogContainer").removeClass("error");
                $(this).dialog("close");
            }
        }
        var message = "Spreadsheet appears to have been created using a different FIMS/BCID system.<br>";
        message += "Spreadsheet says NAAN = " + spreadsheetNaan + "<br>";
        message += "System says NAAN = " + naan + "<br>";
        message += "Proceed only if you are SURE that this spreadsheet is being called.<br>";
        message += "Otherwise, re-load the proper FIMS system or re-generate your spreadsheet template.";

        dialog(message, "NAAN check", buttons);
    }
}

// function to toggle the projectId and expeditionCode inputs of the validation form
function validationFormToggle() {
    $("input[name=file]").change(function() {
        var splitFileName = $(this).val().split('.');
        var ext = splitFileName[splitFileName.length - 1];
        var datasetExts = ["xls", "xlsx", "txt"];

        // Clear the resultsContainer
        $("#resultsContainer").empty();

        if ($.inArray(ext, datasetExts) >= 0) {

            // always need to set dataset and fasta id's
            datasetId = this.id;
            if (this.id == "file1") {
                fastaId = "file2";
            } else {
                fastaId = "file1";
            }

            // Check NAAN
            $.when(parseSpreadsheet("~naan=[0-9]+~", "Instructions")).done(function(spreadsheetNaan) {
                if (spreadsheetNaan > 0) {
                    $.getJSON(biocodeFimsRestRoot + "utils/getNAAN")
                        .done(function(data) {
                            checkNAAN(spreadsheetNaan, data.naan);
                        });
                }
            });

            $.when(parseSpreadsheet("~project_id=[0-9]+~", "Instructions")).done(function(projectId) {
                if ($("#projects option[value='" + projectId + "']").length !== 0) {
                    $('#projects').val(projectId);
                    $('#projects').prop('disabled', true);
                    $('#includePublic').prop('disabled', true);
                    $('#projects').trigger("change");
                    if ($('.toggle-content#projects_toggle').is(':hidden')) {
                        $('.toggle-content#projects_toggle').show(400);
                    }
                } else {
                    var p = $("#projects");
                    // add a refresh map link incase the new dataset has the same project as the previous dataset. In that
                    // case, the user won't change projects and needs to manually refresh the map
                    if (p.val() != 0 && $("#refresh_map").length == 0) {
                        p.parent().append("<a id='refresh_map' href='#' onclick=\"generateMap('map', " + p.val() + ")\">Refresh Map</a>");
                    }
                    p.prop('disabled', false);
                    $('#includePublic').prop('disabled', false);
                    if (!$("#" + datasetId).val()) {
                        $(".toggle-content#projects_toggle").hide(400);
                    } else if ($('.toggle-content#projects_toggle').is(':hidden')) {
                        $('.toggle-content#projects_toggle').show(400);
                    }
                }
            });
        } else if (ext.toLowerCase() == "fasta") {
            // always need to set dataset and fasta id's
            fastaId = this.id;
            if (this.id == "file1") {
                datasetId = "file2";
            } else {
                datasetId = "file1";
            }
        }

        if (!$("#" + datasetId).val()) {
            $("#projects").prop('disabled', false);
            $('#includePublic').prop('disabled', false);
            if ($('.toggle-content#projects_toggle').is(':hidden')) {
                $('.toggle-content#projects_toggle').show(400);
            }
        }

    });

    $('#upload').change(function() {
        if ($('#upload').is(":checked"))
            showUpload();
        else
            hideUpload();
    });

    $("#projects").change(function() {
        // generate the map if the dataset isn't empty
        if ($("#" + datasetId).val()) {
            generateMap('map', this.value);
        }

        // only get expedition codes if a user is logged in
        if (angular.element(document.body).injector().get('AuthFactory').isAuthenticated) {
            $("#expeditionCode").replaceWith("<p id='expeditionCode'>Loading ... </p>");
            getExpeditionCodes();
        }
    });

    $("#file_button").on("click", function() {
        if ($('.toggle-content#file_toggle').is(':hidden')) {
            $('.toggle-content#file_toggle').show(400);
            $('#file_button').html("-");
        } else {
            $('.toggle-content#file_toggle').hide(400);
            $('#file_button').html("+");
            $('#file2').val(null);
        }
    });
}

function showUpload() {
    if ($('.toggle-content#expedition_public_toggle').is(':hidden'))
        $('.toggle-content#expedition_public_toggle').show(400);

    if ($('.toggle-content#expeditionCode_toggle').is(':hidden'))
        $('.toggle-content#expeditionCode_toggle').show(400);

    if ($('.toggle-content#projects_toggle').is(':hidden'))
        $('.toggle-content#projects_toggle').show(400);
}

function hideUpload() {
    if (!$('.toggle-content#expedition_public_toggle').is(':hidden'))
        $('.toggle-content#expedition_public_toggle').hide(400);

    if (!$('.toggle-content#expeditionCode_toggle').is(':hidden'))
        $('.toggle-content#expeditionCode_toggle').hide(400);
}

// update the checkbox to reflect the expedition's public status
function updateExpeditionPublicStatus(expeditionList) {
    $('#expeditionCode').change(function() {
        var code = $('#expeditionCode').val();
        var public;
        $.each(expeditionList, function(key, e) {
            if (e.expeditionCode == code) {
                public = e.public;
                return false;
            }
        });
        if (public == 'true') {
            $('#public_status').prop('checked', true);
        } else {
            $('#public_status').prop('checked', false);
        }
    });
}

// get the expeditions codes a user owns for a project
function getExpeditionCodes() {
    var projectID = $("#projects").val();
    $.getJSON(biocodeFimsRestRoot + "projects/" + projectID + "/expeditions/")
        .done(function(data) {
            var select = "<select name='expeditionCode' id='expeditionCode' style='max-width:199px'>" +
                "<option value='0'>Create New Expedition</option>";
            $.each(data, function(key, e) {
                select += "<option value=" + e.expeditionCode + ">" + e.expeditionCode + " (" + e.expeditionTitle + ")</option>";
            });

            select += "</select>";
            $("#expeditionCode").replaceWith(select);
            updateExpeditionPublicStatus(data);
        }).fail(function(jqxhr) {
            var msg;
            var title = "Error!";
            if (jqxhr.status = 401) {
                msg = "Please login to load your expeditions.";
                title = "Warning!";
            } else {
                msg = JSON.stringify($.parseJSON(jqxhr.responseText).usrMessage);
                $("#dialogContainer").addClass("error");
            }

            $("#expeditionCode").replaceWith('<input type="text" name="expeditionCode" id="expeditionCode" />');
            var buttons = {
                "Ok": function() {
                    $("#dialogContainer").removeClass("error");
                    $(this).dialog("close");
                }
            }
            dialog("Error fetching expeditions!<br><br>" + msg, title, buttons)
        });
}

// function to handle the results from the rest service /biocode-fims/rest/validate
function validationResults(data) {
    $("#resultsContainer").html("");
    var title = "Validation Results";
    if (data.done != null) {
        $("#dialogContainer").dialog("close");
        writeResults(parseResults(data.done));
    } else {
        if (data.continue.message == "continue") {
            continueUpload(false);
        } else {
            // ask user if want to proceed
            var message = parseResults(data.continue);
            var buttons = {
                "Continue": function() {
                    continueUpload(false);
                },
                "Cancel": function() {
                    $(this).dialog("close");
                }
            }
            writeResults(message);
            dialog(message, title, buttons);
        }
    }
}

// function to handle the results from the rest service /biocode-fims/rest/validate/continue
function uploadResults(data) {
    var title = "Upload Results";
    if (data.done != null || data.error != null) {
        var message;
        if (data.done != null) {
            message = data.done.message;
            writeResults(message);
        } else {
            $("#dialogContainer").addClass("error");
            message = data.error;
        }
        var buttons = {
            "Ok": function() {
                $("#dialogContainer").removeClass("error");
                $(this).dialog("close");
            }
        }
        dialog(message, title, buttons);
        // reset the form to default state
        $('form').clearForm();
        $('.toggle-content#projects_toggle').hide(400);
        $('.toggle-content#expedition_public_toggle').hide(400);
        $('.toggle-content#expeditionCode_toggle').hide(400);
//        <!--$('.toggle-content#upload-toggle').hide(400);-->

    } else {
        // ask user if want to proceed
        var buttons = {
            "Continue": function() {
                continueUpload(true);
            },
            "Cancel": function() {
                $(this).dialog("close");
            }
        }
        dialog(data.continue.message, title, buttons);
    }
}

// loop through the messages
function loopMessages(level, groupMessage, messageArray) {
    var message = "<div id=\"expand\">";
    message += "<dl>";
    message += "<dt><div id='groupMessage' class='" + level + "'>" + level + ": " + groupMessage + "</div><dt>";
    $.each(messageArray, function(key, val) {
        message += "<dd>" + val + "</dd>";
    })
    message += "</dl></div>";
    return message;
}

// parse the JSONArray of validation messages
function parseResults(messages) {
    var message = "";

    // loop through the messages for each sheet
    $.each(messages, function(key, val) {
        $.each(val, function(sheetName, sheetMessages) {
            if (sheetMessages.errors.length > 0) {
                message += "<br>\t<b>Validation results on \"" + sheetName + "\" worksheet.</b>";
                message += "<br><b>1 or more errors found.  Must fix to continue. Click each message for details</b><br>";
            } else if (sheetMessages.warnings.length > 0) {
                message += "<br>\t<b>Validation results on \"" + sheetName + "\" worksheet.</b>";
                message += "<br><b>1 or more warnings found. Click each message for details</b><br>";
            } else {
                return false;
            }

            if (sheetMessages.errors.length > 0) {
                $.each(sheetMessages.errors, function(key, val) {
                    $.each(val, function(groupMessage, messageArray) {
                        message += loopMessages("Error", groupMessage, messageArray);
                    });
                });
            }
            if (sheetMessages.warnings.length > 0) {
                $.each(sheetMessages.warnings, function(key, val) {
                    $.each(val, function(groupMessage, messageArray) {
                        message += loopMessages("Warning", groupMessage, messageArray);
                    });
                });
            }
        });
    });
    return message;
}

// write results to the resultsContainer
function writeResults(message) {
    $("#resultsContainer").show();
    // Add some nice coloring
    message = message.replace(/Warning:/g,"<span style='color:orange;'>Warning:</span>");
    message = message.replace(/Error:/g,"<span style='color:red;'>Error:</span>");
    // set the project key for any projectId expressions... these come from the validator to call REST services w/ extra data
    message= message.replace(/projectId=/g,getProjectKeyValue());

    //$("#resultsContainer").html("<table><tr><td>" + message + "</td></tr></table>");
    $("#resultsContainer").html($("#resultsContainer").html() + message);
}

// If the user wants to create a new expedition, get the expedition code
function createExpedition() {
    var d = new $.Deferred();
    if ($("#" + datasetId).val().length < 1 && $("#" + fastaId).val().length > 1 &&
        $("#expeditionCode").is("select") && $("#expeditionCode").val() == 0) {
        dialog("You must select an existing expedition code if you are not uploading a new dataset.", "Select an Expedition",
            {"OK":function() {d.reject();$(this).dialog("close");}});
    } else {
        var message = "<input type='text' id='new_expedition' />";
        var buttons = {
            "Create": function() {
                d.resolve($("#new_expedition").val());
            },
            "Cancel": function() {
                d.reject();
                $(this).dialog("close");
            }
        }
        dialog(message, "Expedition Code", buttons);
    }
    return d.promise();
}

// function to submit the validation form using jquery form plugin to handle the file uploads
function submitForm(){
    var de = new $.Deferred();
    var promise = de.promise();
    var options = {
        url: biocodeFimsRestRoot + "validate/",
        type: "POST",
        contentType: "multipart/form-data",
        beforeSerialize: function(form, options) {
            $('#projects').prop('disabled', false);
        },
        beforeSubmit: function(form, options) {
            $('#projects').prop('disabled', true);
            dialog("Loading ...", "Validation Results", null);
            // For browsers that don't support the upload progress listener
            var xhr = $.ajaxSettings.xhr();
            if (!xhr.upload) {
                loopStatus(promise)
            }
        },
        error: function(jqxhr) {
            de.reject(jqxhr);
        },
        success: function(data) {
            de.resolve(data);
        },
        fail: function(jqxhr) {
            de.reject(jqxhr);
        },
        uploadProgress: function(event, position, total, percentComplete) {
            // For browsers that do support the upload progress listener
            if (percentComplete == 100) {
                loopStatus(promise)
            }
        }
    }

    $('form').ajaxSubmit(options);
    return promise;
}
/* ====== resourceTypes.jsp Functions ======= */

// Populate a table of data showing resourceTypes
function getResourceTypesTable(a) {
    var url = biocodeFimsRestRoot + "resourceTypes";
    var jqxhr = $.getJSON(url, function() {})
        .done(function(data) {
            var html = "<table><tr><td><b>Name/URI</b></td><td><b>Description</b></td></tr>";
            $.each(data, function(key, val) {
                if (!(val.string == "---")) {
                    html += "<tr><td><a href='" + val.uri + "'>" + val.string + "</a></td>";
                    html += "<td>" + val.description + "</td></tr>";
                }
            })
            $("#" + a).html(html);
        })
        .fail(function(jqxhr) {
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
    if ($("#projects").val() == 0)  {
        graphsMessage('Choose an project to see loaded spreadsheets');
        return;
    }
    theUrl = biocodeFimsRestRoot + "projects/" + projectId + "/graphs";
    var jqxhr = $.getJSON( theUrl, function(data) {
        // Check for empty object in response
        if (data.length == 0) {
            graphsMessage('No datasets found for this project');
        } else {
            var listItems = "";
            $.each(data, function(index,graph) {
                listItems+= "<option value='" + graph.graph + "'>" + graph.expeditionTitle + "</option>";
            });
            $("#graphs").html(listItems);
        }
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
            showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
        } else {
            showMessage ("Error completing request!");
        }
    });
}

// Get the query graph URIs
function getGraphURIs() {
    var graphs = [];
    $( "select#graphs option:selected" ).each(function() {
        graphs.push($(this).val());
    });
    return graphs;
}

// Get results as JSON
function queryJSON(params) {
    // serialize the params object using a shallow serialization
    var jqxhr = $.post(biocodeFimsRestRoot + "projects/query/json/", $.param(params, true))
        .done(function(data) {
            $("#resultsContainer").show();
            //alert('debugging queries now, will fix soon!');
            //alert(data);
            // TODO: remove distal from this spot
            distal(results,data);
        }).fail(function(jqXHR,textStatus) {
            if (textStatus == "timeout") {
                showMessage ("Timed out waiting for response! Try again later or reduce the number of graphs you are querying. If the problem persists, contact the System Administrator.");
            } else {
                showMessage ("Error completing request!");
            }
        });
    loadingDialog(jqxhr);
}

// Get results as Excel
function queryExcel(params) {
    showMessage ("Downloading results as an Excel document<br>this will appear in your browsers download folder.");
    download(biocodeFimsRestRoot + "projects/query/excel/", params);
}

// Get results as Excel
function queryKml(params) {
    showMessage ("Downloading results as an KML document<br>If Google Earth does not open you can point to it directly");
    download(biocodeFimsRestRoot + "projects/query/kml/", params);
}

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

// a select element with all of the filterable options. Used to add additional filter statements
var filterSelect = null;

// populate a select with the filter values of a given project
function getFilterOptions(projectId) {
    var jqxhr = $.getJSON(biocodeFimsRestRoot + "projects/" + projectId + "/filterOptions/")
        .done(function(data) {
            filterSelect = "<select id='uri' style='max-width:100px;'>";
            $.each(data, function(k, v) {
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
    $.each(filterKeys, function(index, e) {
        if (filterValues[index].value != "") {
            params[e.value] = filterValues[index].value;
        }
    });

    return params;
}
