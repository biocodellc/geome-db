<div id="validation" class="section">
    <div id="warning"></div>

    <div class="sectioncontent">

        <h2>Validate and Load Data</h2>

        <form method="POST">
            <table class="table" style="width:600px">
                <tr>
                    <td align="right">&nbsp;&nbsp;FIMS Data&nbsp;&nbsp;</td>
                    <td><input type="file" class="btn btn-default btn-xs" name="file" id="file1" /></td>
                    <td><button class="btn btn-default btn-sm" type="button" id="file_button">+</button></td>
                </tr>

                <tr class="toggle-content" id="file_toggle">
                    <td align="right">FIMS Data (optional)&nbsp;&nbsp;</td>
                    <td colspan=2><input type="file" class="btn btn-default btn-xs" name="file" id="file2" /></td>
                </tr>

                <tr class="toggle-content" id="projects_toggle">
                    <td align="right">Project&nbsp;&nbsp;</td>
                    <td colspan=2>
                        <select width=20 name="projectId" id="projects">
                            <option value=0>Loading projects ...</option>
                        </select>
                    </td>
                </tr>

                <tr>
                    <td align="right">Upload&nbsp;&nbsp;</td>
                    <td style="font-size:11px;" colspan=2>
                        <span ng-show="!vm.isLoggedIn()">
                            <input ng-show="!vm.isLoggedIn()" type="checkbox" disabled="disabled" /> (login to upload)
                        </span>
                        <input ng-show="vm.isLoggedIn()" type="checkbox" id="upload" name="upload" />
                    </td>
                </tr>

                <!--<tbody class="toggle-content" id="upload-toggle">-->

                    <tr class="toggle-content-upload toggle-content" id="expeditionCode_toggle">
                        <td align="right">Dataset Code&nbsp;&nbsp;</td>
                        <td colspan=2><input type="select" name="expeditionCode" id="expeditionCode" /></td>
                    </tr>

                    <tr class="toggle-content-upload toggle-content" id="expedition_public_toggle">
                        <td align="right">Public&nbsp;&nbsp;</td>
                        <td><input type="checkbox" name="public_status" id="public_status" /></td>
                    </tr>
                <!--</tbody>-->

                <tr>
                    <td></td>
                    <td><input type="button" value="Submit" class="btn btn-default btn-xs"></td>
                </tr>
            </table>
        </form>

        <div id=resultsContainer style='overflow:auto; display:none;'></div>

        <div id='map' style="height:400px;width:600px;"></div>
    </div>
</div>
