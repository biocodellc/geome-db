<%@ include file="header-home.jsp" %>
<div class="section">
    <div class="sectioncontent">
        <h1>HTTP ERROR ${errorInfo.getHttpStatusCode()}</h1>
        <br>
        <p>Problem accessing ${requestScope['javax.servlet.forward.request_uri']}. Reason:</p>
        <p>&emsp;&emsp;${errorInfo.getMessage()}</p>

        <c:remove var="errorInfo" scope="session" />
    </div>
</div>

<script>
</script>
<%@ include file="footer.jsp" %>