/*

Copyright (C) 2016-2019  Barry de Graaff

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses/.


usage: POST file to https://myzimbra.com/service/extension/proxy/?target=https://nextcloud.com/blog/static-feed/
returns a HTTP resource from a URL to bypass cross origin policy
*/

package tk.barrydegraaff.proxy;


import com.zimbra.cs.extension.ExtensionHttpHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.SystemUtil;
import com.zimbra.cs.account.AuthToken;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Cos;

public class Proxy extends ExtensionHttpHandler {

    /**
     * The path under which the handler is registered for an extension.
     *
     * @return path
     */
    @Override
    public String getPath() {
        return "/proxy";
    }

    /**
     * Processes HTTP POST requests.
     *
     * @param req  request message
     * @param resp response message
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        resp.getOutputStream().print("tk.barrydegraaff.proxy is installed. HTTP POST method is not supported");
    }

    /**
     * Processes HTTP GET requests.
     *
     * @param req  request message
     * @param resp response message
     * @throws java.io.IOException
     * @throws javax.servlet.ServletException
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        String authTokenStr = null;
        //Just read a cos value to see if its a valid user
        Cookie[] cookies = req.getCookies();
        for (int n = 0; n < cookies.length; n++) {
            Cookie cookie = cookies[n];

            if (cookie.getName().equals("ZM_AUTH_TOKEN")) {
                authTokenStr = cookie.getValue();
                break;
            }
        }

        Account account = null;
        Set<String> allowedDomains;

        if (authTokenStr != null) {
            try {
                AuthToken authToken = AuthToken.getAuthToken(authTokenStr);
                Provisioning prov = Provisioning.getInstance();
                Account acct = Provisioning.getInstance().getAccountById(authToken.getAccountId());
                Cos cos = prov.getCOS(acct);
                allowedDomains = cos.getMultiAttrSet(Provisioning.A_zimbraProxyAllowedDomains);
            } catch (Exception ex) {
                //crafted cookie? get out you.
                return;
            }
        } else {
            return;
        }
        final Map<String, String> paramsMap = new HashMap<String, String>();

        if (req.getQueryString() != null) {
            String[] params = req.getQueryString().split("&");
            for (String param : params) {
                String[] subParam = param.split("=");
                paramsMap.put(subParam[0], subParam[1]);
            }
        } else {
            //do nothing
            return;
        }
        try {
            if (checkPermissionOnTarget(paramsMap.get("target"), allowedDomains)) {
                resp.getOutputStream().print(readStringFromURL(resp, paramsMap.get("target")));
            } else {
                resp.getOutputStream().print("Proxy domain not allowed");
            }

        } catch (
                Exception e) {
            ZimbraLog.extensions.error(SystemUtil.getStackTrace(e));
            throw new IOException(e);
        }

    }

    private static boolean checkPermissionOnTarget(String url, Set<String> allowedDomains) {
        try {
            String domain = getDomainName(url);
            if (allowedDomains.contains(domain)) {
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public static String readStringFromURL(HttpServletResponse resp, String url) throws Exception {
        URL website = new URL(url);
        URLConnection connection = website.openConnection();
        resp.setContentType(connection.getContentType());
        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream()));

        StringBuilder response = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null)
            response.append(inputLine);

        in.close();

        return response.toString();
    }

    private static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        return uri.getHost();
    }
}
