import 'dart:html';
import 'dart:convert';
import 'dart:async';
import 'dart:js' as js;

String encodeMap(Map data) {
  return data.keys.map((k) {
    return '${Uri.encodeComponent(k)}=${Uri.encodeComponent(data[k])}';
  }).join('&');
}

void loadEnd(HttpRequest request) {
  if (request.status != 200) {
    print('Uh oh, there was an error of ${request.status}');
  } else {
    print('Data has been posted');

  }
}

void handleCmdOutput(String msg) {
  var textOutput = querySelector("#areaResult");
  if (textOutput != null) {

    textOutput.text = "$msg${textOutput.text}";

  }

}

void handleLoadCfg(String msg) {

  print("Response received: $msg");
  Map msgMap = JSON.decode(msg);

  (querySelector("#txtPVoutUrl") as InputElement).value = msgMap["pvOutputUrl"];
  (querySelector("#txtAPIkey") as InputElement).value = msgMap["pvOutputApiKey"];
  (querySelector("#txtSystemId") as InputElement).value = "${msgMap["pvOutputSystemId"]}";

  (querySelector("#txtSerialPort") as InputElement).value = "${msgMap["serialPort"]}";
  (querySelector("#txtBaudRate") as InputElement).value = "${msgMap["baudRate"]}";
  (querySelector("#txtInvAddress") as InputElement).value = "${msgMap["inverterAddress"]}";

}

void handleStatus(String msg) {

  print("Response received: $msg");
  Map msgMap = JSON.decode(msg);

  String invStatus = msgMap["inverterStatus"];
  String pvOutputStatus = msgMap["pvOutputStatus"];

  setPvOutputRunning(pvOutputStatus == "on");

  if (invStatus == "online") {

    setInverterOnline();
  } else {
    setInverterOffline();
  }


}


void sendInvCommand(Map cmd) {
  var dataUrl = '/cmd/inv';

  String jsonData = JSON.encode(cmd);

  var encodedData = encodeMap(cmd);

  var urlString = dataUrl + $.param(cmd);
  print('urlString ' + urlString);
  HttpRequest.getString(urlString).then((responseString) {
    handleCmdOutput(responseString);

  });

}

String createPVoutParamsQueryString() {

  Map cmdMap = {
  } ;

  var periodBtnActive = querySelectorAll('[id^="btnPeriod"]' '.active').first;

  cmdMap["pvOutputApiKey"] = (querySelector("#txtAPIkey") as InputElement).value;
  cmdMap["pvOutputSystemId"] = (querySelector("#txtSystemId") as InputElement).value;
  cmdMap["pvOutputUrl"] = (querySelector("#txtPVoutUrl") as InputElement).value;
  cmdMap["pvOutputPeriod"] = periodBtnActive.text;


  var encodedData = encodeMap(cmdMap);

  return encodedData;

}

Future<String> sendSaveCfgCommand() {
  var urlString = '/cmd/saveCfg';

  var encodedData = createPVoutParamsQueryString();

  urlString = "$urlString?${encodedData}";
  print('urlString $urlString');
  print("PVoutput settings saved");

  return HttpRequest.getString(urlString);

}

String createSettingsQueryString() {

  Map cmdMap = {
  } ;

  var periodBtnActive = querySelectorAll('[id^="btnPeriod"]''.active').first;

  cmdMap["serialPort"] = (querySelector("#txtSerialPort") as InputElement).value;
  cmdMap["baudRate"] = (querySelector("#txtBaudRate") as InputElement).value;
  cmdMap["inverterAddress"] = (querySelector("#txtInvAddress") as InputElement).value;


  var encodedData = encodeMap(cmdMap);

  return encodedData;

}

void sendSaveSettings() {
  var urlString = '/cmd/saveSettings';
  var encodedData = createSettingsQueryString();

  urlString = "$urlString?${encodedData}";
  print('urlString $urlString');
  HttpRequest.getString(urlString);


}

void setInverterOnline() {

  querySelector("#lblInverterStatus").setAttribute("class", "label label-success");

}

void setInverterOffline() {
  querySelector("#lblInverterStatus").setAttribute("class", "label label-danger");

}

void setPvOutputRunning(bool isRunning) {

  Element btnStart = querySelector("#btnPVoutStart");
  Element btnStop = querySelector("#btnPVoutStop");

  if (isRunning) {
    btnStart.classes.add("active");
    btnStop.classes.remove("active");

  }
  else {
    btnStop.classes.add("active");
    btnStart.classes.remove("active");

  }

}

void sendPvOutputStartCommand() {
  var urlString = '/cmd/pvOutputStart?';


  print('urlString $urlString');
  sendSaveCfgCommand().then((responseString) {
    print("Executed command:$urlString with response: $responseString");
    HttpRequest.getString(urlString).then((responseString) {
      print("Executed command:$urlString with response: $responseString");

    });
  }

  );


}

Future<String> sendPvOutputTestCommand() {
  var urlString = '/cmd/pvOutputTest';

  var encodedData = createPVoutParamsQueryString();

  urlString = "$urlString?${encodedData}";
  print('urlString $urlString');
  print("PVoutput text executed");

  return HttpRequest.getString(urlString);

}


void sendPvOutputStopCommand() {
  var urlString = '/cmd/pvOutputStop?';


  print('urlString $urlString');
  HttpRequest.getString(urlString).then((responseString) {
    print("Executed command:$urlString with response: $responseString");

  });

}

void sendLoadCfgCommand() {
  var urlString = '/cmd/loadCfg?';


  print('urlString $urlString');
  HttpRequest.getString(urlString).then((responseString) {
    handleLoadCfg(responseString);

  });

}

void sendLoadStatusCommand() {
  var urlString = '/cmd/status';


  print('urlString $urlString');
  HttpRequest.getString(urlString).then((responseString) {
    handleStatus(responseString);

  });

}

void savePVoutputSettings() {
  window.localStorage['PVoutUrl'] = (querySelector('#txtPVoutUrl') as InputElement).value;
  window.localStorage['APIkey'] = (querySelector('#txtAPIkey') as InputElement).value;
  window.localStorage['SystemId'] = (querySelector('#txtSystemId') as InputElement).value;

  print("PVoutput settings saved");

}

void loadLocalPVoutSettings() {
  var url = window.localStorage['PVoutUrl'];
  var apiKey = window.localStorage['APIkey'];
  var systemId = window.localStorage['SystemId'];
  (querySelector('#txtPVoutUrl') as InputElement).value = url;
  (querySelector('#txtAPIkey') as InputElement ).value = apiKey;
  (querySelector('#txtSystemId') as InputElement).value = systemId;
  print("PVoutput settings loaded");
}


main() {

//   var jsTab = js.context.callMethod(r'$', ['#tabs a:last']);
  var tab = "pvoutput";


  sendLoadStatusCommand();
  sendLoadCfgCommand();
  // inizializzaizione bottoni
  querySelectorAll('button[data-opcode]').forEach((e) {
    e.onClick.listen((e) {
      // When the button is clicked, it runs this code.

      Element el = e.target;

      Map cmd = {
      };
      var opcode = el.getAttribute('data-opcode');
      var subcode = el.getAttribute('data-subcode');
      cmd['opcode'] = opcode;
      if (subcode != null) {
        cmd['subcode'] = subcode;
      }
      var InputElement = querySelector("#invaddress");
      var address = InputElement.value;
      cmd['address'] = address;

      sendInvCommand(cmd);
      print("Listener fired on button ${el.getAttribute('data-opcode')}");
    });
  });

  // SETTINGS PANE




  // botton PVOutput save
  ButtonElement btnPVoutSave = querySelector("#btnPVoutSave");
  btnPVoutSave.onClick.listen((e) {
    print("Save button pressed");
    ButtonElement thisButton = e.target;

    sendSaveCfgCommand().then((responseString) {
      print("Executed command SaveCfgCommand with response: $responseString");


    });
  });


  // botton PVOutput start
  ButtonElement btnPVoutStart = querySelector("#btnPVoutStart");
  btnPVoutStart.onClick.listen((e) {
    print("Play button pressed");
    ButtonElement thisButton = e.target;

    if (thisButton.getAttribute("class").contains("active")) {
      print("Button already pressed nothing to do");
    } else {
      sendPvOutputStartCommand() ;
    }

  });

  // botton PVOutput Test
  ButtonElement btnPVoutTest = querySelector("#btnPVoutTest");
  btnPVoutTest.onClick.listen((e) {
    print("Test button pressed");
    ButtonElement thisButton = e.target;

    sendPvOutputTestCommand().then((responseString) {
      Map result = JSON.decode(responseString);
      print("responseString :$responseString");
      if (result['result'] == 'OK') {
        var cl = thisButton.getAttribute("class");
        thisButton.setAttribute("class", "$cl btn-success");
        new Timer(new Duration(seconds:1), () => thisButton.setAttribute("class", "$cl"));
      } else {
        var cl = thisButton.getAttribute("class");
        thisButton.setAttribute("class", "$cl btn-danger");
        new Timer(new Duration(seconds:1), () => thisButton.setAttribute("class", "$cl"));
      }
    });
    ;

  });

  // bottone PVOutput stop
  ButtonElement btnPVoutStop = querySelector("#btnPVoutStop");
  btnPVoutStop.onClick.listen((e) {
    print("Stop button pressed");
    ButtonElement thisButton = e.target;

    if (thisButton.getAttribute("class").contains("active")) {
      print("Button already pressed nothing to do");
    } else {
      sendPvOutputStopCommand();

    }

  });

  const statusCheckPeriodSec = const Duration(seconds:30);
  new Timer.periodic(statusCheckPeriodSec, (Timer t) => sendLoadStatusCommand());

}




