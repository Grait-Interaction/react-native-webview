package com.reactnativecommunity.webview;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.security.KeyChain;
import android.security.KeyChainAliasCallback;
import android.security.KeyChainException;
import android.util.Log;
import android.webkit.ClientCertRequest;
import android.webkit.WebView;

import com.facebook.react.uimanager.ThemedReactContext;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper class for managing certificate requests in web view.
 */
public class WebViewClientCertHelper {

  public static final String PREFS_KEY_ALIAS = "alias";
  public static final String SHARED_PREFS_FOR_KEY_ALIAS = "keyAliasSharedPrefs";
  private static final String TAG = "WebViewClientCertHelper";

  private final RNCWebViewManager.RNCWebView webView;

  private CertAndKey certAndKey = new EmptyCertAndKey();

  public WebViewClientCertHelper(RNCWebViewManager.RNCWebView webView) {
    this.webView = webView;
  }

  /**
   * Web page has requested a client certificate.
   *
   * @param request
   */
  public void onReceivedClientCertRequest(ClientCertRequest request) {
//    Log.i(TAG, "onReceivedClientCertRequest");
    if (Build.VERSION.SDK_INT >= 21) {
      if (certAndKey.hasCert()) {
//        Log.i(TAG, "hasCert. proceeding");
        //If already has selected cert, just proceed with it
        request.proceed(certAndKey.getPrivateKey(), certAndKey.getCertificates());
      } else {
        //Check shared preferences for any previously selected cert alias
        final String previousAlias = getSharedPreferences().getString(PREFS_KEY_ALIAS, null);
//        Log.i(TAG, "checking shared prefs");
        if (previousAlias != null) {
          tryUsingPreviousCert(previousAlias);
        } else {
          //If not has selected cert, display dialog
          chooseCertificate();
        }
      }
    }
  }

  private SharedPreferences getSharedPreferences() {
    return getWebViewContext().getSharedPreferences(SHARED_PREFS_FOR_KEY_ALIAS, Context.MODE_PRIVATE);
  }

  private ThemedReactContext getWebViewContext() {
    return (ThemedReactContext) webView.getContext();
  }

  private Context getApplicationContext() {
    return getWebViewContext().getApplicationContext();
  }

  /**
   * Gets the certificate information for the alias from KeyChain
   *
   * @param alias
   * @return
   */
  @NonNull
  private CertAndKey getFromKeyChain(String alias) {
//    Log.i(TAG, "getFromKeyChain: " + alias);
    try {
      Context context = getApplicationContext();
      X509Certificate[] mCertificates = KeyChain.getCertificateChain(context, alias);
      PrivateKey mPrivateKey = KeyChain.getPrivateKey(context, alias);
      return new CertAndKeyImpl(mCertificates, mPrivateKey, alias);
    } catch (KeyChainException | InterruptedException e) {
      Log.e(TAG, e.getMessage(), e);
    }
    return new EmptyCertAndKey();
  }


  private void tryUsingPreviousCert(String alias) {
//    Log.i(TAG, "tryUsingPreviousCert: " + alias);
    //Using AsyncTask since getFromKeyChain should not run on main thread
    AsyncTask<String, Void, CertAndKey> task = new AsyncTask<String, Void, CertAndKey>() {
      @Override
      protected CertAndKey doInBackground(String... params) {
        return getFromKeyChain(params[0]);
      }

      @Override
      protected void onPostExecute(CertAndKey certAndKey) {
        if (certAndKey.hasCert()) {
          reloadWebViewWithClearedCertificates(certAndKey);
        } else {
          chooseCertificate();
        }
      }
    };
    task.execute(alias);
  }

  public void chooseCertificate() {
//    Log.i(TAG, "chooseCertificate");
    KeyChainAliasCallback callback = new KeyChainAliasCallback() {
      @Override
      public void alias(@Nullable String alias) {
        final CertAndKey certAndKey = alias == null ? new EmptyCertAndKey() : getFromKeyChain(alias);
        webView.post(new Runnable() {
          @Override
          public void run() {
            reloadWebViewWithClearedCertificates(certAndKey);
          }
        });
      }
    };

    KeyChain.choosePrivateKeyAlias(getWebViewContext().getCurrentActivity(), callback,
      null,
      null,
      "localhost",
      -1,
      null);
  }

  private void reloadWebViewWithClearedCertificates(final CertAndKey certAndKey) {
//    Log.i(TAG, "reloadWebViewWithClearedCertificates: " + certAndKey);
    if (Build.VERSION.SDK_INT >= 21) {
      this.certAndKey = certAndKey;
      WebView.clearClientCertPreferences(new Runnable() {
        @Override
        public void run() {
          SharedPreferences.Editor prefsEditor = getSharedPreferences().edit();
          if (certAndKey.hasCert()) {
            prefsEditor.putString(PREFS_KEY_ALIAS, certAndKey.getAlias()).apply();
            webView.reload();
          } else {
            prefsEditor.remove(PREFS_KEY_ALIAS).apply();
            webView.reload();
          }
        }
      });
    }
  }

  public void clearCertificates() {
//    Log.i(TAG, "clearCertificates: " + certAndKey);
    reloadWebViewWithClearedCertificates(new EmptyCertAndKey());
  }

  public interface CertAndKey {
    boolean hasCert();

    X509Certificate[] getCertificates();

    PrivateKey getPrivateKey();

    String getAlias();
  }

  public class EmptyCertAndKey implements CertAndKey {

    @Override
    public boolean hasCert() {
      return false;
    }

    @Override
    public X509Certificate[] getCertificates() {
      return new X509Certificate[0];
    }

    @Override
    public PrivateKey getPrivateKey() {
      return null;
    }

    @Override
    public String getAlias() {
      return null;
    }

    @Override
    public String toString() {
      return "EmptyCertAndKey{}";
    }
  }

  public class CertAndKeyImpl implements CertAndKey {
    private final X509Certificate[] mCertificates;
    private final PrivateKey mPrivateKey;
    private final String alias;

    public CertAndKeyImpl(X509Certificate[] mCertificates, PrivateKey mPrivateKey, String alias) {
      this.mCertificates = mCertificates;
      this.mPrivateKey = mPrivateKey;
      this.alias = alias;
    }

    @Override
    public PrivateKey getPrivateKey() {
      return mPrivateKey;
    }

    @Override
    public X509Certificate[] getCertificates() {
      return mCertificates;
    }

    @Override
    public String getAlias() {
      return alias;
    }

    @Override
    public String toString() {
      return "CertAndKey{alias=" + alias + '}';
    }

    @Override
    public boolean hasCert() {
      return true;
    }
  }
}
