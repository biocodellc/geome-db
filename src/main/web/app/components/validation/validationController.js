angular.module('fims.validation')

    .controller('ValidationCtrl', ['$scope', '$q', '$http', '$window', '$uibModal', 'Upload', 'AuthFactory', 'ExpeditionFactory', 'FailModalFactory', 'ResultsDataFactory', 'StatusPollingFactory', 'PROJECT_ID', 'REST_ROOT',
        function ($scope, $q, $http, $window, $uibModal, Upload, AuthFactory, ExpeditionFactory, FailModalFactory, ResultsDataFactory, StatusPollingFactory, PROJECT_ID, REST_ROOT) {
            var defaultFastqMetadata = {
                libraryLayout: null,
                libraryStrategy: null,
                librarySource: null,
                librarySelection: null,
                platform: null,
                designDescription: null,
                instrumentModel: null
            };
            var latestExpeditionCode = null;
            var createExpeditionCode = null;
            var modalInstance = null;
            var vm = this;

            vm.projectId = PROJECT_ID;
            vm.isAuthenticated = AuthFactory.isAuthenticated;
            vm.dataTypes = {
                fims: true,
                fastq: false,
                fasta: false
            };
            vm.fastqMetadataLists = {};
            vm.fimsMetadata = null;
            vm.fastaFile = null;
            vm.fastaData = {};
            vm.fastqFilenames = null;
            vm.fastqMetadata = angular.copy(defaultFastqMetadata);
            vm.expeditonCode = null;
            vm.expeditions = [];
            vm.verifyDataPoints = false;
            vm.coordinatesVerified = false;
            vm.displayResults = false;
            vm.coordinatesErrorClass = null;
            vm.showGenbankDownload = false;
            vm.activeTab = 0;
            vm.fimsMetadataChange = fimsMetadataChange;
            vm.validate = validate;
            vm.upload = upload;
            vm.checkDataTypes = checkDataTypes;
            vm.checkCoordinatesVerified = checkCoordinatesVerified;
            vm.downloadFastqFiles = downloadFastqFiles;

            function downloadFastqFiles() {
                if (latestExpeditionCode == null) {
                    return;
                }
                $window.location = REST_ROOT + "expeditions/" + getExpeditionId() + "/sra/files";
            }

            function getExpeditionId() {
                var expeditionId;
                vm.expeditions.some(function (expedition) {
                    if (expedition.expeditionCode == latestExpeditionCode) {
                        expeditionId = expedition.expeditionId;
                    }
                });
                return expeditionId;
            }

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
                    vm.fasta = false;
                }
                if (!vm.dataTypes.fims) {
                    vm.dataTypes.fims = false;
                }
                if (!vm.dataTypes.fastq) {
                    vm.fastqMetadata = angular.copy(defaultFastqMetadata);
                }
            }

            function getUploadData() {
                var data = {};

                data.projectId = PROJECT_ID;
                data.expeditionCode = vm.expeditionCode ? vm.expeditionCode : createExpeditionCode;
                data.upload = true;
                data.public_status = true;

                if (vm.dataTypes.fims) {
                    data.fimsMetadata = vm.fimsMetadata;
                }
                if (vm.dataTypes.fasta) {
                    data.fastaFile = vm.fastaFile;
                    vm.fastaData.filename = vm.fastaFile.name;
                    data.fastaData = Upload.jsonBlob([vm.fastaData]);
                }
                if (vm.dataTypes.fastq) {
                    data.fastqMetadata = Upload.jsonBlob(vm.fastqMetadata);
                    data.fastqFilenames = vm.fastqFilenames;
                }

                return data;
            }

            function upload() {
                $scope.$broadcast('show-errors-check-validity');


                if (!checkCoordinatesVerified() || vm.uploadForm.$invalid) {
                    return;
                }

                if (!vm.expeditionCode) {
                    var modalInstance = $uibModal.open({
                        templateUrl: 'app/components/expeditions/createExpeditionModal.html',
                        controller: 'CreateExpeditionsModalCtrl',
                        controllerAs: 'vm',
                        size: 'md'
                    });

                    modalInstance.result.then(function (expeditionCode) {
                        createExpeditionCode = expeditionCode;
                        submitUpload();
                        ResultsDataFactory.createExpedition = true;
                    }, function () {
                    });
                } else {
                    submitUpload();
                }
            }

            function submitUpload() {
                var data = getUploadData();

                validateSubmit(data).then(
                    function (response) {
                        if (response.data.done) {
                            ResultsDataFactory.validationMessages = response.data.done;
                            ResultsDataFactory.showOkButton = true;
                            ResultsDataFactory.showValidationMessages = true;
                            createExpeditionCode = null;
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
                            createExpeditionCode = null;
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
                            modalInstance.close();
                            latestExpeditionCode = vm.expeditionCode;
                            vm.showGenbankDownload = true;
                            resetForm();
                        }

                    }, function (response) {
                        ResultsDataFactory.reset();
                        ResultsDataFactory.error = response.data.error || response.data.usrMessage || "Server Error!";
                        ResultsDataFactory.showOkButton = true;
                    })
                    .finally(
                        function () {
                            if (ResultsDataFactory.createExpedition) {
                                getExpeditions();
                                vm.expeditionCode = createExpeditionCode;
                            }
                            StatusPollingFactory.stopPolling();
                        }
                    );
            }

            function validateSubmit(data) {
                ResultsDataFactory.reset();
                vm.showGenbankDownload = false;
                // start polling here, since firefox support for progress events doesn't seem to be very good
                StatusPollingFactory.startPolling();
                openResultsModal();
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
                    fimsMetadata: vm.fimsMetadata
                }).then(
                    function (response) {
                        ResultsDataFactory.validationMessages = response.data.done;
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
                    .then(
                        function () {
                        },
                        function () {
                            // if we are here, then the user canceled the upload
                            createExpeditionCode = null;

                        })
                    .finally(function () {
                            vm.activeTab = 2; // index 2 is the results tab
                            vm.displayResults = true;
                            ResultsDataFactory.showStatus = false;
                            ResultsDataFactory.showValidationMessages = true;
                            ResultsDataFactory.showSuccessMessages = true;
                            ResultsDataFactory.showUploadMessages = false;
                        }
                    )

            }

            function resetForm() {
                vm.fimsMetadata = null;
                vm.fastaFile = null;
                vm.fastaData = null;
                vm.fastqFilenames = null;
                angular.copy(defaultFastqMetadata, vm.fastqMetadata);
                vm.expeditionCode = null;
                vm.verifyDataPoints = false;
                vm.coordinatesVerified = false;
                $scope.$broadcast('show-errors-reset');
            }

            function fimsMetadataChange() {
                // Clear the results
                ResultsDataFactory.reset();

                if (vm.fimsMetadata) {
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

                    generateMap('map', PROJECT_ID, vm.fimsMetadata).then(
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
