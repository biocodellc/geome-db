<%@ include file="header-home.jsp" %>

<div class="section">
    <div class="sectioncontent">
        <h2>Password Reset</h2>

        <form method="POST" id="resetForm" autocomplete="off">
            <table>
                <tr>
                    <td align="right">New Password</td>
                    <td><input class="pwcheck" type="password" autocomplete="off" data-indicator="pwindicator" name="password"></td>
                </tr>
                <tr>
                    <td></td>
                    <td><div id="pwindicator"><div class="label"></div></div></td>
                </tr>
                <c:if test="${param['error'] != null}">
                <tr></tr>
                <tr>
                    <td></td>
                    <td class="error" align="center">${param.error}</td>
                </tr>
                </c:if>
                <tr>
                    <td><input type="hidden" name="resetToken" value="${param.resetToken}" /></td>
                    <td><input type="button" value="Submit" onclick="resetPassSubmit();"></td>
                </tr>
            </table>
        </form>

    </div>
</div>

<%@ include file="footer.jsp" %>