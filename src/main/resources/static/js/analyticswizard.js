var handle201 = function(data, textStatus, jqXHR) {
    $("#config-error-alert").hide();
    $("#config-created-alert").show();
};

var handleError = function(data, textStatus, jqXHR) {
  $("#config-created-alert").hide();
  $("#config-error-alert").show();
};


function submitDetailsForm() {
    var projectName = $("#input-project-name").val();
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

function showConfigDetails() {
    var projectName = $("#input-project-name").val();
    $.ajax({
            url: `/projects/${projectName}/analytics-wizard~stack`,
            type: "GET",
            dataType: "json",
            success: function(data) {
              $('#config-section').removeAttr('hidden');
              $('#config-file').text( $('#config-file').text() + data.config_file_name);
            },
            error: function(e) {
              console.error(JSON.stringify(e));
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