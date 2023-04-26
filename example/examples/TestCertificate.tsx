import React, { useRef } from 'react';
import { View, Text, Button, ScrollView, Alert } from 'react-native';

import WebView from 'react-native-webview';

export default function TestCertificate() {
  const wref = useRef<WebView>(null);

  //
  const onError = (e) => {
    if (wref.current) {
      console.log('httpError', e.nativeEvent);

      if (
        e.nativeEvent.statusCode === 403 &&
        e.nativeEvent.url.includes('malmo.se')
      ) {
        Alert.alert('Error', 'Error', [{ text: 'Ok' }], {
          cancelable: true,
          onDismiss: () => {
            clear();
          },
        });
      }
    }
  };

  return (
    <ScrollView>
      <View style={{ height: 800 }}>
        <Text>CERTIFICATE TEST</Text>

        <Button
          title="Open"
          onPress={() => {
            wref.current?.openCertificateSelector();
          }}
        />

        <Button
          title="Clear"
          onPress={() => {
            wref.current?.clearCertificates();
          }}
        />

        <WebView
          ref={wref}
          source={{ uri: 'https://tempusnu.se' }}
          onReceivedClientCertRequest={() => {
            console.log('onReceivedClientCertRequest - EVENT');
          }}
          onHttpError={onError}
        />
      </View>
    </ScrollView>
  );
}
