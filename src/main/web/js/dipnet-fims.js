/* ====== General Utility Functions ======= */
var appRoot = "/dipnet/";
var biocodeFimsRestRoot = "/biocode-fims/rest/";
var LoginRestRoot = "/dipnet/rest/";

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
                url: LoginRestRoot + 'authenticationService/oauth/refresh',
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
    var projectId = getProjectID();

    theUrl = biocodeFimsRestRoot + "projects/" + projectId + "/getDefinition/" + column;

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
                showMessage ("Error completing request!" );
            }
        });

        $(".def_link").click(function () {
            populateDefinitions($(this).attr('name'));
        });
        return jqxhr;
    }
}

function populateAbstract(targetDivId) {
    $(targetDivId).html("Loading ...");

    var projectId = getProjectID();

    theUrl = biocodeFimsRestRoot + "projects/" + projectId + "/abstract/";

    var jqxhr = $.ajax( {
        url: theUrl,
        async: false,
        dataType : 'json'
    }).done(function(data) {
        $(targetDivId).html(data.abstract +"<p>");
    }).fail(function(jqXHR,textStatus) {
        if (textStatus == "timeout") {
            showMessage ("Timed out waiting for response!");
        } else {
            showMessage ("Error completing request!" );
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
            $.post(biocodeFimsRestRoot + "projects/" + $("#project").val() + "/saveTemplateConfig", $.param(
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
        $.getJSON(biocodeFimsRestRoot + "projects/" + $("#project").val() + "/getTemplateConfig/" + configName.replace(/\//g, "%2F")).done(function(data) {
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

            $.getJSON(biocodeFimsRestRoot + "projects/" + $("#project").val() + "/removeTemplateConfig/" + configName.replace("/\//g", "%2F")).done(function(data) {
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
    if ($("#project").val() == 0)  {
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
