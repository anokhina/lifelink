window.browser = window.browser || window.chrome;
window.chrome = window.browser;

chrome.runtime.onInstalled.addListener(function () {
    console.log("INSTALLED");

    chrome.contextMenus.create({
        id: "search-selected",
        title: "Search: %s",
        contexts: ["selection"]
    });

    chrome.contextMenus.create({
        id: "open-link",
        title: "Go to local: %s",
        contexts: ["link"]
    });

});

var tabData = { action: "life-chrome-tab-send", data: {}, source: "", send: false };

browser.runtime.onMessage.addListener(handleMessage);
function handleMessage(request, sender, sendResponse) {
    console.log("?????????0>", request, tabData); //logs "your message"
    if ("action" in request && request["action"] === "life-chrome-tab") {
        console.log("????????" + request.data.mhtmldata);

        tabData.data = request.data;
        tabData.send = false;
    } if ("action" in request && request["action"] === "life-chrome-tab-src") {
        tabData.source = request.source;
        tabData.send = false;
    }
    setTimeout(() => {
        try {
            if (!tabData.send) {
                tabData.send = true;
                console.log("?????????1>", tabData); //logs "your message"
                browser.runtime.sendMessage(tabData);
            }
        } catch (e) {
            console.log(e);
        }

    }, 2000);
}

    chrome.contextMenus.onClicked.addListener(function (info, tab) {
        console.log(info);
        if (info.menuItemId == "search-selected") {
            getword(info, tab);
        }
        if (info.menuItemId == "open-link") {
            getlink(info, tab);
        }
    });


function getword(info, tab) {
    console.log("Word " + info.selectionText + " was clicked.");
    chrome.tabs.create({
        url: "http://www.google.com/search?q=" + info.selectionText
    });
}

function getlink(info, tab) {
    try {
        chrome.tabs.create({
            url: info.linkUrl
        });
    } catch (e) {
        chrome.tabs.create({
            url: chrome.extension.getURL("error.html")
        });
    }
}

