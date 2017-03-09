(function() {
    'use strict';

    angular.module('fims.alerts')
        .factory('alerts', alerts);

    alerts.$inject = [];

    function alerts() {
        var alerts = [];

        var service = {
            info: info,
            success: success,
            warn: warn,
            error: error,
            getAlerts: getAlerts,
            remove: remove
        };

        return service;

        function info(msg) {
            alerts.push(new Message(msg, 'info'))
        }

        function success(msg) {
            alerts.push(new Message(msg, 'success'))
        }

        function warn(msg) {
            alerts.push(new Message(msg, 'warning'))
        }

        function error(msg) {
            alerts.push(new Message(msg, 'error'))
        }

        function getAlerts() {
            return alerts;
        }

        function remove(alert) {
            var i = alerts.indexOf(alert);
            alerts.splice(i, 1);
        }
    }

    function Message(msg, level) {
        this.msg = msg;
        this.level = level
    }
})();