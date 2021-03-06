package ch.rmy.android.http_shortcuts.http;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.apache.http.HttpHeaders;
import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.util.HashMap;
import java.util.Map;

import ch.rmy.android.http_shortcuts.realm.models.Shortcut;
import ch.rmy.android.http_shortcuts.utils.UserAgentUtil;
import okhttp3.Credentials;

class ShortcutRequest extends Request<ShortcutResponse> {

    private final Deferred<ShortcutResponse, VolleyError, Void> deferred;
    private final Map<String, String> parameters = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private String bodyContent;
    private String contentType;

    private ShortcutRequest(int method, String url, final Deferred<ShortcutResponse, VolleyError, Void> deferred) {
        super(method, url, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                deferred.reject(error);
            }
        });
        this.deferred = deferred;
        headers.put(HttpHeaders.CONNECTION, "close");
        headers.put(HttpHeaders.USER_AGENT, UserAgentUtil.getUserAgent());
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        byte[] regularBody = super.getBody();
        byte[] customBody = bodyContent.getBytes();
        if (regularBody == null) {
            return customBody;
        }
        byte[] mergedBody = new byte[regularBody.length + customBody.length];

        System.arraycopy(regularBody, 0, mergedBody, 0, regularBody.length);
        System.arraycopy(customBody, 0, mergedBody, regularBody.length, customBody.length);

        return mergedBody;

    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return headers;
    }

    @Override
    public String getBodyContentType() {
        if (contentType != null) {
            return contentType;
        }
        return super.getBodyContentType();
    }

    @Override
    public Map<String, String> getParams() {
        return parameters;
    }

    @Override
    protected Response<ShortcutResponse> parseNetworkResponse(NetworkResponse response) {
        ShortcutResponse shortcutResponse = new ShortcutResponse(response.statusCode, response.headers, response.data);
        return Response.success(shortcutResponse, null);
    }

    @Override
    protected void deliverResponse(ShortcutResponse response) {
        deferred.resolve(response);
    }

    Promise<ShortcutResponse, VolleyError, Void> getPromise() {
        return deferred.promise();
    }

    static class Builder {

        private final ShortcutRequest request;

        Builder(String method, String url) {
            request = new ShortcutRequest(getMethod(method), url, new DeferredObject<ShortcutResponse, VolleyError, Void>());
        }

        private static int getMethod(String method) {
            switch (method) {
                case Shortcut.METHOD_POST:
                    return Method.POST;
                case Shortcut.METHOD_PUT:
                    return Method.PUT;
                case Shortcut.METHOD_DELETE:
                    return Method.DELETE;
                case Shortcut.METHOD_PATCH:
                    return Method.PATCH;
                case Shortcut.METHOD_OPTIONS:
                    return Method.OPTIONS;
                case Shortcut.METHOD_HEAD:
                    return Method.HEAD;
                case Shortcut.METHOD_TRACE:
                    return Method.TRACE;
                default:
                    return Method.GET;
            }
        }

        Builder basicAuth(String username, String password) {
            request.headers.put(HttpHeaders.AUTHORIZATION, Credentials.basic(username, password));
            return this;
        }

        Builder body(String body) {
            request.bodyContent = body;
            return this;
        }

        Builder parameter(String key, String value) {
            request.parameters.put(key, value);
            return this;
        }

        Builder header(String key, String value) {
            if (key.equalsIgnoreCase(HttpHeaders.CONTENT_TYPE)) {
                request.contentType = value;
            } else {
                request.headers.put(key, value);
            }
            return this;
        }

        Builder timeout(int timeout) {
            request.setRetryPolicy(new DefaultRetryPolicy(timeout, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            return this;
        }

        ShortcutRequest build() {
            return request;
        }

    }

}
