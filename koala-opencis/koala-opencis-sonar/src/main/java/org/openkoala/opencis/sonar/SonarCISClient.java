package org.openkoala.opencis.sonar;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.openkoala.opencis.CISClientBaseRuntimeException;
import org.openkoala.opencis.api.CISClient;
import org.openkoala.opencis.api.Developer;
import org.openkoala.opencis.api.Project;

public class SonarCISClient implements CISClient {

    protected static final String SONAR_PERMISSION_USER = "user";

    private SonarConnectConfig connectConfig;

    private CloseableHttpClient httpClient;

    private HttpClientContext localContext;

    public SonarCISClient(SonarConnectConfig connectConfig) {
        this.connectConfig = connectConfig;
    }

    @Override
    public void close() {
        try {
            if (null != httpClient) {
                httpClient.close();
            }
        } catch (IOException e) {
            throw new CISClientBaseRuntimeException("sonar.httpclient.closeFailure");
        }
    }


    @Override
    public void createProject(Project project) {
        project.validate();
        HttpPost httpPost = new HttpPost(connectConfig.getAddress() + "/api/projects/create");
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("key", getKeyOf(project)));
        params.add(new BasicNameValuePair("name", project.getProjectName()));
        CloseableHttpResponse response = null;
        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost, localContext);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
                    || response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
                //删除anyone组的权限
                removeAnyOnePermission(project);
                return;
            }
            throw new CISClientBaseRuntimeException("sonar.createProjectFailure");
        } catch (IOException e) {
            throw new CISClientBaseRuntimeException("sonar.createProjectFailure", e);
        } finally {
            closeResponse(response);
        }

    }

    private void removeAnyOnePermission(Project project) {
        HttpPost httpPost1 = new HttpPost(connectConfig.getAddress() + "/api/permissions/remove");
        List<NameValuePair> params1 = new ArrayList<NameValuePair>();
        params1.add(new BasicNameValuePair("permission", "user"));
        params1.add(new BasicNameValuePair("group", "anyone"));
        params1.add(new BasicNameValuePair("component", getKeyOf(project)));
        CloseableHttpResponse response = null;
        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params1, "UTF-8");
            httpPost1.setEntity(entity);
            response = httpClient.execute(httpPost1, localContext);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return;
            }

            throw new CISClientBaseRuntimeException("sonar.removeAnyOnePermissionFailure : " + response.getStatusLine());

        } catch (UnsupportedEncodingException e) {
            throw new CISClientBaseRuntimeException("sonar.removeAnyOnePermission.UnsupportedEncodingException", e);
        } catch (ClientProtocolException e) {
            throw new CISClientBaseRuntimeException("sonar.removeAnyOnePermission.ClientProtocolException", e);
        } catch (IOException e) {
            throw new CISClientBaseRuntimeException("sonar.removeAnyOnePermission.IOException", e);
        } finally {
            closeResponse(response);
        }

    }

    @Override
    public void removeProject(Project project) {
        HttpDelete httpDelete = new HttpDelete(connectConfig.getAddress() + "/api/projects/" + getKeyOf(project));
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(httpDelete, localContext);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return;
            }
            throw new CISClientBaseRuntimeException("sonar.deleteProjectFailure");
        } catch (IOException e) {
            throw new CISClientBaseRuntimeException("sonar.deleteProjectFailure", e);
        } finally {
            closeResponse(response);
        }

    }

    @Override
    public void createUserIfNecessary(Project project, Developer developer) {

        HttpPost httpPost = new HttpPost(connectConfig.getAddress() + "/api/users/create");
        CloseableHttpResponse response = null;
        try {

            httpPost.setEntity(createDeveloperHttpEntityBy(developer));
            response = httpClient.execute(httpPost, localContext);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return;
            }
            //已存在用户
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
                return;
            }

            // TODO
            throw new CISClientBaseRuntimeException("sonar.createUserIfNecessaryFailure");
        } catch (IOException e) {
            throw new CISClientBaseRuntimeException("sonar.createUserIfNecessaryFailure", e);
        } finally {
            closeResponse(response);
        }
    }

    private HttpEntity createDeveloperHttpEntityBy(Developer developer) {
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("login", developer.getId()));
        params.add(new BasicNameValuePair("password", developer.getPassword()));
        params.add(new BasicNameValuePair("password_confirmation", developer.getPassword()));
        params.add(new BasicNameValuePair("name", developer.getName()));
        params.add(new BasicNameValuePair("email", developer.getEmail()));
        try {
            return new UrlEncodedFormEntity(params, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new CISClientBaseRuntimeException(e);
        }
    }

    public boolean existsUser(String developerId) {
        authenticate();
        CloseableHttpResponse response = null;
        try {
            HttpGet http = new HttpGet(connectConfig.getAddress() + "/api/users/search?includeDeactivated=true&logins="
                    + developerId);
            response = httpClient.execute(http, localContext);
            String strResult = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return strResult.contains(developerId);
            }
            return false;
        } catch (IOException e) {
            throw new CISClientBaseRuntimeException("sonar.existsUserRequestFailure", e);
        } finally {
            closeResponse(response);
        }

    }

    /**
     * 检测用户是否为激活状态
     *
     * @param developerId
     * @return
     */
    public boolean isActivated(String developerId) {
        authenticate();
        CloseableHttpResponse response = null;
        try {
            HttpGet http = new HttpGet(connectConfig.getAddress() + "/api/users/search?includeDeactivated=true&logins=" + URLEncoder.encode(developerId, "UTF-8"));
            response = httpClient.execute(http, localContext);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String result = EntityUtils.toString(response.getEntity());
                return result.contains(developerId) && result.contains("\"active\":true");
            }
            return false;
        } catch (IOException e) {
            throw new CISClientBaseRuntimeException("sonar.existsUserRequestFailure", e);
        } finally {
            closeResponse(response);
        }
    }

    @Override
    public void removeUser(Project project, Developer developer) {
        authenticate();
        if (!existsUser(developer.getId())) {
            return;
        }
        HttpPost httpPost = new HttpPost(connectConfig.getAddress() + "/api/users/deactivate");
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("login", developer.getId()));
        CloseableHttpResponse response = null;
        try {
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
            httpPost.setEntity(entity);
            response = httpClient.execute(httpPost, localContext);

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                return;
            }
            throw new CISClientBaseRuntimeException("sonar.removeUserFailure");
        } catch (IOException e) {
            throw new CISClientBaseRuntimeException("sonar.removeUserFailure", e);
        } finally {
            closeResponse(response);
        }
    }


    @Override
    public void assignUsersToRole(Project project, String role, Developer... developers) {
        authenticate();
        project.validate();
        for (Developer each : developers) {
            HttpPost httpPost = new HttpPost(connectConfig.getAddress() + "/api/permissions/add");
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("permission", SONAR_PERMISSION_USER));
            params.add(new BasicNameValuePair("user", each.getId()));
            params.add(new BasicNameValuePair("component", getKeyOf(project)));
            CloseableHttpResponse response = null;


            try {
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
                httpPost.setEntity(entity);
                response = httpClient.execute(httpPost, localContext);


                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    return;
                }

                if (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
                    throw new CISClientBaseRuntimeException("sonar.NotFoundUser");
                }

                // TODO
                throw new CISClientBaseRuntimeException("sonar.assignUsersToRoleFailure");
            } catch (IOException e) {
                throw new CISClientBaseRuntimeException("sonar.assignUsersToRoleFailure", e);
            } finally {
                closeResponse(response);
            }
        }

    }

    @Override
    public boolean authenticate() {

        httpClient = HttpClientBuilder.create().setDefaultCredentialsProvider(getCredentialsProvider()).build();
        localContext = createLocalContext();
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(
                    new HttpGet(connectConfig.getAddress() + "/api/authentication/validate"),
                    localContext);
            String str = EntityUtils.toString(response.getEntity());
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK
                    && str.contains(":true");
        } catch (IOException e) {
            throw new CISClientBaseRuntimeException("sonar.authenticateFailure", e);
        } finally {
            closeResponse(response);
        }
    }

    protected static String getKeyOf(Project project) {
        return project.getGroupId() + ":" + project.getArtifactId();
    }

    private HttpHost getHttpHostByConnectConfig(SonarConnectConfig connectConfig) {
        return new HttpHost(connectConfig.getHost(), connectConfig.getPort(), connectConfig.getProtocol());

    }

    private CredentialsProvider getCredentialsProvider() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(connectConfig.getUsername(), connectConfig.getPassword()));

        return provider;
    }

    private HttpClientContext createLocalContext() {
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(getHttpHostByConnectConfig(connectConfig), basicAuth);

        HttpClientContext result = HttpClientContext.create();
        result.setAttribute(ClientContext.AUTH_CACHE, authCache);
        result.setCookieStore(new BasicCookieStore());
        return result;
    }

    private void closeResponse(CloseableHttpResponse response) {
        if (null == response) return;

        try {
            response.close();
        } catch (IOException e) {
            throw new CISClientBaseRuntimeException(e);
        }
    }

}
