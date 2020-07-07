window.browser = window.browser || window.chrome;
document.addEventListener('DOMContentLoaded', function () {
  function generateQuickGuid() {
    return Math.random().toString(36).substring(2, 15) +
        Math.random().toString(36).substring(2, 15);
  }

  Formio.createForm(document.getElementById('formio'), {
    components: [
      {
        id: "formId",
        type: 'textfield',
        key: 'gid',
        label: 'Id',
        placeholder: 'Enter id here.',
        spellcheck: false,
        input: true
      },
      {
        "label": "Created",
        "format": "yyyy-MM-dd HH:mm:ss",
        "tableView": false,
        "datePicker": {
          "disableWeekends": false,
          "disableWeekdays": false
        },
        "timePicker": {
          "showMeridian": false
        },
        "validate": {
          "unique": false,
          "multiple": false
        },
        "key": "created",
        "type": "datetime",
        "input": true,
        "suffix": "<i ref=\"icon\" class=\"fa fa-calendar\" style=\"\"></i>",
        "widget": {
          "type": "calendar",
          "displayInTimezone": "viewer",
          "language": "en",
          "useLocaleSettings": false,
          "allowInput": true,
          "mode": "single",
          "enableTime": true,
          "noCalendar": false,
          "format": "yyyy-MM-dd HH:mm:ss",
          "hourIncrement": 1,
          "minuteIncrement": 1,
          "time_24hr": true,
          "minDate": null,
          "disableWeekends": false,
          "disableWeekdays": false,
          "maxDate": null
        }
      },
      {
        id: "formTitle",
        type: 'textfield',
        key: 'title',
        label: 'Title',
        placeholder: 'Enter title here.',
        spellcheck: true,
        input: true
      },
      {
        id: "formUrl",
        type: 'textfield',
        key: 'url',
        label: 'Url',
        placeholder: 'Enter url here.',
        spellcheck: false,
        input: true
      },
      {
        id: "formNote",
        label: "Note",
        autoExpand: false,
        spellcheck: true,
        tableView: true,
        key: "note",
        type: "textarea",
        input: true
      },
      {
        id: "formTags",
        label: "Tags",
        autoExpand: false,
        spellcheck: true,
        tableView: true,
        key: "tags",
        type: "textarea",
        validate: {
          "required": true
        },
        input: true
      },
      {
        id: "formSnapshot",
        label: "Snapshot",
        autoExpand: false,
        spellcheck: true,
        tableView: true,
        key: "sshot",
        type: "textarea",
        input: true
      },
      {
        label: "Watch later",
        tableView: false,
        key: "watchLater",
        type: "checkbox",
        input: true
      },
      {
        label: "Grab content",
        tableView: false,
        key: "grabContent",
        type: "checkbox",
        input: true
      },
      {
        label: "Snapshot",
        tableView: false,
        key: "snapshot",
        type: "checkbox",
        input: true
      },
      {
        type: 'button',
        action: 'submit',
        label: 'Submit',
        theme: 'primary'
      }
    ]
  })
    .then(function (form) {

      window.myData = {};

      browser.runtime.onMessage.addListener(function (request, sender, sendResponse) {
        if ("action" in request && request["action"] === "life-chrome-tab-send") {
          var myData = request["data"];
          for (i in myData) {
            window.myData[i] = myData[i];
          }

          console.log("got life-chrome-tab-send", request);
          if ("mhtmldata" in window.myData && window.myData.mhtmldata instanceof Blob) {
          } else if ("myData_mhtmldata" in window) {
            console.log("use mhtml " + window.myData_mhtmldata);
            window.myData.mhtmldata = window.myData_mhtmldata;
          } else {
            if ("source" in request) {
              console.log("use source ");
              window.myData.mhtmldata = new Blob([request["source"]], {
                type: 'text/html'
              });
            }
          }
          console.log("got data to submit>", window.myData);
          form.submission = {
            data: {
              gid: generateQuickGuid(),
              created: new Date(),
              title: myData.title,
              url: myData.url,
              sshot: myData.sshot,
              snapshot: true
            }
          };
          document.getElementById('sshot-img').src = myData.sshot;
        }
      });


      if ("myData" in window) {
        document.getElementById('sshot-img').src = window.myData.sshot;
        form.submission = {
          data: {
            gid: generateQuickGuid(),
            created: new Date(),
            title: window.myData.title,
            url: window.myData.url,
            sshot: window.myData.sshot,
            snapshot: true
          }
        };
      } else {
        form.submission = {
          data: {
            gid: generateQuickGuid(),
            created: new Date(),
            snapshot: true
          }
        };
      }

      // Register for the submit event to get the completed submission.
      form.on('submit', function (submission) {
        console.log('Submission was made!', window.myData.extId, submission);
        console.log('Submission was made!>', window.myData);

        var fd = new FormData();
        for (i in submission.data) {
          if (i === "sshot") {
            if (submission.data["snapshot"]) {
              fd.append(i, submission.data[i]);
            }
          } else {
            fd.append(i, submission.data[i]);
          }
        }
        if (submission.data["grabContent"]) {
          fd.append('content', window.myData.mhtmldata, 'content.mhtml');
        }
        console.log('Submission was made!>', fd.get("title"), window.myData.mhtmldata);
        $.ajax({
            type: 'POST',
            url: 'http://127.0.0.1:9999/addlnk',
            data: fd,
            processData: false,
            contentType: false,
            success: function(result) {
              console.log(result);
              window.close();
            },
            error: function(result) {
              console.log(result);
            }
        }).done(function(data) {
               console.log(data);
        });
        // var xhr = new XMLHttpRequest();
        // xhr.onreadystatechange = function() {
        //   if (xhr.readyState == XMLHttpRequest.DONE) {
        //       console.log(xhr.response);
        //       alert(xhr.responseText);
        //   }
        // }        
        // xhr.open("POST", "http://127.0.0.1:9999/addlnk");
        // xhr.send(fd);        

      });

      // Everytime the form changes, this will fire.
      form.on('change', function (changed) {
        console.log('Form was changed', changed);
      });
    });


  var checkPageButton = document.getElementById('checkPage');
  checkPageButton.addEventListener('checkPage', function () {
    alert('checkPage');
    chrome.tabs.getSelected(null, function (tab) {
      alert('checkPage>' + tab.url + ":" + tab.title + ":" + tab.favIconUrl);

      /*
            var d = document;
      
            var f = d.createElement('form');
            f.action = '<url>';
            f.method = 'post';
            var i = d.createElement('input');
            i.type = 'hidden';
            i.name = 'url';
            i.value = tab.url;
            f.appendChild(i);
            d.body.appendChild(f);
            f.submit();
      */
    });
  }, false);
}, false);
