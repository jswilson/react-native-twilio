package com.rogchap.react.modules.twilio;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;

import com.twilio.client.Device;
import com.twilio.client.Connection;
import com.twilio.client.ConnectionListener;
import com.twilio.client.DeviceListener;
import com.twilio.client.Twilio;
import com.twilio.client.PresenceEvent;

import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.app.PendingIntent;
import android.support.annotation.Nullable;
import android.util.Log;

public class TwilioModule extends ReactContextBaseJavaModule implements ConnectionListener, DeviceListener {

  private ReactContext _reactContext;
  private Device _phone;
  private Connection _connection;
  private Connection _pendingConnection;
  private IntentReceiver _receiver;

  public class IntentReceiver extends BroadcastReceiver {
    private ConnectionListener _cl;

    public IntentReceiver(ConnectionListener connectionListener) {
        this._cl = connectionListener;
    }

    public void onReceive(Context context, Intent intent) {
        _pendingConnection = (Connection)intent.getParcelableExtra("com.twilio.client.Connection");
        _pendingConnection.setConnectionListener(this._cl);
        _pendingConnection.accept();
        _connection = _pendingConnection;
        _pendingConnection = null;
        sendEvent("deviceDidReceiveIncoming", null);
    }
  }

  public TwilioModule(ReactApplicationContext reactContext) {
    super(reactContext);

    _reactContext = reactContext;
    this._reactContext = reactContext;
    this._receiver = new IntentReceiver(this);
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction("com.rogchap.react.modules.twilio.incoming");
    this._reactContext.registerReceiver(this._receiver, intentFilter);
  }

  private void sendEvent(String eventName, @Nullable Map<String, String> params) {

    if (eventName.equals("connectionDidDisconnect")) {
      Log.e("mytag", "not emitting an event, just dereferncing the DeviceEventEmitter");
      _reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).toString();
      Log.e("mytag", "DONE");
    }
    else {
      _reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, null);
    }
  }

  @Override
  public String getName() {
    return "Twilio";
  }

  @ReactMethod
  public void initWithTokenUrl(String tokenUrl) {
    StringBuilder sb = new StringBuilder();
    try {
      URLConnection conn = new URL(tokenUrl).openConnection();
      InputStream in = conn.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
      String line = "";
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }
    } catch (Exception e) {
    }
    initWithToken(sb.toString());
  }

  @ReactMethod
  public void initWithToken(final String token) {
    final DeviceListener dl = this;

    if (!Twilio.isInitialized()) {
      Twilio.initialize(_reactContext, new Twilio.InitListener() {
        @Override
          public void onInitialized() {
            try {
              if (_phone == null) {
                _phone = Twilio.createDevice(token, dl);
                Intent intent = new Intent();
                intent.setAction("com.rogchap.react.modules.twilio.incoming");
                PendingIntent pi = PendingIntent.getBroadcast(_reactContext, 0, intent, 0);
                _phone.setIncomingIntent(pi);
              }
            } catch (Exception e) {
            }
          }

          @Override
          public void onError(Exception e) {
          }
      });
    }
  }

  @ReactMethod
  public void connect(ReadableMap params) {
  }

  @ReactMethod
  public void disconnect() {
    if (_connection != null) {
      _connection.disconnect();
      _connection = null;
    }
  }

  @ReactMethod
  public void accept() {
  }

  @ReactMethod
  public void reject() {
    _pendingConnection.reject();
  }

  @ReactMethod
  public void ignore() {
    _pendingConnection.ignore();
  }

  @ReactMethod
  public void setMuted(Boolean isMuted) {
    if (_connection != null && _connection.getState() == Connection.State.CONNECTED) {
      _connection.setMuted(isMuted);
    }
  }

  /* ConnectionListener */

  @Override
  public void onConnecting(Connection connection) {
    sendEvent("connectionDidStartConnecting", null);
  }

  @Override
  public void onConnected(Connection connection) {
    sendEvent("connectionDidConnect", null);
  }

  @Override
  public void onDisconnected(Connection connection) {
    if (connection == _connection) {
      _connection = null;
    }
    if (connection == _pendingConnection) {
        _pendingConnection = null;
    }
    sendEvent("connectionDidDisconnect", null);
  }

  @Override
  public void onDisconnected(Connection connection, int errorCode, String errorMessage) {
    Map errors = new HashMap();
    errors.put("err", errorMessage);
    sendEvent("connectionDidFail", errors);
  }

  /* DeviceListener */
  @Override
  public void onStartListening(Device device) {
    this.sendEvent("deviceDidStartListening", null);
  }

  @Override
  public void onStopListening(Device device) {
  }

  @Override
  public void onStopListening(Device inDevice, int inErrorCode, String inErrorMessage) {
  }

  @Override
  public boolean receivePresenceEvents(Device device) {
    return false;
  }

  @Override
  public void onPresenceChanged(Device inDevice, PresenceEvent inPresenceEvent) {
  }
}
