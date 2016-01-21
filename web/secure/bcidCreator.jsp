<%@ include file="../header-home.jsp" %>

<div id="doiMinter" class="section">

    <div class="sectioncontent">
        <h2>BCID Creator</h2>

        Create a group-level identifier that acts as a container for an unlimited number of user-specified elements.

        View the <a href="https://github.com/biocodellc/bcid/wiki/BCIDs">Biocode-Fims codesite</a> for more information.
        <ul>
            <li><b>Title</b> The title of this BCID.
            <li><b>ResourceType*</b> is required.  Each group level identifier can only represent one type of resource.
            <li><b>Target URL</b> is a place where requests for this identifier and any suffixes will resolve to. Required if Follow Suffixes is checked.
            <li><b>DOI</b> indicates a DOI that this dataset belongs to
            <li><b>Rights</b> All BCIDS fall under <a href="http://creativecommons.org/licenses/by/3.0/">Creative Commons Attribution 3.0</a>
            <li><b>Follow Suffixes</b> Check this box if you intend to append suffixes to this group for resolution.
        </ul>


        <form method="POST" id="bcidForm">
            <table>
                 <tr>
                    <td align=right>Title</td>
                    <td><input id=title name=title type=textbox size="40"></td>
                </tr>
                 <tr>
                        <td align=right><a href='/resourceTypes.jsp'>ResourceType*</a></td>
                        <td><select name=resourceTypesMinusDataset id=resourceTypesMinusDataset class=""></select></td>
                    </tr>
                <tr>
                    <td align=right>Target URL</td>
                    <td><input id=webaddress name=webaddress type=textbox size="40"></td>
                </tr>

                <tr>
                    <td align=right>DOI</td>
                    <td><input id=doi name=doi type=textbox size="40"></td>
                </tr>

                <tr>
                    <td align=right>Rights</td>
                    <td><a href="http://creativecommons.org/licenses/by/3.0/">Creative Commons Attribution 3.0</a></td>
                </tr>

                <tr>
                    <td align=xright>Follow Suffixes</td>
                    <td><input type=checkbox id=suffixPassThrough name=suffixPassThrough checked=yes></td>
                </tr>

                <tr>
                    <td colspan=2>
                    <input type="hidden" name="username" value="${username}" >
                    <input type="button" value="Submit" onclick="bcidCreatorSubmit();"/>
                    </td>
                 </tr>
            </table>
        </form>

    </div>
</div>

<script>
    window.onload = getResourceTypesMinusDataset("resourceTypesMinusDataset");
</script>

<%@ include file="../footer.jsp" %>
