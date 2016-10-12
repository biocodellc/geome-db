angular.module('fims.validation')

    .controller('ValidationCtrl', ['$scope', '$q', '$http', '$uibModal', 'Upload', 'AuthFactory', 'ExpeditionFactory', 'FailModalFactory', 'ResultsDataFactory', 'StatusPollingFactory', 'PROJECT_ID', 'REST_ROOT',
        function ($scope, $q, $http, $uibModal, Upload, AuthFactory, ExpeditionFactory, FailModalFactory, ResultsDataFactory, StatusPollingFactory, PROJECT_ID, REST_ROOT) {
            var vm = this;

            vm.projectId = PROJECT_ID;
            vm.isAuthenticated = AuthFactory.isAuthenticated;
            vm.dataTypes = {
                fims: true,
                fastq: false,
                fasta: false
            };
            vm.dataset = null;
            vm.fasta = null;
            vm.fastq = {
                filenames: null,
                pe: false
            };
            vm.expeditionCode = "";
            vm.expeditions = [];
            vm.verifyDataPoints = false;
            vm.coordinatesVerified = false;
            vm.displayResults = false;
            vm.coordinatesErrorClass = null;
            vm.createExpedition = createExpedition;
            vm.datasetChange = datasetChange;
            vm.validate = validate;
            vm.upload = upload;
            vm.checkDataTypes = checkDataTypes;
            vm.checkCoordinatesVerified = checkCoordinatesVerified;

            function checkCoordinatesVerified() {
                if (vm.verifyDataPoints && !vm.coordinatesVerified) {
                    vm.coordinatesErrorClass = "has-error";
                    return false;
                } else {
                    vm.coordinatesErrorClass = null;
                    return true;
                }
            }

            function checkDataTypes() {
                if (!vm.dataTypes.fasta && !vm.dataTypes.fastq) {
                    vm.dataTypes.fims = true;
                }
            }

            function upload() {
                ResultsDataFactory.reset();
                $scope.$broadcast('show-errors-check-validity');

                if (!checkCoordinatesVerified() || vm.uploadForm.$invalid || !(vm.dataset || vm.fasta)) {
                    return;
                }

                validateSubmit({
                    projectId: PROJECT_ID,
                    expeditionCode: vm.expeditionCode,
                    upload: true,
                    dataset: vm.dataset,
                    fasta: vm.fasta
                }).then(
                    function (response) {
                        if (response.data.done) {
                            angular.extend(ResultsDataFactory.messages, response.data.done);
                            ResultsDataFactory.showOkButton = true;
                        } else if (response.data.continue.message == "continue") {
                            continueUpload();
                        } else if (response.data.continue) {
                            ResultsDataFactory.messages = response.data.continue;
                            ResultsDataFactory.showContinueButton = true;
                            ResultsDataFactory.showCancelButton = true;

                            $scope.$on("resultsModalContinueUploadEvent", function () {
                                continueUpload();
                                ResultsDataFactory.showContinueButton = false;
                                ResultsDataFactory.showCancelButton = false;
                            });

                        } else {
                            ResultsDataFactory.error = "Unexpected response from server. Please contact system admin.";
                            ResultsDataFactory.showOkButton = true;
                        }

                    }
                ).finally(function () {
                    StatusPollingFactory.stopPolling();
                });
            }

            function continueUpload() {
                StatusPollingFactory.startPolling();
                return $http.get(REST_ROOT + "validate/continue").then(
                    function (response) {
                        if (response.data.error) {
                            ResultsDataFactory.error = response.data.error;
                        } else {
                            ResultsDataFactory.successMessage = response.data.done.message;
                            resetForm();
                        }
                        ResultsDataFactory.showOkButton = true;
                        vm.displayResults = true;

                    }, function (response) {
                        ResultsDataFactory.reset();
                        ResultsDataFactory.error = response.data.error || response.data.usrMessage || "Server Error!";
                        ResultsDataFactory.showOkButton = true;
                    }).finally(
                    function () {
                        StatusPollingFactory.stopPolling();
                    }
                );
            }

            function validateSubmit(data) {
                return Upload.upload({
                    url: REST_ROOT + "validate",
                    data: data
                }).then(
                    function (response) {
                        return response;
                    },
                    function (response) {
                        ResultsDataFactory.error = response.data.usrMessage || "Server Error!";
                        ResultsDataFactory.showOkButton = true;
                        return response;
                    }, function (evt) {
                        if (evt.type == "load") {
                            StatusPollingFactory.startPolling();
                            openResultsModal();
                        }
                    }
                ).finally(function () {
                    StatusPollingFactory.stopPolling();
                });

            }

            function validate() {
                validateSubmit({
                    projectId: PROJECT_ID,
                    expeditionCode: vm.expeditionCode,
                    upload: false,
                    dataset: vm.dataset
                }).then(
                    function (response) {
                        angular.extend(ResultsDataFactory.messages, response.data.done);
                        ResultsDataFactory.showOkButton = true;
                        StatusPollingFactory.stopPolling();
                    }
                );
            }

            function openResultsModal() {
                var modalInstance = $uibModal.open({
                    templateUrl: 'app/components/validation/results/resultsModal.tpl.html',
                    size: 'md',
                    controller: 'ResultsModalCtrl',
                    controllerAs: 'vm',
                    windowClass: 'app-modal-window',
                    backdrop: 'static'
                });

                modalInstance.result.then(
                    function () {
                        vm.displayResults = true;
                    }, function () {
                        vm.displayResults = true;
                    }
                )

            }

            function resetForm() {
                vm.dataset = null;
                vm.fasta = null;
                vm.fastq = {
                    filenames: null,
                    pe: false
                };
                vm.expeditionCode = "";
                vm.verifyDataPoints = false;
                vm.coordinatesVerified = false;
            }

            function datasetChange() {
                // Clear the results
                ResultsDataFactory.reset();

                if (vm.dataset) {
                    // Check NAAN
                    parseSpreadsheet("~naan=[0-9]+~", "Instructions").then(
                        function (spreadsheetNaan) {
                            if (spreadsheetNaan > 0) {
                                $http.get(REST_ROOT + "utils/getNAAN")
                                    .then(function (data) {
                                        checkNAAN(spreadsheetNaan, data.naan);
                                    });
                            }
                        });

                    generateMap('map', PROJECT_ID, vm.dataset).then(
                        function () {
                            vm.verifyDataPoints = true;
                        }, function () {
                            vm.verifyDataPoints = false;
                        }).always(function () {
                        // this is a hack since we are using jQuery for generateMap
                        $scope.$apply();
                    });
                } else {
                    vm.verifyDataPoints = false;
                    vm.coordinatesVerified = false;
                }
            }

            function createExpedition() {
                var modalInstance = $uibModal.open({
                    templateUrl: 'app/components/expeditions/createExpeditionModal.html',
                    controller: 'CreateExpeditionsModalCtrl',
                    controllerAs: 'vm',
                    size: 'md'
                });

                modalInstance.result.then(function (expeditionCode) {
                    getExpeditions();
                    vm.expeditionCode = expeditionCode;
                }, function () {
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

                    var splitFileName = vm.dataset.name.split('.');
                    if (XLSXReader.exts.indexOf(splitFileName[splitFileName.length - 1]) > -1) {
                        $q.when(XLSXReader.utils.findCell(vm.dataset, regExpression, sheetName)).then(function (match) {
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
            function checkNAAN(spreadsheetNaan, naan) {
                if (spreadsheetNaan != naan) {
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

            function getExpeditions() {
                ExpeditionFactory.getExpeditions()
                    .then(function (response) {
                        angular.extend(vm.expeditions, response.data);
                    }, function (response, status) {
                        FailModalFactory.open("Failed to load datasets", response.data.usrMessage);
                    })
            }

            (function init() {
                fimsBrowserCheck($('#warning'));

                if (vm.isAuthenticated) {
                    getExpeditions();
                }
            }).call();

        }]);
