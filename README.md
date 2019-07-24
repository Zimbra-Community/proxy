# Proxy
A replacement for /service/proxy that no longer works on Zimbra 8.8.15.

This extension is to be put in the following location on your server: /opt/zimbra/lib/ext/proxy/extension.jar.

Then you need to configure it like so:

zmprov mc default +zimbraProxyAllowedDomains blog.zimbra.com

The extension does not support wildcarts (aka *.zimbra.com will not work)

Use it like so:
https://example-zimbra.com/service/extension/proxy/?target=https://blog.zimbra.com/feed/

It supports text and binary content, but it is tested for use with API's and RSS feeds.
