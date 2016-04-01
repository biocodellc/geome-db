<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML>

<html>
    <head>
    <title>Biocode FIMS Query</title>

    <link rel="stylesheet" type="text/css" href="/dipnet/css/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/dipnet/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="/dipnet/css/alerts.css"/>
    <link rel="stylesheet" type="text/css" href="/dipnet/css/biscicol.css"/>

    <script type="text/javascript" src="/dipnet/js/jquery.js"></script>
    <script type="text/javascript" src="/dipnet/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/dipnet/js/jquery.form.js"></script>
    <script type="text/javascript" src="/dipnet/js/BrowserDetect.js"></script>

    <script type="text/javascript" src="/dipnet/js/dipnet-fims.js"></script>

    <script src="/dipnet/js/distal.js"></script>
    <script>
        jQuery.fn.distal = function (json) {
            return this.each( function () { distal(this, json) } )
        };
    </script>

    <script type="text/javascript" src="/dipnet/js/dipnet-fims.js"></script>
    <script type="text/javascript" src="/dipnet/js/bootstrap.min.js"></script>

    <link rel="short icon" href="/dipnet/docs/fimsicon.png" />


</head>

<body>

<%@ include file="header-menus.jsp" %>
