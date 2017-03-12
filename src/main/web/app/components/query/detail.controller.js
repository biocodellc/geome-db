(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryDetailController', QueryDetailController);

    QueryDetailController.$inject = ['sample'];

    function QueryDetailController(sample) {
        var vm = this;

        vm.sample = sample;
        vm.bcid = null;
        vm.bioProjectLink = null;
        vm.bioSamplesLink = null;

        activate();

        function activate() {
            vm.bcid = sample.bcid;
            delete sample.bcid;

            var fastqMetadata = sample.fastqMetadata;
            delete sample.fastqMetadata;

            if (fastqMetadata && fastqMetadata.bioSample) {
                vm.bioSamplesLink = "https://www.ncbi.nlm.nih.gov/bioproject/" + fastqMetadata.bioSample.bioProjectId;
                vm.bioProjectLink = "https://www.ncbi.nlm.nih.gov/biosample?LinkName=bioproject_biosample_all&from_uid=" + fastqMetadata.bioSample.bioProjectId;
            }
        }
    }
})();