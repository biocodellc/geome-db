(function () {
    'use strict';

    angular.module('fims.templates')
        .controller('TemplateController', TemplateController);

    TemplateController.$inject = ['$rootScope', 'AuthFactory', 'TemplateService', 'ProjectService', 'UserFactory'];

    function TemplateController($scope, AuthFactory, TemplateService, ProjectService, UserFactory) {
        var DEFAULT_TEMPLATE = {name: 'DEFAULT'};
        var config = undefined;
        var vm = this;

        vm.isAuthenticated = AuthFactory.isAuthenticated;
        vm.template = DEFAULT_TEMPLATE;
        vm.templates = [DEFAULT_TEMPLATE];
        vm.sheetName = undefined;
        vm.description = undefined;
        vm.defAttribute = undefined;
        vm.required = [];
        vm.selected = [];
        vm.worksheets = [];
        vm.attributes = [];
        vm.toggleSelected = toggleSelected;
        vm.sheetChange = sheetChange;
        vm.templateChange = templateChange;
        vm.canRemoveTemplate = canRemoveTemplate;
        vm.generate = generate;

        init();

        function init() {
            var project = ProjectService.currentProject;
            if (project) {
                config = project.config;
                _getTemplates();
                _getWorksheets();
                _getAttributes();
                vm.description = project.description;
            }
        }

        function toggleSelected(attribute) {
            var i = vm.selected.indexOf(attribute);

            // currently selected
            if (i > -1) {
                vm.selected.splice(i, 1);
            } else {
                vm.selected.push(attribute);
            }
        }

        function sheetChange() {
            _getAttributes();
            vm.template = DEFAULT_TEMPLATE;
        }

        function templateChange() {
            if (vm.template === DEFAULT_TEMPLATE) {
                vm.selected = vm.required.concat(config.suggestedAttributes(vm.sheetName));
            } else {
                vm.selected = vm.required;

                angular.forEach(vm.attributes, function (attribute) {
                    if (vm.template.attributeUris.indexOf(attribute.uri) > -1) {
                        vm.selected.push(attribute);
                    }
                });
            }
        }

        function canRemoveTemplate() {
            return vm.template && vm.template !== DEFAULT_TEMPLATE &&
                vm.template.user.userId === UserFactory.user.userId;
        }
        
        function generate() {
            var columns = [];

            angular.forEach(vm.selected, function(attribute) {
                columns.push(attribute.column);
            });

            TemplateService.generate(ProjectService.currentProject.projectId, vm.sheetName, columns);
        }

        function _getTemplates() {
            TemplateService.all(ProjectService.currentProject.projectId)
                .then(function (response) {
                    vm.templates = [DEFAULT_TEMPLATE].concat(response.data);
                    templateChange();
                }, function (response) {
                    //TODO handle error
                });
        }

        function _getAttributes() {
            vm.attributes = config.attributesByGroup(vm.sheetName);
            vm.required = config.requiredAttributes(vm.sheetName);
        }

        function _getWorksheets() {
            vm.worksheets = config.worksheets();
            vm.sheetName = vm.worksheets[0];
        }

        $scope.$on('$projectChangeEvent', function () {
            init();
        });

    }

    // a  .
    //    controller('TemplateCtrl', ['$rootScope', '$location', '$scope', function ($rootScope, $location, $scope) {
    //        var vm = this;
    //
    //        $rootScope.$on('projectSelectLoadedEvent', function (event) {
    //
    //            $('#projects').on('change', function () {
    //                var deferred = $.Deferred();
    //                if ($('.toggle-content#config_toggle').is(':hidden')) {
    //                    $('.toggle-content#config_toggle').show(400);
    //                }
    //
    //                if ($('#projects').val() == 0)
    //                    hideTemplateInfo();
    //                else
    //                    showTemplateInfo();
    //
    //                populateColumns('#cat1');
    //                populateAbstract('#abstract');
    //                populateConfigs();
    //            });
    //
    //        });
    //
    //        $('input').click(populate_bottom);
    //
    //        $('#default_bold').click(function () {
    //            $('.check_boxes').prop('checked', true);
    //            populate_bottom();
    //        });
    //        $('#excel_button').click(function () {
    //            var li_list = new Array();
    //            $(".check_boxes").each(function () {
    //                li_list.push($(this).text());
    //            });
    //            if (li_list.length > 0) {
    //                download_file();
    //            }
    //            else {
    //                showMessage('You must select at least 1 field in order to export a spreadsheet.');
    //            }
    //        });
    //    }]);

})();
