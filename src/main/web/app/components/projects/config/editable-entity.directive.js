(function () {
    'use strict';

    angular.module('fims.projects')
        .directive('editEntity', editEntity)
        .directive('editableEntity', editableEntity);

    editEntity.$inject = ['$location', '$anchorScroll', '$uibTooltip', 'ProjectService'];

    function editEntity($location, $anchorScroll, $uibTooltip, ProjectService) {
        return {
            restrict: 'A',
            scope: {
                entity: '=',
                index: '='
            },
            bindToController: true,
            controller: _entityController,
            controllerAs: 'vm',
            templateUrl: 'app/components/projects/config/templates/entity.tpl.html',
            compile: function (el, attrs) {
                var tooltipLink = $uibTooltip('editPopoverTemplate', 'editPopover', 'none', {
                    useContentExp: true
                }).compile.apply(this, arguments);

                return function link(scope, element, attrs, ctrl) {
                    tooltipLink.apply(this, arguments);

                    ctrl.isChild = (ctrl.entity.parentEntity);

                    if (ctrl.entity.isNew) {
                        ctrl.editing = true;
                        $location.hash("entity_" + ctrl.index);
                        $anchorScroll();
                    }

                    ctrl.entities = [];
                    angular.forEach(ProjectService.currentProject.config.entities, function (entity) {
                        if (entity.conceptAlias !== ctrl.entity.conceptAlias) {
                            ctrl.entities.push(entity.conceptAlias);
                        }
                    });

                    ctrl.columns = [];
                    angular.forEach(ctrl.entity.attributes, function (attribute) {
                        ctrl.columns.push(attribute.column);
                    });

                    ctrl.existingWorksheets = ProjectService.currentProject.config.worksheets();

                }
            }
        }
    }

    editableEntity.$inject = ['$compile'];

    function editableEntity($compile) {
        return {
            priority: 1001,
            terminal: true, // don't compile anything else, they will be compiled in the link function
            compile: function (el, attrs) {
                el.removeAttr('editable-entity');
                el.attr('edit-entity', "");
                el.attr('edit-popover-template', "'app/components/projects/config/templates/edit-entity.tpl.html'");
                el.attr('edit-popover-is-open', 'vm.editing');
                el.attr('edit-popover-placement', 'auto bottom');
                el.attr('edit-popover-class', 'edit-popover');

                return function link(scope, iElement, iAttrs, ctrl) {
                    $compile(iElement)(scope);
                }
            }
        }
    }

    _entityController.$inject = ['$scope', '$uibModal', 'ProjectService'];

    function _entityController($scope, $uibModal, ProjectService) {
        var vm = this;
        var _broadcaster = false;
        var _config = ProjectService.currentProject.config;

        vm.editing = false;
        vm.remove = remove;
        vm.toggleEdit = toggleEdit;
        vm.newWorksheet = newWorksheet;

        function remove() {
            var modal = $uibModal.open({
                templateUrl: 'app/components/projects/config/templates/delete-entity-confirmation.tpl.html',
                size: 'md',
                controller: _deleteConfirmationController,
                controllerAs: 'vm',
                windowClass: 'app-modal-window',
                backdrop: 'static'
            });

            modal.result.then(
                function () {
                    var i = _config.entities.indexOf(vm.entity);
                    _config.entities.splice(i, 1);
                }
            );
        }

        function toggleEdit() {
            if (!vm.editing) {
                _broadcaster = true; // close other popups when we open this one
                $scope.$emit("$closeSiblingEditPopupEvent");
            }

            vm.editing = !vm.editing;
        }

        function newWorksheet(worksheet) {
            vm.existingWorksheets.push(worksheet);
            return worksheet;
        }

        $scope.$on("$closeEditPopupEvent", function () {
            if (!_broadcaster) {
                vm.editing = false;
            }
            _broadcaster = false;
        });
        
        $scope.$watch('vm.isChild', function (val) {
            if (!val) {
                vm.entity.parentEntity = undefined;
            }
        });

        //TODO watch uniqueKey and parentEntity and add required rule if necessary
        // $scope.$watch('vm.entity.uniqueKey')
    }

    _deleteConfirmationController.$inject = ['$uibModalInstance'];

    function _deleteConfirmationController($uibModalInstance) {
        var vm = this;
        vm.delete = $uibModalInstance.close;
        vm.cancel = $uibModalInstance.dismiss;
    }
})
();
