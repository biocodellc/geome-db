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
                <div>
                <a class="navbar-brand" href="/index.jsp" style="float:left;"><img src='/docs/fishHeader.gif' height=50><br></a>
                <a class="navbar-brand" href="/index.jsp" style="float:left;line-height: 50px;color: #080808;">Biocode Field Information Management System</a>
                </div>
            </div>

            <!-- Collect the nav links, forms, and other content for toggling -->
            <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
                <ul class="nav navbar-nav navbar-right" style="margin-top: -15px;">
                    <li class="dropdown">
                        <a href="#" data-toggle="dropdown" class="dropdown-toggle">Tools<b class="caret"></b></a>
                        <ul class="dropdown-menu">
                            <li><a href='/templates.jsp' class='enabled'>Generate Template</a></li>
                            <li><a href='/validation.jsp' class='enabled'>Validate and Load Data</a></li>
                            <li><a href='/query.jsp' class='enabled'>Query</a></li>
                            <li><a href='/secure/expeditions.jsp' class='enabled'>Expedition Manager</a></li>
                            <li><a href='/lookup.jsp' class='enabled'>ID Lookup</a></li>
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

                    <c:if test="${username == null}">
                            <li><a id="login" href="/login.jsp">Login</a></li>
                    </c:if>
                    <c:if test="${username != null}">
                            <li><a href="/secure/profile.jsp">${username}</a></li>
                            <li><a id="logout" href="/biocode-fims/rest/authenticationService/logout/">Logout</a></li>
                    </c:if>
                    <li><a href="http://fims.readthedocs.org/en/latest/">Help</a></li>
                </ul>
            </div><!-- /.navbar-collapse -->
        </div>
    </nav>
<div class="alert-container"><div id="alerts"></div></div>
