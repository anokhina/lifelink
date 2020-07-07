window.browser = window.browser || window.chrome;
window.chrome = window.browser;
document.addEventListener('DOMContentLoaded', function () {
    var myData = {};

    chrome.tabs.captureVisibleTab(null, { format: "jpeg", quality: 50 }, function (image) {
        myData.sshot = image.toString();

        chrome.tabs.query({ active: true, currentWindow: true }, function (tabs) {
            var tab = tabs[0];

            chrome.tabs.executeScript(tab.id, {
                file: "js/getPagesSource.js"
            }, function () {
                if (chrome.runtime.lastError) {
                    console.log("error", chrome.runtime.lastError);
                }
            });

            myData.title = tab.title;
            myData.url = tab.url;
            myData.extId = chrome.runtime.id;
            if (typeof chrome.pageCapture == 'undefined') {
                myData.mhtmldata = "";
                var popupWindow = window.open(
                    chrome.extension.getURL("popup.html"),
                    "win" + myData.extId,
                    "width=500,height=1000"
                );
                try {
                    chrome.runtime.sendMessage({ "action": "life-chrome-tab", "data": myData });
                } catch (e) {
                    console.log(e);
                }
                console.log(">>>>>>>>>>>>0>" + popupWindow);
                if (popupWindow == null) {
                    chrome.windows.create({
                        type: "detached_panel",
                        url: "popup.html",
                        width: 500,
                        height: 1000
                    });
                } else {
                    popupWindow.myData = myData;
                }
                window.close();

            } else {
                chrome.pageCapture.saveAsMHTML({ tabId: tab.id }, function (mhtmldata) {
                    myData.mhtmldata = mhtmldata;
                    try {
                        chrome.runtime.sendMessage({ "action": "life-chrome-tab", "data": myData });
                    } catch (e) {
                        console.log(e);
                    }
                    var popupWindow = window.open(
                        chrome.extension.getURL("popup.html"),
                        "win" + myData.extId,
                        "width=500,height=1000"
                    );
                    popupWindow.myData = myData;
                    popupWindow.myData_mhtmldata = myData.mhtmldata;

                    window.close();
                    /*
                    var reader = new FileReader();
                    reader.onload = function () {
                        myData.mhtmldata = reader.result;

                        var popupWindow = window.open(
                            chrome.extension.getURL("popup.html"),
                            "exampleName",
                            "width=400,height=400"
                        );
                        popupWindow.myData = myData;

                        window.close();

                    }
                    reader.readAsText(mhtmldata);
                    */
                });
            }

        });
    });


});