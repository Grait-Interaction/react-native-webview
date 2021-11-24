import React, {Component, useRef} from 'react';
import {View, Text, Button, ScrollView, Alert} from 'react-native';

import WebView from 'react-native-webview';

export default function TestCertificate() {
  const wref = useRef();

  //
  function _open() {
    if (wref.current) {
      (wref.current as WebView).openCertificateSelector();
    }
  }
  //
  function _clear() {
    if (wref.current) {
      (wref.current as WebView).clearCertificates();
    }
  }
  //
  function _onError(e) {
    if (wref.current) {
      console.log('httpError', e.nativeEvent);

      if (
        e.nativeEvent.statusCode === 403 &&
        e.nativeEvent.url.includes('malmo.se')
      ) {
        Alert.alert('Error', 'Error', [{text: 'Ok'}], {
          cancelable: true,
          onDismiss: () => {
            _clear();
          },
        });
      }
    }
  }

  return (
    <ScrollView>
      <View style={{height: 800}}>
        <Text>CERTIFICATE TEST</Text>

        <Button title="Open" onPress={_open} />

        <Button title="Clear" onPress={_clear} />

        <WebView
          source={{uri: 'https://tempusnu.se'}}
          onReceivedClientCertRequest={() => {
            console.log('onReceivedClientCertRequest - EVENT');
          }}
          onHttpError={_onError}
          ref={(ref: any) => (wref.current = ref)}
        />
      </View>
    </ScrollView>
  );
}
