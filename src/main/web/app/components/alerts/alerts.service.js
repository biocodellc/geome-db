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
            remove: remove,
            removeTmp: removeTmp
        };

        return service;

        function info(msg, persist) {
            alerts.push(new Message(msg, 'info', persist))
        }

        function success(msg, persist) {
            alerts.push(new Message(msg, 'success', persist))
        }

        function warn(msg, persist) {
            alerts.push(new Message(msg, 'warning', persist))
        }

        function error(msg, persist) {
            alerts.push(new Message(msg, 'error', persist))
        }

        function getAlerts() {
            return alerts;
        }

        function remove(alert) {
            var i = alerts.indexOf(alert);
            alerts.splice(i, 1);
        }

        function removeTmp() {
            for (var i=0; i < alerts.length; i++) {
                if (!alert.persist) {
                    alerts.splice(i, 1);
                }
            }
        }
    }

    function Message(msg, level, persist) {
        this.msg = msg;
        this.level = level;
        this.persist = persist || false;
    }
})();