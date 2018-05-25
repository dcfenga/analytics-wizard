// Copyright (C) 2018 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

var handle201 = function(data, textStatus, jqXHR) {
    $("#config-error-alert").hide();
    $("#config-created-alert").show();
};

var handleError = function(data, textStatus, jqXHR) {
  $("#config-created-alert").hide();
  $("#config-error-alert").show();
};


function submitDetailsForm() {
    var projectName = encodeURIComponent($("#input-project-name").val());
    $.ajax({
      type : "PUT",
      url : `/a/projects/${projectName}/analytics-wizard~stack`,
      dataType: 'application/json',
      // Initially project-dashboard is a 1 to 1 relationship
      data: "{'dashboard_name': '" + projectName + "}'}",
      contentType:"application/json; charset=utf-8",
      // Need to catch the status code since Gerrit doesn't return
      // a well formed JSON, hence Ajax treats it as an error
      statusCode: {
        201: handle201
      },
      error: function(jqXHR, textStatus, errorThrown) {
        if(jqXHR.status != 201) {
          handleError()
        }
      }
    });
}

var handle201Status = function(command) {
  $("#up-error-alert").hide();
  $("#down-error-alert").hide();
  $("#up-ok-alert").hide();
  $("#down-ok-alert").hide();
  $("#" + command + "-ok-alert").show();
};

function dashboardService(command) {
    var projectName = encodeURIComponent($("#input-project-name").val());
    $.ajax({
      type : "POST",
      url : `/a/projects/${projectName}/analytics-wizard~server`,
      dataType: 'application/json',
      // Initially project-dashboard is a 1 to 1 relationship
      data: "{'action': '" + command + "'}",
      contentType:"application/json; charset=utf-8",
      // Need to catch the status code since Gerrit doesn't return
      // a well formed JSON, hence Ajax treats it as an error
      statusCode: {
        201: handle201Status(command)
      },
      error: function(jqXHR, textStatus, errorThrown) {
        if(jqXHR.status != 201) {
          handleError()
        }
      }
    });
}

$(document).ready(function () {
  console.log("Starting Analytics wizard plugin...");
  $.ajaxSetup({
      dataFilter: function(data, type) {
        //Strip out Gerrit API prefix
        var prefixes = [")]}'"];

        if (type != 'json' && type != 'jsonp') {
          return data;
        }

        for (i = 0, l = prefixes.length; i < l; i++) {
          pos = data.indexOf(prefixes[i]);
          if (pos === 0) {
            return data.substring(prefixes[i].length);
          }
        }

        console.log("Parsed data: " + JSON.stringify(data))
        return data;
      }
  });
});