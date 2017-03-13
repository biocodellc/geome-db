(function () {
    'use strict';

    angular.module('fims.query')
        .controller('QueryDetailController', QueryDetailController);

    QueryDetailController.$inject = ['_sample'];

    function QueryDetailController(_sample) {
        var vm = this;

        vm.sample = _sample;
        vm.bcid = null;
        vm.bioProjectLink = null;
        vm.bioSamplesLink = null;

        activate();

        function activate() {
            vm.bcid = vm.sample.bcid;
            delete vm.sample.bcid;

            var fastqMetadata = vm.sample.fastqMetadata;
            delete vm.sample.fastqMetadata;

            if (fastqMetadata && fastqMetadata.bioSample) {
                vm.bioSamplesLink = "https://www.ncbi.nlm.nih.gov/bioproject/" + fastqMetadata.bioSample.bioProjectId;
                vm.bioProjectLink = "https://www.ncbi.nlm.nih.gov/biosample?LinkName=bioproject_biosample_all&from_uid=" + fastqMetadata.bioSample.bioProjectId;
            }
        }
    }
})();