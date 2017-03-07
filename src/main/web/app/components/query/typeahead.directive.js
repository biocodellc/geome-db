angular.module('fims.query')

    .directive('typeahead', [
        function () {

            return {
                restrict: 'E',
                scope: {
                    expeditions: '=',
                    selectedExpeditions: '='
                },
                controller: ["$scope", function ($scope) {
                    $scope.isVisible = false;
                    $scope.expeditions = [];
                    $scope.active = null;
                    $scope.filterTerm = "";
                    $scope.filteredExpeditions = [];

                    this.activate = function (expedition) {
                        $scope.active = expedition;
                    };

                    this.activateNextItem = function () {
                        var index = $scope.filteredExpeditions.indexOf($scope.active);
                        this.activate($scope.filteredExpeditions[(index + 1) % $scope.filteredExpeditions.length]);
                    };

                    this.activatePreviousItem = function () {
                        var index = $scope.filteredExpeditions.indexOf($scope.active);
                        this.activate($scope.filteredExpeditions[index === 0 ? $scope.filteredExpeditions.length - 1 : index - 1]);
                    };

                    this.isActive = function (expedition) {
                        return $scope.active === expedition;
                    };

                    this.selectActive = function () {
                        if ($scope.filteredExpeditions.length > 0 && $scope.filterTerm.length > 0 && $scope.active) {
                            this.select($scope.active);
                        }
                    };

                    this.select = function (expedition) {
                        $scope.selectedExpeditions.push(expedition);
                        $scope.active = null;
                        $scope.filterExpeditions();
                        this.activate($scope.filteredExpeditions[0]);
                    };

                    $scope.removeExpedition = function (expedition) {
                        $scope.selectedExpeditions.splice($scope.selectedExpeditions.indexOf(expedition), 1);
                        $scope.filterExpeditions();
                    };

                    $scope.showExpedition = function () {

                        return function (expedition) {
                            return !($scope.selectedExpeditions.indexOf(expedition) !== -1 ||
                            ($scope.filterTerm && !new RegExp($scope.filterTerm, 'i').test(expedition)));
                        }
                    };

                    $scope.filterExpeditions = function () {
                        var filteredExpeditions = [];
                        angular.forEach($scope.expeditions, function (expedition) {

                            if ($scope.selectedExpeditions.indexOf(expedition) === -1 &&
                                (!$scope.filterTerm || new RegExp($scope.filterTerm, 'i').test(expedition))) {
                                filteredExpeditions.push(expedition);
                            }
                        });

                        $scope.filteredExpeditions = filteredExpeditions;

                    };

                    this.init = function() {
                        $scope.filterExpeditions();
                        this.activate($scope.expeditions[0]);
                    }

                }],
                link: function (scope, element, attrs, controller) {

                    element.bind('keyup', function (e) {
                        if (e.keyCode === 9 || e.keyCode === 13) {
                            scope.$apply(function () {
                                controller.selectActive();
                            });
                        }

                        if (e.keyCode === 27) {
                            scope.$apply(function () {
                                scope.isVisible = false;
                            });
                        }

                        if (e.keyCode === 8 && scope.filterTerm.length == 0) {
                            scope.$apply(function() {scope.isVisible = false});
                        }
                    });

                    element.bind('keydown', function (e) {
                        if (!scope.isVisible) {
                            scope.isVisible = true;
                        }

                        if (e.keyCode === 9 || e.keyCode === 13 || e.keyCode === 27) {
                            e.preventDefault();
                        }

                        if (e.keyCode === 40) {
                            e.preventDefault();
                            scope.$apply(function () {
                                controller.activateNextItem();
                            });
                        }

                        if (e.keyCode === 38) {
                            e.preventDefault();
                            scope.$apply(function () {
                                controller.activatePreviousItem();
                            });
                        }
                    });

                    scope.$watch('expeditions', function(expeditions) {
                        expeditions.length ? controller.init() : null;
                    });

                },
                templateUrl: "app/components/query/typeahead.tpl.html"
            }

        }])

    .directive('typeaheadItem', function () {
        return {
            require: '^typeahead',
            link: function (scope, element, attrs, controller) {

                var item = scope.$eval(attrs.typeaheadItem);

                scope.$watch(function () {
                    return controller.isActive(item);
                }, function (active) {
                    if (active) {
                        element.addClass('active');
                    } else {
                        element.removeClass('active');
                    }
                });

                element.bind('mouseenter', function (evt) {
                    scope.$apply(function () {
                        controller.activate(item);

                    });
                });

                element.bind('click', function (e) {
                    scope.$apply(function () {
                        controller.select(item);
                    });
                });
            }
        };
    });