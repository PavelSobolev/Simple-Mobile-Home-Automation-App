// https://www.domoticz.com/wiki/Domoticz_API/JSON_URL

var firebase = require("firebase");
var zWaveHttp = require("http");

// Initialize Firebase
var config = {
    apiKey: "************",
    //authDomain: "*************",
    databaseURL: "***************",
    storageBucket: "****************",
};
firebase.initializeApp(config);

const newData = firebase.database().ref('/');

const address = "localhost";  //"192.168.0.103";
const onOffSpan = 20;

var oldHue = 0;
var oldLightBrightness = -1;
var counter = 0;
var oldOn = -1;


function sendHttpRequest(parameters)
{
    var command = `http://${address}:8080/json.htm?${parameters}`;    
    zWaveHttp.get(command,
        (resp) => {
            resp.on('data', (data) => {});
            resp.on('end', () => {});
        });
    
}


// new data arrival (when any part of realtime database is changed)
newData.on('value', (snapshot) =>
{
    //console.log(snapshot.val())
    
    let paramToApply = parseInt(snapshot.val().ParamToApply);
    var hue = parseInt(snapshot.val().Hue);
    var attention = parseInt(snapshot.val().LastAttentionData);
    var meditation = parseInt(snapshot.val().LastMeditationData);
    var offThreshold = parseInt(snapshot.val().offThreshold);
    var onThreshold = parseInt(snapshot.val().onThreshold);
    var lightBrightness = parseInt(snapshot.val().LightBrightness);
    var isOn = parseInt(snapshot.val().IsOn);
    var brainDataToUse = parseInt(snapshot.val().BrainDataToUse);

    // update threshold values!!??
    
    if (isOn == 0)
    {
        if (oldOn != isOn)
        {
            oldOn = isOn;
            sendHttpRequest('type=command&param=switchlight&idx=2&switchcmd=Off')
            return;
        }
        if (paramToApply == -1) return;
    }
    else
    {
        if (oldOn != isOn)
        {
            oldOn = isOn;
            sendHttpRequest("type=command&param=switchlight&idx=2&switchcmd=On");
            return
        }
        if (paramToApply == -1) return;
    }

    if (paramToApply == -1) return;

    // --------------------------------------------------------------------------------------
    switch (paramToApply) 
    {
        case 1: // Brightness ===============================================================
            
            if (oldLightBrightness != lightBrightness)
            {
                //console.log("paramToApply==1");
                sendHttpRequest(`type=command&param=setcolbrightnessvalue&idx=2&hue=360&brightness=${lightBrightness}&iswhite=true`);
                oldLightBrightness = lightBrightness;
                //console.log(`lightBrightness=${lightBrightness}`);
            }
            
            break;

        case 2: // hue =====================================================================
            if (parseInt(snapshot.val().IsOn) == 0) return;

            var xxx = new Date();
            var ch = xxx.getHours();
            var mi = xxx.getMinutes();
            if (mi < 10) mi = "0" + mi;
            var se = xxx.getSeconds();
            if (se < 10) se = "0" + se;            
            var mise = xxx.getMilliseconds();
            if (mise < 10) mise = "00" + mise;
            if (mise < 100) mise = "0" + mise;
            console.log(`${ch}:${mi}:${se}:${mise} - hue`);

            if (Math.abs(oldHue - hue) > 8)
            {
                let responseTxt = new String("");
                oldHue = hue;
                sendHttpRequest(`type=command&param=setcolbrightnessvalue&idx=2&hue=${hue}&brightness=100&iswhite=false`);
            }
            break;

        case 0: // on/off ==================================================================
            if (counter <= 20)
            {
                counter++;
                break;
            }
            else
                counter = 0;

            let isOn = parseInt(snapshot.val().IsOn)
            let responseTxt = new String("");

            var controlValue = 0;
            switch (brainDataToUse)
            {
                case 0: controlValue = attention; break;  // console.log("attention control");
                case 1: controlValue = meditation; break; //console.log("meditation control");
            }

            if (controlValue > onThreshold)            
                sendHttpRequest("type=command&param=switchlight&idx=2&switchcmd=On");            

            if (controlValue < offThreshold)
                sendHttpRequest("type=command&param=switchlight&idx=2&switchcmd=Off");            
            break;
    }
});