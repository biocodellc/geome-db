<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML>

<html>
    <head>
    <title>Biocode FIMS Query</title>

    <link rel="stylesheet" type="text/css" href="/biscicol/css/jquery-ui.css" />
    <link rel="stylesheet" type="text/css" href="/biscicol/css/bootstrap.min.css">
    <link rel="stylesheet" type="text/css" href="/biscicol/css/alerts.css"/>
    <link rel="stylesheet" type="text/css" href="/biscicol/css/biscicol.css"/>

    <script type="text/javascript" src="/biscicol/js/jquery.js"></script>
    <script type="text/javascript" src="/biscicol/js/jquery-ui.min.js"></script>
    <script type="text/javascript" src="/biscicol/js/jquery.form.js"></script>
    <script type="text/javascript" src="/biscicol/js/BrowserDetect.js"></script>

    <script src="/biscicol/js/distal.js"></script>
    <script>
        jQuery.fn.distal = function (json) {
            return this.each( function () { distal(this, json) } )
        };
    </script>

    <script type="text/javascript" src="/biscicol/js/biscicol-fims.js"></script>
    <script type="text/javascript" src="/biscicol/js/bootstrap.min.js"></script>

    <link rel="short icon" href="/biscicol/docs/fimsicon.png" />


</head>

<body>

<%@ include file="header-menus.jsp" %>
