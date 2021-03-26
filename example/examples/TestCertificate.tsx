import React, {Component, useRef} from 'react';
import { View, Text, Button } from 'react-native';

import WebView from 'react-native-webview';

export default function TestCertificate(){

  const wref = useRef();

  // 
  function _open(){
    if(wref.current){
      console.log(wref.current);
      (wref.current as WebView).openCertificateSelector();
    }
  }

  return <View style={{ height: 400 }}>
    <Text>CERTIFICATE TEST</Text>

    <Button title="Open" onPress={_open} />

    <WebView 
      source={{ uri: 'https://google.se' }}
      onReceivedClientCertRequest={()=>{
        console.log('onReceivedClientCertRequest - EVENT')
      }}
      ref={ (ref: any) => wref.current = ref}
    />
  </View>
}