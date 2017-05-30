(function () {
    'use strict';

    angular.module('fims.templates')
        .run(configureRoutes);

    configureRoutes.$inject = ['routerHelper'];

    function configureRoutes(routerHelper) {
        routerHelper.configureStates(getStates());
    }

    function getStates() {
        return [
            {
                state: 'template',
                config: {
                    url: "/template",
                    templateUrl: "app/components/templates/templates.html",
                    controller: "TemplateController as vm",
                    projectRequired: true
                }
            }
        ];
    }
})();