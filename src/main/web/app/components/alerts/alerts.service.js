(function () {
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
            var m = new Message(msg, 'info', persist);
            return _addMessage(m);
        }

        function success(msg, persist) {
            var m = new Message(msg, 'success', persist);
            return _addMessage(m);
        }

        function warn(msg, persist) {
            var m = new Message(msg, 'warning', persist);
            return _addMessage(m);
        }

        function error(msg, persist) {
            var m = new Message(msg, 'error', persist);
            return _addMessage(m);
        }

        function getAlerts() {
            return alerts;
        }

        function remove(alert) {
            var toRemove = undefined;

            for (var i = 0; i < alerts.length; i++) {
                if (angular.equals(alert, alerts[i])) {
                    toRemove = i;
                    break;
                }
            }

            if (toRemove !== undefined) {
                alerts.splice(toRemove, 1);
            }
        }

        function removeTmp() {
            for (var i = 0; i < alerts.length; i++) {
                if (!alert.persist) {
                    alerts.splice(i, 1);
                }
            }
        }

        function _addMessage(m) {
            for (var i = 0; i < alerts.length; i++) {
                if (angular.equals(m, alerts[i])) {
                    return m;
                }
            }

            alerts.push(m);
        }
    }

    function Message(msg, level, persist) {
        this.msg = msg;
        this.level = level;
        this.persist = persist || false;
    }
})();