/** Process submit button for Data Group Creator **/
function dataGroupCreatorSubmit() {
    $( "#dataGroupCreatorResults" ).html( "Processing ..." );
    /* Send the data using post */
    var posting = $.post( "/id/groupService", $("#dataGroupForm").serialize() );
    results(posting,"#dataGroupCreatorResults");
}

function bcidEditorSubmit() {
    var posting = $.post( "/id/groupService/dataGroup/update", $("#bcidEditForm").serialize())
        .done(function(data) {
            populateBCIDPage();
        }).fail(function(jqxhr) {
            $(".error").html($.parseJSON(jqxhr.responseText).usrMessage);
        });
    loadingDialog(posting);
}

/** Process submit button for Data Group Creator **/
function expeditionCreatorSubmit() {
    $( "#expeditionCreatorResults" ).html( "Processing ..." );
    /* Send the data using post */
    var posting = $.post( "/id/expeditionService", $("#expeditionForm").serialize() );
    results(posting,"#expeditionCreatorResults");
}

/** Generic way to display results from creator functions, relies on standardized JSON **/
function results(posting, a) {
    // Put the results in a div
    posting.done(function( data ) {
        var content = "<table>";
        content += "<tr><th>Results</th></tr>"
        content += "<tr><td>"+ data.prefix +"</td></tr>";
        //$.each(data, function(k,v) {
        //    content += "<tr><td>"+v+"</td></tr>";
        //})
        content += "</table>";
        $( a ).html( content );
    });
   posting.fail(function(jqxhr) {
        $( a ).html( "<table><tr><th>System error, unable to perform function!!</th></tr><tr><td>" +
            $.parseJSON(jqxhr.responseText).usrMessage + "</td></tr></table>" );
   });
}

// Control functionality when a datasetList is activated in the creator page
// if it is not option 0, then need to look up values from server to fill
// in title and concept.
function datasetListSelector() {

    // Set values when the user chooses a particular dataset
    if ($("#datasetList").val() != 0) {
        // Construct the URL
        var url = "/id/groupService/metadata/" + $("#datasetList").val();
        // Initialize cells
        $("#resourceTypesMinusDatasetDiv").html("");
        $("#suffixPassThroughDiv").html("");
        $("#titleDiv").html("");
        $("#doiDiv").html("");

        // Get JSON response
        var jqxhr = $.getJSON(url, function() {})
            .done(function(data) {
                var options = '';
                $.each(data[0], function(keyData,valData) {
                    $.each(valData, function(key, val) {
                        // Assign values from server to JS field names
                        if (key == "resourceType")
                            $("#resourceTypesMinusDatasetDiv").html(val);
                        if (key == "bcidsSuffixPassThrough")
                            $("#suffixPassThroughDiv").html(val);
                        if (key == "title")
                            $("#titleDiv").html(val);
                        if (key == "doi")
                            $("#doiDiv").html(val);
                    });
                });
            });
        // Set styles
        var color = "#463E3F";
        $("#titleDiv").css("color",color);
        $("#resourceTypesMinusDatasetDiv").css("color",color);
        $("#suffixPassThroughDiv").css("color",color);
        $("#doiDiv").css("color",color);

    // Set the Creator Defaults
    } else {
        creatorDefaults();
    }
}

// Set default settings for the Creator Form settings
function creatorDefaults() {
    $("#titleDiv").html("<input id=title name=title type=textbox size=40>");
    $("#resourceTypesMinusDatasetDiv").html("<select name=resourceTypesMinusDataset id=resourceTypesMinusDataset class=''>");
    $("#suffixPassThroughDiv").html("<input type=checkbox id=suffixPassThrough name=suffixPassThrough checked=yes>");
    $("#doiDiv").html("<input type=textbox id=doi name=doi checked=yes>");

    $("#titleDiv").css("color","black");
    $("#resourceTypesMinusDatasetDiv").css("color","black");
    $("#suffixPassThroughDiv").css("color","black");
    $("#doiDiv").css("color","black");

    populateSelect("resourceTypesMinusDataset");
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

// Populate a table of data showing resourceTypes
function populateResourceTypes(a) {
    var url = "/id/elementService/resourceTypes";
    var jqxhr = $.ajax(url, function() {})
        .done(function(data) {
           $("#" + a).html(data);
        })
        .fail(function() {
            $("#" + a).html("Unable to load resourceTypes!");
        });
}

// Populate the SELECT box with resourceTypes from the server
function populateSelect(a) {
    // bcid Service Call
    var url = "/id/elementService/select/" + a;

    // get JSON from server and loop results
    var jqxhr = $.getJSON(url, function() {})
        .done(function(data) {
            var options = '';
            $.each(data, function(key, val) {
                options+='<option value="' + key + '">' +val + '</option>';
            });
            $("#" + a).html(options);
            if (a == "adminProjects") {
                $("." + a).html(options);
            }
        });
    return jqxhr;
}

// **
// Take the resolver results and populate a table
function resolverResults() {
    $.get("/biscicol/rest/" + $("#identifier").val()).done(function(data) {
        $("#results").html(data);
    }).fail(function(jqxhr) {
        var html;
        if (jqxhr.status == 400) {
            html = "Invalid identifier.";
        } else {
            html = "Failed to retrieve identifier information.";
        }
        $("#results").html(html);
    });
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

// **
// function to populate the bcid projects.jsp page
function populateProjectPage(username) {
    var jqxhr = listProjects(username, '/biscicol/rest/projects/admin/list', false
    ).done(function() {
        // attach toggle function to each project
        $(".expand-content").click(function() {
            projectToggle(this.id)
        });
    });
}

// **
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
                            + '\t <img src="/biscicol/images/right-arrow.png" id="arrow" class="img-arrow">{text}'
                            + '</a>\n';
        $.each(data, function(index, element) {
            key=element.projectId;
            val=element.projectTitle;
            var project = val.replace(new RegExp('[#. ]', 'g'), '_') + '_' + key;

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
            var project = val.replace(new RegExp('[#. ]', 'g'), '_') + '_' + key;

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

// **
// function to apply the jquery slideToggle effect.
function projectToggle(id) {
    if ($('.toggle-content#'+id).is(':hidden')) {
        $('.img-arrow', '#'+id).attr("src","/biscicol/images/down-arrow.png");
    } else {
        $('.img-arrow', '#'+id).attr("src","/biscicol/images/right-arrow.png");
    }
    // check if we've loaded this section, if not, load from service
    var divId = 'div#' + id
    if ((id.indexOf("metadata") != -1 || id.indexOf("users") != -1 || id.indexOf("expeditions") != -1) &&
        ($(divId).children().length == 0 || $('#submitForm', divId).length !== 0)) {
        populateProjectSubsections(divId);
    }
    $('.toggle-content#'+id).slideToggle('slow');
}

// **
// populate the metadata subsection of projects.jsp from REST service
function populateMetadata(id, projectID) {
    var jqxhr = populateDivFromService(
        '/biscicol/rest/projects/' + projectID + '/metadata',
        id,
        'Unable to load this project\'s metadata from server.'
    ).done(function() {
        $("#edit_metadata", id).click(function() {
            var jqxhr2 = populateDivFromService(
                '/biscicol/rest/projects/' + projectID + '/metadataEditor',
                id,
                'Unable to load this project\'s metadata editor.'
            ).done(function() {
                $('#metadataSubmit', id).click(function() {
                    projectMetadataSubmit(projectID, id);
                 });
            });
            loadingDialog(jqxhr2);
        });
    });
    loadingDialog(jqxhr);
    return jqxhr;
}

// **
// show a confirmation dialog before removing a user from a project
function confirmRemoveUserDialog(element) {
    var username = $(element).data('username');
    $('#confirm').html($('#confirm').html().replace('{user}', username));
    $('#confirm').dialog({
        modal: true,
        autoOpen: true,
        title: "Remove User",
        resizable: false,
        width: 'auto',
        draggable: false,
        buttons:{ "Yes": function() {
                                        projectRemoveUser(element);
                                        $(this).dialog("close");
                                        $(this).dialog("destroy");
                                        $('#confirm').html("Are you sure you wish to remove {user}?");
                                    },
                  "Cancel": function(){
                                        $(this).dialog("close");
                                        $('#confirm').html("Are you sure you wish to remove {user}?");
                                        $(this).dialog("destroy");
                                      }
                }
        });
}

// **
// populate the users subsection of projects.jsp from REST service
function populateUsers(id, projectID) {
    var jqxhr = populateDivFromService(
        '/biscicol/rest/projects/' + projectID + '/users',
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
                    "/biscicol/rest/users/" + username + "/profile/listEditorAsTable/",
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
                loadingDialog(jqxhr2);


            });
        });
    });
    loadingDialog(jqxhr);
    return jqxhr;
}

// **
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
            '/biscicol/rest/projects/' + projectID + '/admin/expeditions/',
            id,
            'Unable to load this project\'s expeditions from server.'
        ).done(function() {
            $('#expeditionForm', id).click(function() {
                expeditionsPublicSubmit(id);
            });
        });
        loadingDialog(jqxhr);
    }
    return jqxhr;
}

// **
// function to submit the user's profile editor form
function profileSubmit(divId) {
    if ($("input.pwcheck", divId).val().length > 0 && $(".label", "#pwindicator").text() == "weak") {
        $(".error", divId).html("password too weak");
    } else if ($("input[name='new_password']").val().length > 0 &&
                    ($("input[name='old_password']").length > 0 && $("input[name='old_password']").val().length == 0)) {
        $(".error", divId).html("Old Password field required to change your Password");
    } else {
        var postURL = "/biscicol/rest/users/profile/update/";
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
                }
                $(location).attr("href", $(location).attr("host") + "/biscicol");
            }
        }).fail(function(jqxhr) {
            var json = $.parseJSON(jqxhr.responseText);
            $(".error", divId).html(json.usrMessage);
        });
        loadingDialog(jqxhr);
    }
}

// get profile editor
function getProfileEditor(username) {
    var jqxhr = populateDivFromService(
        "/biscicol/rest/users/profile/listEditorAsTable",
        "listUserProfile",
        "Unable to load this user's profile editor from the Server"
    ).done(function() {
        $(".error").text(getQueryParam("error"));
        $("#cancelButton").click(function() {
            var jqxhr2 = populateDivFromService(
                "/id/userService/profile/listAsTable",
                "listUserProfile",
                "Unable to load this user's profile from the Server")
                .done(function() {
                    $("a", "#profile").click( function() {
                        getProfileEditor();
                    });
                });
            loadingDialog(jqxhr2);
        });
        $("#profile_submit").click(function() {
            profileSubmit('div#listUserProfile');
        });
    });
    loadingDialog(jqxhr);
}

// **
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
    var jqxhr = $.post('/biscicol/rest/expeditions/admin/updateStatus', data.replace('&', '')
    ).done(function() {
        populateProjectSubsections(divId);
    }).fail(function(jqxhr) {
        $(divId).html(jqxhr.responseText);
    });
    loadingDialog(jqxhr);
}

// **
// function to add an existing user to a project or retrieve the create user form.
function projectUserSubmit(id) {
    var divId = 'div#' + id + "-users";
    if ($('select option:selected', divId).val() == 0) {
        var projectId = $("input[name='projectId']", divId).val()
        var jqxhr = populateDivFromService(
            '/biscicol/rest/users/admin/createUserForm',
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
        loadingDialog(jqxhr);
    } else {
        var jqxhr = $.post("/biscicol/rest/projects/" + $("input[name='projectId']").val() + "/admin/addUser", $('form', divId).serialize()
        ).done(function(data) {
            var jqxhr2 = populateProjectSubsections(divId);
        }).fail(function(jqxhr) {
            var jqxhr2 = populateProjectSubsections(divId);
            $(".error", divId).html($.parseJSON(jqxhr.responseText).usrMessage);
        });
        loadingDialog(jqxhr);
    }
}

// **
// function to submit the create user form.
function createUserSubmit(projectId, divId) {
    if ($(".label", "#pwindicator").text() == "weak") {
        $(".error", divId).html("password too weak");
    } else {
        var jqxhr = $.post("/biscicol/rest/users/admin/create", $('form', divId).serialize()
        ).done(function() {
            populateProjectSubsections(divId);
        }).fail(function(jqxhr) {
            $(".error", divId).html($.parseJSON(jqxhr.responseText).usrMessage);
        });
        loadingDialog(jqxhr);
    }
}

// **
// function to remove the user as a member of a project.
function projectRemoveUser(e) {
    var userId = $(e).data('userid');
    var projectId = $(e).closest('table').data('projectid');
    var divId = 'div#' + $(e).closest('div').attr('id');

    var jqxhr = $.getJSON("/biscicol/rest/projects/" + projectId + "/admin/removeUser/" + userId
    ).done (function(data) {
        var jqxhr2 = populateProjectSubsections(divId);
    }).fail(function(jqxhr) {
        var jqxhr2 = populateProjectSubsections(divId)
            .done(function() {
                $(".error", divId).html($.parseJSON(jqxhr.responseText).usrMessage);
            });
    });
    loadingDialog(jqxhr);
}

// **
// function to submit the project metadata editor form
function projectMetadataSubmit(projectId, divId) {
    var jqxhr = $.post("/biscicol/rest/projects/" + projectId + "/metadata/update", $('form', divId).serialize()
    ).done(function(data) {
        populateProjectSubsections(divId);
    }).fail(function(jqxhr) {
        $(".error", divId).html($.parseJSON(jqxhr.responseText).usrMessage);
    });
    loadingDialog(jqxhr);
}

// **
// function to populate the expeditions.jsp page
function populateExpeditionPage(username) {
    var jqxhr = listProjects(username, '/biscicol/rest/projects/listUserProjects', true
    ).done(function() {
        // attach toggle function to each project
        $(".expand-content").click(function() {
            loadExpeditions(this.id)
        });
    }).fail(function(jqxhr) {
        $("#sectioncontent").html(jqxhr.responseText);
    });
    loadingDialog(jqxhr);
}

// **
// function to load the expeditions.jsp subsections
function loadExpeditions(id) {
    if ($('.toggle-content#'+id).is(':hidden')) {
        $('.img-arrow', '#'+id).attr("src","/biscicol/images/down-arrow.png");
    } else {
        $('.img-arrow', '#'+id).attr("src","/biscicol/images/right-arrow.png");
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

// **
// retrieve the expeditions for a project and display them on the page
function listExpeditions(divId) {
    var projectId = $(divId).data('projectId');
    var jqxhr = $.getJSON('/biscicol/rest/projects/' + projectId + '/expeditions/')
        .done(function(data) {
            var html = '';
            var expandTemplate = '<br>\n<a class="expand-content" id="{expedition}-{section}" href="javascript:void(0);">\n'
                                + '\t <img src="/biscicol/images/right-arrow.png" id="arrow" class="img-arrow">{text}'
                                + '</a>\n';
            $.each(data, function(index, e) {
                var expedition = e.expeditionTitle.replace(new RegExp('[#. ]', 'g'), '_') + '_' + e.expeditionId;

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
                var expedition = e.expeditionTitle.replace(new RegExp('[#. ]', 'g'), '_') + '_' + e.expeditionId;

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
    loadingDialog(jqxhr);
}

// **
// function to populate the expedition resources, datasets, or configuration subsection of expeditions.jsp
function populateExpeditionSubsections(divId) {
    // load config table from REST service
    var expeditionId= $(divId).data('expeditionId');
    if (divId.indexOf("resources") != -1) {
        var jqxhr = populateDivFromService(
            '/biscicol/rest/expeditions/' + expeditionId + '/resourcesAsTable/',
            divId,
            'Unable to load this expedition\'s resources from server.');
        loadingDialog(jqxhr);
    } else if (divId.indexOf("datasets") != -1) {
        var jqxhr = populateDivFromService(
            '/biscicol/rest/expeditions/' + expeditionId + '/datasetsAsTable/',
            divId,
            'Unable to load this expedition\'s datasets from server.');
        loadingDialog(jqxhr);
    } else {
        var jqxhr = populateDivFromService(
            '/biscicol/rest/expeditions/' + expeditionId + '/metadataAsTable/',
            divId,
            'Unable to load this expedition\'s configuration from server.');
        loadingDialog(jqxhr);
    }
}

// load bcid editor
function getBCIDEditor(element) {
    var jqxhr = populateDivFromService(
        "/id/groupService/dataGroupEditorAsTable?ark=" + element.dataset.ark,
        "listUserBCIDsAsTable",
        "Unable to load the BCID editor from Server"
    ).done(function() {
        populateSelect("resourceTypesMinusDataset"
        ).done(function() {
            var options = $('option')
            $.each(options, function() {
                if ($('select').data('resource_type') == this.text) {
                    $('select').val(this.value);
                }
            })
        });
        $("#cancelButton").click(function() {
            populateBCIDPage();
        });
    });
    loadingDialog(jqxhr);

}

// function to populate the bcids.jsp page
function populateBCIDPage() {
    var jqxhr = populateDivFromService(
        "/id/groupService/listUserBCIDsAsTable",
        "listUserBCIDsAsTable",
        "Unable to load this user's BCIDs from Server"
    ).done(function() {
        $("a.edit").click(function() {
            getBCIDEditor(this);
        });
    }).fail(function(jqxhr) {
        $("#listUserBCIDsAsTable").html(jqxhr.responseText);
    });
    loadingDialog(jqxhr);
    return jqxhr;
}

// **
// function to submit the reset password form
function resetPassSubmit() {
    var jqxhr = $.get("/biscicol/rest/users/" + $("#username").val() + "/sendResetToken/")
        .done(function(data) {
            if (data.success) {
                var buttons = {
                    "Ok": function() {
                        window.location.replace("/biscicol/");
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
    loadingDialog(jqxhr);
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

// **
// function to login user
function login() {
    var url = "/biscicol/rest/authenticationService/login";
    var return_to = getQueryParam("return_to");
    if (return_to != null) {
        url += "?return_to=" + return_to;
    }
    var jqxhr = $.post(url, $('form').serialize())
        .done(function(data) {
            window.location.replace(data.url);
        }).fail(function(jqxhr) {
            $(".error").html($.parseJSON(jqxhr.responseText).usrMessage);
        });
    loadingDialog(jqxhr);
}
