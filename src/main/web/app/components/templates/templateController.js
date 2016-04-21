angular.module('fims.templates', [])

.controller('TemplateCtrl', ['$rootScope', 'PROJECT_ID',
    function($rootScope, PROJECT_ID) {
        var vm = this;
        vm.projectId = PROJECT_ID;

        angular.element(document).ready(function() {

            showTemplateInfo();

            var jqxhr = populateColumns('#cat1');
            if (jqxhr) {
                loadingDialog(jqxhr);
            }
            populateAbstract('#abstract');
            populateConfigs();

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