angular.module('fims.validation')

    .controller('ValidationCtrl', ['$scope', '$q', '$http', '$uibModal', 'Upload', 'AuthFactory', 'ExpeditionFactory', 'FailModalFactory', 'ResultsDataFactory', 'StatusPollingFactory', 'PROJECT_ID', 'REST_ROOT',
        function ($scope, $q, $http, $uibModal, Upload, AuthFactory, ExpeditionFactory, FailModalFactory, ResultsDataFactory, StatusPollingFactory, PROJECT_ID, REST_ROOT) {
            var defaultFastqMetadata = {
                libraryLayout: null,
                libraryStrategy: null,
                librarySource: null,
                librarySelection: null,
                platform: null,
                designDescription: null,
                instrumentModel: null
            };
            var vm = this;

            vm.projectId = PROJECT_ID;
            vm.isAuthenticated = AuthFactory.isAuthenticated;
            vm.dataTypes = {
                fims: true,
                fastq: false,
                fasta: false
            };
            vm.fastqMetadataLists = {};
            vm.dataset = null;
            vm.fasta = null;
            vm.fastqFilenames = null;
            vm.fastqMetadata = angular.copy(defaultFastqMetadata);
            vm.expeditonCode = "";
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
                if (!vm.dataTypes.fasta) {
                    vm.fasta = null;
                }
                if (!vm.dataTypes.fims) {
                    vm.dataTypes.fims = null;
                }
                if (!vm.dataTypes.fastq) {
                    vm.fastqMetadata = angular.copy(defaultFastqMetadata);
                }
            }

            function upload() {
                ResultsDataFactory.reset();
                $scope.$broadcast('show-errors-check-validity');

                // if (!checkCoordinatesVerified() || vm.uploadForm.$invalid || !(vm.dataset || vm.fasta)) {
                //     return;
                // }

                validateSubmit({
                    projectId: PROJECT_ID,
                    expeditionCode: vm.expeditionCode,
                    fastqMetadata: Upload.jsonBlob(vm.fastqMetadata),
                    upload: true,
                    public_status: true,
                    dataset: vm.dataset,
                    fasta: vm.fasta,
                    fastqFilenames: vm.fastqFilenames
                }).then(
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

                    }
                ).finally(function () {
                    StatusPollingFactory.stopPolling();
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
                return $http.get(REST_ROOT + "validate/continue?createExpedition=" + ResultsDataFactory.createExpedition).then(
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
                            ResultsDataFactory.createExpedition = true;
                        } else {
                            ResultsDataFactory.successMessage = response.data.done;
                            ResultsDataFactory.showStatus = false;
                            ResultsDataFactory.showUploadMessages = false;
                            ResultsDataFactory.showSuccessMessages = true;
                            ResultsDataFactory.showOkButton = true;
                            ResultsDataFactory.showContinueButton = false;
                            ResultsDataFactory.showCancelButton = false;
                            vm.displayResults = true;
                            resetForm();
                        }

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
                ResultsDataFactory.reset();
                validateSubmit({
                    projectId: PROJECT_ID,
                    expeditionCode: vm.expeditionCode,
                    upload: false,
                    dataset: vm.dataset
                }).then(
                    function (response) {
                        ResultsDataFactory.validationMessages = response.data.done;
                        ResultsDataFactory.showOkButton = true;
                        ResultsDataFactory.showValidationMessages = true;
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

                modalInstance.result.finally(function () {
                        vm.displayResults = true;
                        ResultsDataFactory.showStatus = false;
                        ResultsDataFactory.showValidationMessages = true;
                    ResultsDataFactory.showSuccessMessages = true;
                        ResultsDataFactory.showUploadMessages = false;
                    }
                )

            }

            function resetForm() {
                vm.dataset = null;
                vm.fasta = null;
                vm.fastqFilenames = null;
                angular.copy(defaultFastqMetadata, vm.fastqMetadata);
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
                                    .then(function (response) {
                                        checkNAAN(spreadsheetNaan, response.data.naan);
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

            function getFastqMetadataLists() {
                $http.get(REST_ROOT + "projects/" + PROJECT_ID + "/lists/fastqMetadata")
                    .then(function (response) {
                        angular.extend(vm.fastqMetadataLists, response.data);
                    }, function (response) {
                        FailModalFactory.open("Failed to load fastq metadata lists", response.data.usrMessage);
                    });
            }

            (function init() {
                fimsBrowserCheck($('#warning'));

                if (vm.isAuthenticated) {
                    getExpeditions();
                }

                getFastqMetadataLists();
            }).call();

        }]);
