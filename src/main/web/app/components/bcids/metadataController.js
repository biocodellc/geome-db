angular.module('fims.bcids')

    .controller('MetadataCtrl', ['$http', '$stateParams', 'queryService', 'QueryBuilder', 'REST_ROOT',
        function ($http, $stateParams, queryService, QueryBuilder, REST_ROOT) {
            var DATASET_TYPE = "http://purl.org/dc/dcmitype/Dataset";

            var vm = this;
            vm.identifier = $stateParams.ark;
            vm.metadata = {};
            vm.filteredMetadata = {};
            vm.submit = submitForm;
            vm.getValue = getValue;
            vm.downloadLatestMetadata = downloadLatestMetadata;

            function getValue(key) {
                if (vm.metadata[key]) {
                    return vm.metadata[key].value
                }

                return ""
            }

            function submitForm() {
                $window.location = 'http://biscicol.org/id/' + vm.identifier;
            }

            function filterMetadata(metadata) {
                var filteredMetadata = {};
                var metadataToExclude = ['identifier', 'datasets', 'download', 'message'];
                angular.forEach(metadata, function (value, key) {
                    if (metadataToExclude.indexOf(key) == -1 && value.value.trim()) {
                        filteredMetadata[key] = value;
                    }
                });
                return filteredMetadata;
            }

            function downloadLatestMetadata(expeditionCode) {
                var builder = new QueryBuilder();
                builder.add("+expedition:" + expeditionCode);
                return queryService.downloadFastq(builder.build());
            }


            (function fetchMetadata() {
                return $http.get(REST_ROOT + 'bcids/metadata/' + vm.identifier)
                    .then(
                        function (response) {
                            vm.metadata = response.data;
                            if (vm.metadata['rdf:type'].value == DATASET_TYPE) {
                                vm.metadata.download = REST_ROOT + 'bcids/dataset/' + vm.identifier;
                            }
                            vm.filteredMetadata = filterMetadata(vm.metadata);
                        },
                        function (response, status) {
                            if (status == 404) {
                                vm.error = "Invalid identifier";
                            } else {
                                vm.error = response.data.usrMessage;
                            }
                        });
            }).call(this);

        }]);