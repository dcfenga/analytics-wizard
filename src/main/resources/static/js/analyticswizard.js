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
  showLoading('Importing analytics data. Be patient, this might take a while...', 'success');
  pollStatusEndpoint();
}

function showLoading(text, type) {
  hideAllAlerts();
  waitingDialog.show(text, {
    dialogSize: 'lg',
    progressType: type
  });
}

function pollStatusEndpoint() {
    setTimeout(function () { checkStatusRequest()}, 50000);
}

function baseURL() {
    return location.protocol + "//" + location.hostname;
}

function getKibanaIndex(success) {
    var elasticSearchURL = baseURL() + ":9200/.kibana/_search?q=_type:dashboard&stored_fields=_id";

    $.ajax({
        type : "GET",
        crossDomain: true,
        url : elasticSearchURL,
        dataType: 'json',
        contentType:"application/json; charset=utf-8"
    })
    .done(function ( json ) {
        var kibanaIndex = json["hits"]["hits"][0]["_id"];
        success("dashboard/" + kibanaIndex);
    })
    .fail(function ( jqxhr, textStatus, error ) {
        console.error("Error extracting kibana index: " + textStatus + ", " + error);
        console.log("Defaulting to all dashboards");
        success("dashboards");
    });
}

function wizardGoToDashboard() {
    waitingDialog.hide();

    getKibanaIndex( function (kibanaIndex) {
        var redirectLocation = baseURL() + ":5601/app/kibana#/" + kibanaIndex + "?" + kibanaTimeframeParams();
        window.location.replace(redirectLocation);
        //window.open(redirectLocation);
    });
}

function kibanaTimeframeParams() {
    var from = $("#since").val()
        || new Date(new Date().setFullYear(new Date().getFullYear() - 10)).toISOString();

    var to = $("#until").val()
        || new Date(Date.now()).toISOString();

    var timeFrame = "_g=(time:(from:'"+from+"',to:'"+to+"',mode:absolute))";

    return timeFrame;
}

function getRequestBody() {
  var etlConfigRaw = {
    aggregate: "email_hour",
    since: $("#since").val(),
    until: $("#until").val(),
    project_prefix: $("#input-project-prefix").val(),
    username: $("#username").val(),
    password: $("#password").val()
  }

  var etlConfig = {};
  Object.
    keys(etlConfigRaw).
    forEach((key) => {
      if (etlConfigRaw[key]) {
        etlConfig[key] = etlConfigRaw[key];
      }
    });

  var requestBody = {
    dashboard_name: $("#input-dashboard-name").val(),
    etl_config: etlConfig
  }
  console.log(`Request body: ${JSON.stringify(requestBody)}`);
  return requestBody;
}

function submitDetailsForm() {
    // Hardcoding All-Projects just to get access to the Gerrit Rest API
    // A dashboard can span across multiple projects.
    var projectName = encodeURIComponent("All-Projects");
    var requestBody = getRequestBody();
    if (!requestBody.dashboard_name) {
      showFailureWithText("Dashboard name must be defined!")
    } else {
      $.ajax({
        type : "PUT",
        url : `/a/projects/${projectName}/analytics-wizard~stack`,
        dataType: 'application/json',
        // Initially project-dashboard is a 1 to 1 relationship
        data: JSON.stringify(requestBody),
        contentType:"application/json; charset=utf-8",
        // Need to catch the status code since Gerrit doesn't return
        // a well formed JSON, hence Ajax treats it as an error
        statusCode: {
          201: function () { dashboardService('up'); }
        },
        error: function(jqXHR, textStatus, errorThrown) {
          if(jqXHR.status != 201) {
            showFailureWithText("Error creating configuration: " + errorThrown)
          }
        }
      });
    }
}

function dashboardService(command) {
    var projectName = encodeURIComponent("All-Projects");
    hideAllAlerts();
    var requestBody = {
      action: command,
      dashboard_name: $("#input-dashboard-name").val()
    };
    showLoading('Getting docker containers ready. Be patient, this might take a while...', 'info');
    $.ajax({
      type : "POST",
      url : `/a/projects/${projectName}/analytics-wizard~server`,
      dataType: 'application/json',
      // Initially project-dashboard is a 1 to 1 relationship
      data: JSON.stringify(requestBody),
      contentType:"application/json; charset=utf-8",
      // Need to catch the status code since Gerrit doesn't return
      // a well formed JSON, hence Ajax treats it as an error
      statusCode: {
        201: function () { waitForImport(); }
      },
      error: function(jqXHR, textStatus, errorThrown) {
        if(jqXHR.status == 0) {
            waitForImport();
        } else {
            waitingDialog.hide();
            showFailureWithText("Error starting your dashboard: " + errorThrown)
        }
        //if(jqXHR.status != 201) {
          //waitingDialog.hide();
          //showFailureWithText("Error starting your dashboard: " + errorThrown)
        //}
      }
    });
}

function checkStatusRequest() {
    var projectName = encodeURIComponent("All-Projects");
    $.ajax({
      type : "GET",
      url : `/a/projects/${projectName}/analytics-wizard~status`,
      dataType: 'application/json',
      contentType:"application/json; charset=utf-8",
      statusCode: {
        202: function() { pollStatusEndpoint(); },
        204: function() { wizardGoToDashboard(); }
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
  $(".dropdown-menu li a").click(function(){
    $(this).parents(".dropdown").find('.btn').html($(this).text() + ' <span class="caret"></span>');
    $(this).parents(".dropdown").find('.btn').val($(this).data('value'));
  });
  $('[data-toggle="tooltip"]').tooltip();
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

