<%@ include file="header-home.jsp" %>

<div id="resolver" class="section">
    <div class="sectioncontent row">

        <h1>Lookup</h1>

        <form method="GET">
            <table border=0>
            <tr>
                <td>Identifier</td>
                <td>
                    <input
                        id="identifier"
                        value="ark:/21547/R2"
                        size="40"
                        onkeypress="if(event.keyCode==13) {submitResolver(); return false;}" />
                </td>
                <td>
                    <input
                        type="button"
                        onclick="submitResolver();"
                        name="Submit"
                        value="Submit" />
                </td>
            </tr>
            </table>
        </form>

    </div>
    <div class="sectioncontent row" id="results"></div>
</div>

<script>
    /* parse input parameter -- ARKS must be minimum length of 12 characters*/
    var a = '<%=request.getParameter("id")%>';
    if (a.length > 12) {
        $("#identifier").val(a);
        submitResolver();
    }
</script>
<%@ include file="footer.jsp" %>
