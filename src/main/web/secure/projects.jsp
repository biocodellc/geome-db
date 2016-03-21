<%@ include file="../header-home.jsp" %>

<div class="section">
    <div class="sectioncontent">Loading Projects...</div>
</div>

<script>
    $(document).ready(function() {
        populateProjectPage("${username}");
    });
</script>

<%@ include file="../footer.jsp" %>