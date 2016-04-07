<div id="query" class="section">
<div class="sectioncontent">

<h2>Query</h2>

    <form method="POST">
        <table border=0 class="table" style="width:800px;">

            <tr project-select></tr>

            <tr>
                <td align=right>&nbsp;&nbsp;Choose Dataset(s)&nbsp;&nbsp;&nbsp;</td>
                <td><select id=graphs multiple style="display:inline-block;width:400px;text-align:left;"></select></td>
            </tr>

            <tr class=toggle-content id=filter-toggle>
                <td align=right>Filter</td>
                <td>
                    <select id="uri" style="max-width:100px;"><option value="0">Loading ...</option></select>
                    <p style='display:inline;'>=</p>
                    <input type="text" name="filter_value" style="width:250px;"/>
                    <input type=button value=+ id=add_filter  class="btn btn-default btn-sm"/>
                </td>
            </tr>

            <tr>
                <td colspan=2>
                    <input type=button id="submit" value=table class="btn btn-default btn-sm">
                    <input type=button id="submit" value=excel class="btn btn-default btn-sm">
                    <input type=button id="submit" value=kml class="btn btn-default btn-sm">
                </td>
            </tr>
        </table>
    </form>

    <div id=resultsContainer style='overflow:auto; display:none;'>
        <table id=results class="table" border=0>
            <tr>
                <th data-qrepeat="m header"><b data-qtext="m"></b></th>
            </tr>
            <tr data-qrepeat="m data">
                <td data-qrepeat="i m.row"><i data-qtext="i"></i>
            </tr>
        </table>
    </div>

</div>
</div>