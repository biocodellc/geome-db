(function () {
    'use strict';

    angular.module('fims.projects.config')
        .factory('ProjectConfig', ProjectConfig);

    ProjectConfig.$inject = [];

    function ProjectConfig() {

        var ProjectConfig = function (config) {
            var self = this;

            init();

            function init() {
                angular.extend(self, config);
            }
        };

        return ProjectConfig;
    }
})();