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

function showSuccessWithText(text) {
    $("#error-alert").hide();
    $("#success-alert").html(text).show();
}

function showFailureWithText(text) {
    $("#success-alert").hide();
    $("#error-alert").html(text).show();
}

function hideAllAlerts() {
    $("#success-alert").hide();
    $("#error-alert").hide();
}

function waitForImport() {
  hideAllAlerts();
  waitingDialog.show(
  'Importing analytics data. Be patient, this might take a while...', {
    dialogSize: 'lg',
    progressType: 'success'
  });
  pollStatusEndpoint();
}

function pollStatusEndpoint() {
    setTimeout(function () { checkStatusRequest()}, 5000);
}

function wizardGoToDashboard() {
    waitingDialog.hide();
    var redirectLocation = location.protocol + "//" + location.hostname + ":5601/app/kibana#/dashboards";
    window.location.replace(redirectLocation);
}

function submitDetailsForm() {
    var projectName = encodeURIComponent($("#input-project-name").val());
    $.ajax({
      type : "PUT",
      url : `/a/projects/${projectName}/analytics-wizard~stack`,
      dataType: 'application/json',
      // Initially project-dashboard is a 1 to 1 relationship
      data: "{'dashboard_name': '" + projectName + "'}",
      contentType:"application/json; charset=utf-8",
      // Need to catch the status code since Gerrit doesn't return
      // a well formed JSON, hence Ajax treats it as an error
      statusCode: {
        201: showSuccessWithText("Configuration created successfully")
      },
      error: function(jqXHR, textStatus, errorThrown) {
        if(jqXHR.status != 201) {
          showFailureWithText("Error creating configuration: " + errorThrown)
        }
      }
    });
}

function dashboardService(command) {
    var projectName = encodeURIComponent($("#input-project-name").val());
    hideAllAlerts();
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
        201: waitForImport
      },
      error: function(jqXHR, textStatus, errorThrown) {
        if(jqXHR.status != 201) {
          showFailureWithText("Error starting your dashboard: " + errorThrown)
        }
      }
    });
}

function checkStatusRequest() {
    var projectName = encodeURIComponent($("#input-project-name").val());
    $.ajax({
      type : "GET",
      url : `/a/projects/${projectName}/analytics-wizard~status`,
      dataType: 'application/json',
      contentType:"application/json; charset=utf-8",
      statusCode: {
        202: pollStatusEndpoint,
        204: wizardGoToDashboard
      },
      error: function(jqXHR, textStatus, errorThrown) {
        if(jqXHR.status != 202 && jqXHR.status != 204) {
          showFailureWithText("Cannot start your dashboard: " + errorThrown);
          waitingDialog.hide();
        }
      }
    });
}

$(document).ready(function () {
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

        return data;
      }
  });
});