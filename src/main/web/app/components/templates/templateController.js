angular.module('fims.templates', [])

.controller('TemplateCtrl', [function() {
    var vm = this;
    
    angular.element(document).ready(function () {
        populateProjects().done(function() {
            $('.toggle-content#projects_toggle').show(400);

            $('#projects').on('change', function() {
                var deferred = $.Deferred();
                loadingDialog(deferred);
                if ($('.toggle-content#config_toggle').is(':hidden')) {
                    $('.toggle-content#config_toggle').show(400);
                }

                populateColumns('#cat1');
                populateAbstract('#abstract');
                populateConfigs();
            });

            /* if there is a projectId query param, set the template generator projectId */
            var projectId = '<%=request.getParameter("projectId")%>';
            if (projectId > 0) {
                // verify that the projectId is a valid select option first
                if ($("#projects option[value='" + projectId + "']").length !== 0) {
                    $("#projects").val(projectId);

                    /* trigger the "change" event */
                    $("#projects").trigger("change");
                } else {
                    dialog("projectId " + projectId + " not found<br>", "Error", {"OK": function() {
                        $("#dialogContainer").removeClass("error");
                        $(this).dialog("close"); }
                    });
                }
            }
       });  
    });

    $('input').click(populate_bottom);

    $('#default_bold').click(function() {
        $('.check_boxes').prop('checked',true);
        populate_bottom();
    });
    $('#excel_button').click(function() {
        var li_list = new Array();
        $(".check_boxes").each(function() {
            li_list.push($(this).text() );
        });
        if(li_list.length > 0){
            download_file();
        }
        else{
            showMessage('You must select at least 1 field in order to export a spreadsheet.');
        }
    });
    
}]);