# OPENTGF

It is an opensource java project to generate session or event based traffic using IMS, DATA, VOICE and SMS services via Diameter protocol.
With its flexible configurable features, the specified tps can be created simply over the services.

# USING

File client-jdiameter-config.xml located under folder tgf/config;

- Give the Realm you use as a server to the Realm section under the LocalPeer attribute.
- In the Peer section under the Network attribute, the environment ip and port information of the application you use as a server,
  Enter the name and peers in the Realm field.


File sqliteTgf.db  located under folder tgf/config/database;

- Connect to sqliteTgf.db with any database tool you use.
- TGF_CONFIG, TGF_INPUT, TGF_SESSN and TGF_SESSN_DTL tables available.
- There are configurations to be used by the tgf application to the TGF_CONFIG table, you can see the comments in the table columns,
  You can configure and use it the way you want.
- In the TGF_INPUT table you need to enter the information that the application will use to create the Avps.
- The TGF_SESSN and TGF_SESSN_DTL tables are the tables that the application uses to print the statistics after its operation.