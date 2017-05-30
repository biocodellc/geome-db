(function () {
    'use strict';

    angular.module('fims.projects')
        .run(init);

    init.$inject = ['$location', 'StorageService', 'ProjectService'];

    function init($location, StorageService, ProjectService) {
        var projectId = StorageService.get('projectId');

        if ($location.search()['projectId']) {
            projectId = $location.search()['projectId'];
        }

        if (projectId) {
            ProjectService.setFromId(projectId);
        }
    }

})();