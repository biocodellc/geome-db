<%@ include file="/header-home.jsp" %>

<div id="resolver" class="section">
    <div class="sectioncontent row">

        <h1>Lookup</h1>

        <form>
            <table border=0>
                <tr>
                    <td>Identifier</td>
                    <td>
                        <input
                            type=text
                            name="identifier"
                            id="identifier"
                            value="${it.identifier.value}"
                            size="40"
                            onkeypress="if(event.keyCode==13) {resolverResults(); return false;}" />
                    </td>
                    <td>
                        <input
                            type="button"
                            onclick="resolverResults();"
                            name="Submit"
                            value="Submit" />
                    </td>
                </tr>
            </table>
        </form>

    </div>

    <div class="sectioncontent row" id="results">
        <h1>${it.identifier.value} is a <a href="${it.resource.value}"><!--${it.rdf.shortValue}--> TEST</a></h1>
        <table>
            <tr>
                <th>Description</th>
                <th>Value</th>
                <th>Definition</th>
            </tr>

            <c:forEach var="item" items="${it}">
            <h3>item:<h3> <p>${item}</p>
                <c:if test="${key != 'identifier'}">
                    <tr>
                        <td>${it.key.value}</td>
                        <td><a href="${it.key.fullKey}">${it.key.key}</a></td>
                        <td>${it.key.description}</td>
                    </tr>
                </c:if>
            </c:forEach>
        </table>

        <!-- if it.download != null
        <c:if test="false">
            <table>
                <tr>
                    <th>Download:</th>
                    <th><a href="${it.download.excel}">.xlsx</a></th>
                    <th><a href="${it.download.tab}">.txt</a></th>
                    <th><a href="${it.download.n3}">.n3</a></th>
                </tr>
            </table>
        </c:if>
        -->

        <!-- if it.datasets != null
        <c:if test="false">
            <table>
                <tr>
                    <th>Date</th>
                    <th>Identifier</th>
                </tr>

                <!-- loop through each dataset
                <c:forEach var="item" items="${it.datasets}">
                    <tr>
                        <td>${item.ts}</td>
                        <td>this rootPath + ${item.identifier}</td>
                    </tr>
                </c:forEach>
                -->
            </table>
        </c:if>
        -->
    </div>
</div>

<%@ include file="/footer.jsp" %>