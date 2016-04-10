angular.module('fims.templates', [])

.controller('TemplateCtrl', ['$rootScope', '$location', '$scope', function($rootScope, $location, $scope) {
    var vm = this;

    $rootScope.$on('projectSelectLoadedEvent', function(event){

        $('#projects').on('change', function() {
            var deferred = $.Deferred();
            if ($('.toggle-content#config_toggle').is(':hidden')) {
                $('.toggle-content#config_toggle').show(400);
            }

            if ($('#projects').val() == 0)
                hideTemplateInfo();
            else
                showTemplateInfo();

            populateColumns('#cat1');
            populateAbstract('#abstract');
            populateConfigs();
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