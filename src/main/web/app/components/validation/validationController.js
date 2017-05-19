angular.module('fims.validation')

    .controller('ValidationCtrl', ['$scope', '$q', '$http', '$window', '$location', '$uibModal', 'Validator', 'Upload', 'AuthFactory', 'ExpeditionFactory', 'FailModalFactory', 'ResultsDataFactory', 'ProjectFactory', 'StatusPollingFactory', 'REST_ROOT', 'NAAN', 'MAPBOX_TOKEN',
        function ($scope, $q, $http, $window, $location, $uibModal, Validator, Upload, AuthFactory, ExpeditionFactory, FailModalFactory, ResultsDataFactory, ProjectFactory, StatusPollingFactory, REST_ROOT, NAAN, MAPBOX_TOKEN) {
            var modalInstance = null;
            var vm = this;

            vm.projectId = undefined;
            vm.showProject = false;
            vm.isAuthenticated = AuthFactory.isAuthenticated;
            vm.newExpedition = false;
            vm.expeditonCode = undefined;
            vm.expedition = undefined;
            vm.expeditions = [];
            vm.isPublic = false;
            vm.displayResults = false;
            vm.fimsMetadata = undefined;
            vm.activeTab = 0;
            vm.setIsPublic = setIsPublic;
            vm.fimsMetadataChange = fimsMetadataChange;
            vm.validate = validate;
            vm.upload = upload;

            function upload() {
                $scope.$broadcast('show-errors-check-validity');

                if (vm.newExpedition) {
                    checkExpeditionExists()
                        .finally(function () {
                            if (vm.uploadForm.$invalid) {
                                return;
                            }

                            submitUpload();
                        });
                } else {
                    if (vm.uploadForm.$invalid) {
                        return;
                    }
                    submitUpload();
                }

            }

            function submitUpload() {

                validateSubmit(true).then(
                    function (response) {
                        if (response.data.done) {
                            ResultsDataFactory.validationMessages = response.data.done;
                            ResultsDataFactory.showOkButton = true;
                            ResultsDataFactory.showValidationMessages = true;
                        } else if (response.data.continue) {
                            if (response.data.continue.message == "continue") {
                                continueUpload();
                            } else {
                                ResultsDataFactory.validationMessages = response.data.continue;
                                ResultsDataFactory.showValidationMessages = true;
                                ResultsDataFactory.showStatus = false;
                                ResultsDataFactory.showContinueButton = true;
                                ResultsDataFactory.showCancelButton = true;
                            }
                        } else {
                            ResultsDataFactory.error = "Unexpected response from server. Please contact system admin.";
                            ResultsDataFactory.showOkButton = true;
                        }
                    });
            }

            $scope.$on("resultsModalContinueUploadEvent", function () {
                continueUpload();
                ResultsDataFactory.showContinueButton = false;
                ResultsDataFactory.showCancelButton = false;
                ResultsDataFactory.showValidationMessages = false;
            });

            function continueUpload() {
                StatusPollingFactory.startPolling();
                return $http.get(REST_ROOT + "validate/continue?createExpedition=" + vm.newExpedition).then(
                    function (response) {
                        if (response.data.error) {
                            ResultsDataFactory.error = response.data.error;
                        } else if (response.data.continue) {
                            ResultsDataFactory.uploadMessage = response.data.continue.message;
                            ResultsDataFactory.showOkButton = false;
                            ResultsDataFactory.showContinueButton = true;
                            ResultsDataFactory.showCancelButton = true;
                            ResultsDataFactory.showStatus = false;
                            ResultsDataFactory.showUploadMessages = true;
                        } else {
                            ResultsDataFactory.successMessage = response.data.done;
                            modalInstance.close();
                            resetForm();
                        }

                    }, function (response) {
                        ResultsDataFactory.reset();
                        ResultsDataFactory.error = response.data.error || response.data.usrMessage || "Server Error!";
                        ResultsDataFactory.showOkButton = true;
                    })
                    .finally(
                        function () {
                            if (vm.newExpedition) {
                                getExpeditions();
                            }
                            StatusPollingFactory.stopPolling();
                        }
                    );
            }

            function validateSubmit(forUpload) {
                ResultsDataFactory.reset();

                var validator = new Validator(vm.project.projectId, vm.expeditionCode)
                    .isPublic(vm.public)
                    .forUpload(forUpload);

                var splitFileName = vm.fimsMetadata.name.split('.');

                if (['xls', 'xlsx'].indexOf(splitFileName[splitFileName.length - 1]) > -1) {
                    validator.workbook(vm.fimsMetadata);
                } else {
                    validator.dataSource(vm.fimsMetadata);
                }

                // start polling here, since firefox support for progress events doesn't seem to be very good
                StatusPollingFactory.startPolling();
                openResultsModal();

                return validator.validate().then(
                    function (response) {
                        return response;
                    },
                    function (response) {
                        ResultsDataFactory.error = response.data.usrMessage || "Server Error!";
                        ResultsDataFactory.showOkButton = true;
                        return response;
                    }
                ).finally(function () {
                    StatusPollingFactory.stopPolling();
                });

            }

            function validate() {
                validateSubmit(false).then(
                    function (response) {
                        ResultsDataFactory.validationResponse = response.data;
                        modalInstance.close();
                    }
                );
            }

            function openResultsModal() {
                modalInstance = $uibModal.open({
                    templateUrl: 'app/components/validation/results/resultsModal.tpl.html',
                    size: 'md',
                    controller: 'ResultsModalCtrl',
                    controllerAs: 'vm',
                    windowClass: 'app-modal-window',
                    backdrop: 'static'
                });

                modalInstance.result
                    .finally(function () {
                            vm.displayResults = true;
                            if (!ResultsDataFactory.error) {
                                vm.activeTab = 2; // index 2 is the results tab
                            }
                            ResultsDataFactory.showStatus = false;
                            ResultsDataFactory.showValidationMessages = true;
                            ResultsDataFactory.showSuccessMessages = true;
                            ResultsDataFactory.showUploadMessages = false;
                        }
                    )

            }

            function resetForm() {
                vm.fimsMetadata = null;
                vm.expeditionCode = null;
                $scope.$broadcast('show-errors-reset');
            }

            function fimsMetadataChange() {
                // Clear the results
                ResultsDataFactory.reset();

                // Check NAAN
                parseSpreadsheet("~naan=[0-9]+~", "Instructions").then(
                    function (spreadsheetNaan) {
                        if (spreadsheetNaan > 0) {
                            checkNAAN(spreadsheetNaan);
                        }
                    });

                parseSpreadsheet("~project_id=[0-9]+~", "Instructions").then(
                    function (projectId) {
                        if (projectId) {
                            project = ProjectFactory.get(projectId);

                            if (project) {
                                vm.projectId = project.projectId;
                            }
                        }
                        vm.showProject = true;
                    });
            }

            function parseSpreadsheet(regExpression, sheetName) {
                try {
                    f = new FileReader();
                } catch (err) {
                    return null;
                }
                var deferred = new $q.defer();
                // older browsers don't have a FileReader
                if (f != null) {

                    var splitFileName = vm.fimsMetadata.name.split('.');
                    if (XLSXReader.exts.indexOf(splitFileName[splitFileName.length - 1]) > -1) {
                        $q.when(XLSXReader.utils.findCell(vm.fimsMetadata, regExpression, sheetName)).then(function (match) {
                            if (match) {
                                deferred.resolve(match.toString().split('=')[1].slice(0, -1));
                            } else {
                                deferred.resolve(null);
                            }
                        });
                        return deferred.promise;
                    }
                }
                setTimeout(function () {
                    deferred.resolve(null)
                }, 100);
                return deferred.promise;

            }

            // function to verify naan's
            function checkNAAN(spreadsheetNaan) {
                if (spreadsheetNaan != NAAN) {
                    var buttons = {
                        "Ok": function () {
                            $("#dialogContainer").removeClass("error");
                            $(this).dialog("close");
                        }
                    };
                    var message = "Spreadsheet appears to have been created using a different FIMS/BCID system.<br>";
                    message += "Spreadsheet says NAAN = " + spreadsheetNaan + "<br>";
                    message += "System says NAAN = " + naan + "<br>";
                    message += "Proceed only if you are SURE that this spreadsheet is being called.<br>";
                    message += "Otherwise, re-load the proper FIMS system or re-generate your spreadsheet template.";

                    dialog(message, "NAAN check", buttons);
                }
            }

            function checkExpeditionExists() {
                return ExpeditionFactory.getExpedition(vm.expeditionCode)
                    .then(function (response) {
                        // if we get an expedition, then it already exists
                        if (response.data) {
                            vm.uploadForm.newExpeditionCode.$setValidity("exists", false);
                        } else {
                            vm.uploadForm.newExpeditionCode.$setValidity("exists", true);
                        }
                    });
            }

            function getExpeditions() {
                ExpeditionFactory.getExpeditionsForUser(vm.project.projectId, true)
                    .then(function (response) {
                        angular.extend(vm.expeditions, response.data);
                        vm.newExpedition = vm.expeditions.length === 0;
                    }, function (response) {
                        FailModalFactory.open("Failed to load projects", response.data.usrMessage);
                    })
            }

            function setIsPublic(expedition) {
                vm.isPublic = expedition.public;
            }

            (function init() {
                fimsBrowserCheck($('#warning'));

                // if (vm.isAuthenticated) {
                //     getExpeditions();
                // }

                if ($location.search()['error']) {
                    $("#dialogContainer").addClass("error");
                    dialog("Authentication Error!<br><br>" + $location.search()['error'] + "Error", {
                        "OK": function () {
                            $("#dialogContainer").removeClass("error");
                            $(this).dialog("close");
                        }
                    });
                }
            }).call();

            $scope.$watch('vm.project', function (newValue, oldValue) {
                if (newValue && newValue !== oldValue) {
                    getExpeditions();
                    generateMap('map', vm.project.projectId, vm.fimsMetadata, MAPBOX_TOKEN)
                        .always(function () {
                        // this is a hack since we are using jQuery for generateMap
                        $scope.$apply();
                    });
                }
            });

        }]);
