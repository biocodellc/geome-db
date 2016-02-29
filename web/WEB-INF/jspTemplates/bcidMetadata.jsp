<%@ include file="/header-home.jsp" %>

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
                            value="${it.identifier.value}"
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

    <div class="sectioncontent row" id="results">
        <h1>${it.identifier.value} is a <a href="${it.get('rdf:type').value}">${it.get("rdf:type").shortValue}</a></h1>
        <table>
            <tr>
                <th>Description</th>
                <th>Value</th>
                <th>Definition</th>
            </tr>

            <c:forEach var="entry" items="${it}">
                <c:if test="${entry.key != 'identifier' && entry.key != 'download' && entry.key != 'datasets' && not empty entry.value.value }">
                    <tr>
                        <c:if test="${ entry.value.isResource}">
                            <td><a href="${entry.value.value}">${entry.value.value}</a></td>
                        </c:if>
                        <c:if test="${ !entry.value.isResource}">
                            <td>${entry.value.value}</td>
                        </c:if>
                        <td><a href="${entry.value.fullKey}">${entry.key}</a></td>
                        <td>${entry.value.description}</td>
                    </tr>
                </c:if>
            </c:forEach>
        </table>

        <c:if test="${not empty it['download']}">
            <table>
                <tr>
                    <th>Download:</th>
                    <th><a href="${it.download.appRoot}biocode-fims/rest/projects/query/excel?graphs=${it.download.graph}&project_id=${it.download.projectId}">.xlsx</a></th>
                    <th><a href="${it.download.appRoot}biocode-fims/rest/projects/query/tab?graphs=${it.download.graph}&project_id=${it.download.projectId}">.txt</a></th>
                    <th><a href="${it.download.n3}">.n3</a></th>
                </tr>
            </table>
        </c:if>

        <c:if test="${not empty it['datasets']}">
            <table>
                <tr>
                    <th>Date</th>
                    <th>Identifier</th>
                </tr>

                <!-- loop through each dataset -->
                <c:forEach var="entry" items="${it.datasets.datasets}">
                    <tr>
                        <td>${entry.ts}</td>
                        <td><a href="${it.datasets.appRoot}lookup.jsp?id=${entry.identifier}">${entry.identifier}</a></td>
                    </tr>
                </c:forEach>
            </table>
        </c:if>
    </div>
</div>

<%@ include file="/footer.jsp" %>