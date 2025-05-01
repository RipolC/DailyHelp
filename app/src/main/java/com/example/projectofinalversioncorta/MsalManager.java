package com.example.projectofinalversioncorta;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.SilentAuthenticationCallback;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;

public class MsalManager {

    private static MsalManager instance;
    private ISingleAccountPublicClientApplication msalApp;
    private IAccount currentAccount;

    private MsalManager(Context context) {
        PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.auth_config,
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        Log.d("MSAL", "MSAL App creada correctamente");
                        msalApp = application;

                    }

                    @Override
                    public void onError(MsalException exception) {
                        Log.e("MSAL", "Error al crear la aplicación MSAL", exception);
                    }
                });

    }

    public static MsalManager getInstance(Context context) {
        if (instance == null) {
            instance = new MsalManager(context.getApplicationContext());
        }
        return instance;
    }

    public void loadAccount(AccountCallback callback) {
        if (msalApp == null) {
            Log.e("MSAL", "MSAL App aún no está inicializada");
            callback.onAccountUnavailable();
            return;
        }

        msalApp.getCurrentAccountAsync(new ISingleAccountPublicClientApplication.CurrentAccountCallback() {
            @Override
            public void onAccountLoaded(@Nullable IAccount account) {
                currentAccount = account;
                if (account != null) {
                    callback.onAccountAvailable(account);
                } else {
                    callback.onAccountUnavailable();
                }
            }

            @Override
            public void onAccountChanged(@Nullable IAccount priorAccount, @Nullable IAccount currentAccount) {
                MsalManager.this.currentAccount = currentAccount;
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Log.e("MSAL", "Error al cargar la cuenta", exception);
                callback.onAccountUnavailable();
            }
        });
    }



    public void signIn(Activity activity, AuthenticationCallback callback) {
        if (msalApp == null) return;
        msalApp.signIn(activity, null, new String[]{"Calendars.Read"}, callback);
    }

    public void signOut(SignOutCallback callback) {
        if (msalApp == null) return;
        msalApp.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                currentAccount = null;
                callback.onSuccess();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                callback.onError(exception);
            }
        });
    }

    public void acquireTokenSilent(TokenCallback callback) {
        if (msalApp == null || currentAccount == null) {
            callback.onError(new MsalClientException("No hay cuenta activa", "no_account"));
            return;
        }

        msalApp.acquireTokenSilentAsync(
                new String[]{"Calendars.Read"},
                currentAccount.getAuthority(),
                new SilentAuthenticationCallback() {
                    @Override
                    public void onSuccess(IAuthenticationResult authenticationResult) {
                        callback.onTokenReceived(authenticationResult.getAccessToken());
                    }

                    @Override
                    public void onError(MsalException exception) {
                        callback.onError(exception);
                    }
                }
        );

    }

    public interface SignOutCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface TokenCallback {
        void onTokenReceived(String token);
        void onError(Exception e);
    }
    public IAccount getCurrentAccount() {
        return currentAccount;
    }

    public interface AccountCallback {
        void onAccountAvailable(IAccount account);
        void onAccountUnavailable();
    }

    public interface MsalReadyCallback {
        void onMsalReady();
    }

    public void whenReady(MsalReadyCallback callback) {
        if (msalApp != null) {
            callback.onMsalReady();
        } else {
            // Espera a que msalApp esté listo
            android.os.Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> whenReady(callback), 100);
        }
    }


}

