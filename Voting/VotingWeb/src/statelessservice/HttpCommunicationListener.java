// ------------------------------------------------------------
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//  Licensed under the MIT License (MIT). See License.txt in the repo root for license information.
// ------------------------------------------------------------

package statelessservice;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.Headers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.FileInputStream;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URL;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import microsoft.servicefabric.services.communication.client.FabricServicePartitionClient;
import microsoft.servicefabric.services.communication.runtime.CommunicationListener;
import microsoft.servicefabric.services.communication.client.ExceptionHandler;
import microsoft.servicefabric.services.runtime.StatelessServiceContext;
import microsoft.servicefabric.services.client.ServicePartitionKey;
import microsoft.servicefabric.services.remoting.Service;
import microsoft.servicefabric.services.remoting.client.ServiceProxyBase;
import microsoft.servicefabric.services.communication.client.TargetReplicaSelector;
import system.fabric.CancellationToken;

import rpcmethods.VotingRPC;

public class HttpCommunicationListener implements CommunicationListener {

    private static final Logger logger = Logger.getLogger(HttpCommunicationListener.class.getName());
    
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final int STATUS_OK = 200;
    private static final int STATUS_NOT_FOUND = 404; 
    private static final int STATUS_ERROR = 500;
    private static final String RESPONSE_NOT_FOUND = "404 (Not Found) \n";
    private static final String MIME = "text/html";  
    private static final String ENCODING = "UTF-8";
    
    private static final String ROOT = "wwwroot/";
    private static final String FILE_NAME = "index.html";
    private StatelessServiceContext context;
    private com.sun.net.httpserver.HttpServer server;
    private ServicePartitionKey partitionKey;
    private final int port;

    public HttpCommunicationListener(StatelessServiceContext context, int port) {
        this.partitionKey = new ServicePartitionKey(0); 
        this.context = context;
        this.port = port;
    }

    public void start() {
        try {
            logger.log(Level.INFO, "Starting Server");
            server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(this.port), 0);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new RuntimeException(ex);
        }
        
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange t) {
                try {
	                File file = new File(ROOT + FILE_NAME).getCanonicalFile();
	
	                if (!file.isFile()) {
	                  // Object does not exist or is not a file: reject with 404 error.
	                  t.sendResponseHeaders(STATUS_NOT_FOUND, RESPONSE_NOT_FOUND.length());
	                  OutputStream os = t.getResponseBody();
	                  os.write(RESPONSE_NOT_FOUND.getBytes());
	                  os.close();
	                } else {	
	                  Headers h = t.getResponseHeaders();
	                  h.set(HEADER_CONTENT_TYPE, MIME);
	                  t.sendResponseHeaders(STATUS_OK, 0);              
	
	                  OutputStream os = t.getResponseBody();
	                  FileInputStream fs = new FileInputStream(file);
	                  final byte[] buffer = new byte[0x10000];
	                  int count = 0;
	                  while ((count = fs.read(buffer)) >= 0) {
	                    os.write(buffer,0,count);
	                  }
	                  
	                  fs.close();
	                  os.close();
	                }  
                } catch (Exception e) {
                    logger.log(Level.WARNING, null, e);
                }
            }
        });

        server.createContext("/getStatelessList", new HttpHandler() {
            @Override
            public void handle(HttpExchange t) {
                try {                    
                    t.sendResponseHeaders(STATUS_OK,0);
                    OutputStream os = t.getResponseBody();
                    
                    HashMap<String,String> list = ServiceProxyBase.create(VotingRPC.class, new URI("fabric:/VotingApplication/VotingDataService"), partitionKey, TargetReplicaSelector.DEFAULT, "").getList().get();
                    String json = new Gson().toJson(list);
                    os.write(json.getBytes(ENCODING));                   
                    os.close();
                } catch (Exception e) {
                    logger.log(Level.WARNING, null, e);
                }
            }
        });
        
        server.createContext("/removeItem", new HttpHandler() {
            @Override
            public void handle(HttpExchange t) {
                try {
                    OutputStream os = t.getResponseBody();
                    URI r = t.getRequestURI();     

                    Map<String, String> params = queryToMap(r.getQuery());
                    String itemToRemove = params.get("item");                    
                    
                    Integer num = ServiceProxyBase.create(VotingRPC.class, new URI("fabric:/VotingApplication/VotingDataService"), partitionKey, TargetReplicaSelector.DEFAULT, "").removeItem(itemToRemove).get();
                    
                    if (num != 1) 
                    {
                        t.sendResponseHeaders(STATUS_ERROR, 0);
                    } else {
                        t.sendResponseHeaders(STATUS_OK,0);
                    }

                    String json = new Gson().toJson(num);
                    os.write(json.getBytes(ENCODING));
                    os.close();
                } catch (Exception e) {
                    logger.log(Level.WARNING, null, e);
                }

            }
        });
        
        server.createContext("/addItem", new HttpHandler() {
            @Override
            public void handle(HttpExchange t) {
                try {
                    URI r = t.getRequestURI();
                    Map<String, String> params = queryToMap(r.getQuery());
                    String itemToAdd = params.get("item");
                    
                    OutputStream os = t.getResponseBody();
                    Integer num = ServiceProxyBase.create(VotingRPC.class, new URI("fabric:/VotingApplication/VotingDataService"), partitionKey, TargetReplicaSelector.DEFAULT, "").addItem(itemToAdd).get();
                    if (num != 1) 
                    {
                        t.sendResponseHeaders(STATUS_ERROR, 0);
                    } else {
                        t.sendResponseHeaders(STATUS_OK,0);
                    }

                    String json = new Gson().toJson(num);
                    os.write(json.getBytes(ENCODING));
                    os.close();
                } catch (Exception e) {
                    logger.log(Level.WARNING, null, e);
                }
            }
        });
        
        server.setExecutor(null);
        server.start();
    }
    
    private Map<String, String> queryToMap(String query){
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length>1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }

    private void stop() {
        if (null != server)
            server.stop(0);
    }

    @Override
    public CompletableFuture<String> openAsync(CancellationToken cancellationToken) {
        this.start();
                    logger.log(Level.INFO, "Opened Server");
        String publishUri = String.format("http://%s:%d/", this.context.getNodeContext().getIpAddressOrFQDN(), port);
        return CompletableFuture.completedFuture(publishUri);
    }

    @Override
    public CompletableFuture<?> closeAsync(CancellationToken cancellationToken) {
        this.stop();
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void abort() {
        this.stop();
    }
}
