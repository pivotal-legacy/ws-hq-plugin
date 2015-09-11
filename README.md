ws-hq-plugin
============
This is a plugin for Hyperic to monitor vFabric(Pivotal) Web Server. This plugin requires that mod_bmx
be enabled on the web server instance. Please see instructions associated with Web Server for information
on enabling this module if not enabled already.


Installation
============
Follow these steps to start using the vFabric Web Server Monitoring Plugin for Hyperic:

1. If necessary, configure each vFabric Web Server instance that you want to monitor with vFabric Hyperic.
In particular, ensure that the BMX module is enabled in each instance.   Note that, in version 5.1 and 5.2(but not newer), all
new vFabric Web Server instances are configured with BMX by default, so you may not need to perform any action
for this step. See [Configure BMX for Monitoring vFabric Web Server Instances](http://pubs.vmware.com/vfabric52/index.jsp?topic=/com.vmware.vfabric.web-server.5.2/web-server/config-mod-bmx.html).

2. Download the [Pivotal Web Server Monitoring Plugin JAR file](http://public.pivotal.com.s3.amazonaws.com/releases/plugins/ws-hq-plugin/com/vmware/vfabric/hyperic/plugin/vfws-plugin/1.5.RELEASE/vfws-plugin.jar).

3. Upload the "vfws-plugin.jar" file to the Hyperic Server using the Hyperic Plugin Manager.  See
[Plugin Manager](http://pubs.vmware.com/vfabricHyperic50/index.jsp?topic=/com.vmware.vfabric.hyperic.5.0/ui-Administration.Plugin.Manager.html).

License
=======
This code is licensed under the GPL v2.0 License. Please see [LICENSE] for full text.
