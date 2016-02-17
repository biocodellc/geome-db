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
                <a class="navbar-brand" href="/index.jsp">Biocode Field Information Management System</a>
            </div>

            <!-- Collect the nav links, forms, and other content for toggling -->
            <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
                <ul class="nav navbar-nav navbar-right">
                    <li class="dropdown">
                        <a href="#" data-toggle="dropdown" class="dropdown-toggle">Tools<b class="caret"></b></a>
                        <ul class="dropdown-menu">
                            <li><a href='/templates.jsp' class='enabled'>Generate Template</a></li>
                            <li><a href='/validation.jsp' class='enabled'>Validate/Load Data</a></li>
                            <li><a href='/query.jsp' class='enabled'>Query</a></li>
                            <li><a href='/secure/expeditions.jsp' class='enabled'>Expedition Manager</a></li>
                            <li><a href='/lookup.jsp' class='enabled'>ID Lookup</a></li>
                            <li>--------</li>
                            <c:if test="${username == null}">
                                <li><a href='#' class='disabled'>BCID Creator</a></li>
                            </c:if>
                            <c:if test="${username != null}">
                                <li><a href='/secure/bcidCreator.jsp' class='enabled'>BCID Creator</a></li>
                            </c:if>
                            <c:if test="${projectAdmin == true}">
                                <li><a href='/secure/projects.jsp' class='enabled'>Project Manager</a></li>
                            </c:if>
                            <c:if test="${projectAdmin == false}">
                                <li><a href='#' class='disabled'>Project Manager</a></li>
                            </c:if>
                        </ul>
                    </li>

<!--
                    <c:if test="${username != null}">
                        <li class="dropdown">
                            <a href="#" data-toggle="dropdown" class="dropdown-toggle">Management<b class="caret"></b></a>
                            <ul class="dropdown-menu">
                                <li><a href='/lookup.jsp' class='enabled'>ID Lookup</a></li>
                                <c:if test="${username == null}">
                                    <li><a href='#' class='disabled'>BCID Creator</a></li>
                                </c:if>
                                <c:if test="${username != null}">
                                    <li><a href='/secure/bcidCreator.jsp' class='enabled'>BCID Creator</a></li>
                                </c:if>
                                <c:if test="${projectAdmin == true}">
                                    <li><a href='/secure/projects.jsp' class='enabled'>Project</a></li>
                                </c:if>
                                <c:if test="${projectAdmin == false}">
                                    <li><a href='#' class='disabled'>Project</a></li>
                                </c:if>
                                <!--<li><a href='/secure/profile.jsp' class='enabled'>User Profile</a></li>-->
                            </ul>
                        </li>
                    </c:if>
-->

                    <c:if test="${username == null}">
                            <li><a id="login" href="/login.jsp">Login</a></li>
                    </c:if>
                    <c:if test="${username != null}">
                            <li><a href="/secure/profile.jsp">${username}</a></li>
                            <li><a id="logout" href="/biocode-fims/rest/authenticationService/logout/">Logout</a></li>
                    </c:if>
                    <li><a href="https://github.com/biocodellc/biscicol-fims/wiki/WebApp">Help</a></li>
                </ul>
            </div><!-- /.navbar-collapse -->
        </div>
    </nav>
<div class="alert-container"><div id="alerts"></div></div>
