<%@ include file="../header-home.jsp" %>

<div class="section">
    <div class="sectioncontent">Loading Expeditions...</div>
</div>

<script>
    $(document).ready(function() {
        populateExpeditionPage("${username}");
    });
</script>

<%@ include file="../footer.jsp" %>