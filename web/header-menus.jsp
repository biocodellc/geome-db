<div id="container">
    <nav id="myNavbar" class="navbar navbar-default" role="navigation">
        <!-- Brand and toggle get grouped for better mobile display -->
        <div>
            <div class="navbar-header">
                <button type="button" class="navbar-toggle" data-toggle="collapse" data-target="#bs-example-navbar-collapse-1">
                    <span class="sr-only">Toggle navigation</span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                    <span class="icon-bar"></span>
                </button>
                <a class="navbar-brand" href="/biscicol/index.jsp">Biocode Field Information Management System</a>
            </div>

            <!-- Collect the nav links, forms, and other content for toggling -->
            <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
                <ul class="nav navbar-nav navbar-right">
                    <li class="dropdown">
                        <a href="#" data-toggle="dropdown" class="dropdown-toggle">Tools<b class="caret"></b></a>
                        <ul class="dropdown-menu">
                            <li><a href='/biscicol/templates.jsp' class='enabled'>Template</a></li>
                            <li><a href='/biscicol/validation.jsp' class='enabled'>Validation</a></li>
                            <li><a href='/biscicol/query.jsp' class='enabled'>Query</a></li>
                            <li><a href='/biscicol/lookup.jsp' class='enabled'>ID Lookup</a></li>
                        </ul>
                    </li>

                    <c:if test="${user != null}">
                        <li class="dropdown">
                            <a href="#" data-toggle="dropdown" class="dropdown-toggle">Management<b class="caret"></b></a>
                            <ul class="dropdown-menu">
                                <li><a href='/biscicol/secure/expeditions.jsp' class='enabled'>Expedition</a></li>
                                <c:if test="${projectAdmin == true}">
                                    <li><a href='/biscicol/secure/projects.jsp' class='enabled'>Project</a></li>
                                </c:if>
                                <c:if test="${projectAdmin == false}">
                                    <li><a href='#' class='disabled'>Project</a></li>
                                </c:if>
                                <li><a href='/biscicol/secure/profile.jsp' class='enabled'>User Profile</a></li>
                            </ul>
                        </li>
                    </c:if>

                    <c:if test="${user == null}">
                            <li><a id="login" href="/biscicol/login.jsp">Login</a></li>
                    </c:if>
                    <c:if test="${user != null}">
                            <li><a href="/biscicol/secure/profile.jsp">${user}</a></li>
                            <li><a id="logout" href="/biscicol/rest/authenticationService/logout/">Logout</a></li>
                    </c:if>
                    <li><a href="https://github.com/biocodellc/biocode-fims/wiki/WebVersion">Help</a></li>
                </ul>
            </div><!-- /.navbar-collapse -->
        </div>
    </nav>
<div class="alert-container"><div id="alerts"></div></div>
