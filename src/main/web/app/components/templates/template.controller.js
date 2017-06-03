(function () {
    'use strict';

    angular.module('fims.templates')
        .controller('TemplateController', TemplateController);

    TemplateController.$inject = ['$rootScope', 'UserService', 'TemplateService', 'ProjectService', 'exception'];

    function TemplateController($scope, UserService, TemplateService, ProjectService, exception) {
        var DEFAULT_TEMPLATE = {name: 'DEFAULT'};
        var config = undefined;
        var vm = this;

        vm.isAuthenticated = false;
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

            $scope.$watch(
                function() {return UserService.currentUser},
                function(newVal) {
                    vm.isAuthenticated = !!(newVal);
                }
            )
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
                vm.template.user.userId === UserService.currentUser.userId;
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
                },
                    exception.catcher("Failed to load templates")
                );
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

})();
