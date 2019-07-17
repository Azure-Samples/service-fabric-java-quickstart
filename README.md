---
page_type: sample
description: "How to deploy your first Java application to Service Fabric using the Eclipse IDE on a Linux developer machine."
languages:
- java
products:
- azure
---

# Service Fabric Java Quickstart

Azure Service Fabric is a distributed systems platform for deploying and managing microservices and containers. 

This quickstart shows how to deploy your first Java application to Service Fabric using the Eclipse IDE on a Linux developer machine. When you're finished, you have a voting application with a Java web front end that saves voting results in a stateful back-end service in the cluster.

In this quickstart, you learn how to:

* Use Eclipse as a tool for your Service Fabric Java applications
* Deploy the application to your local cluster 
* Deploy the application to a cluster in Azure
* Scale-out the application across multiple nodes

## Prerequisites
To complete this quickstart:
1. [Install Service Fabric SDK & Service Fabric Command Line Interface (CLI)](https://docs.microsoft.com/azure/service-fabric/service-fabric-get-started-linux#installation-methods). Service Fabric version 6.1 or higher must be used. 
2. [Install Git](https://git-scm.com/)
3. [Install Eclipse](https://www.eclipse.org/downloads/)
4. [Set up Java Environment](https://docs.microsoft.com/azure/service-fabric/service-fabric-get-started-linux#set-up-java-development), making sure to follow the optional steps to install the Eclipse plug-in 

## Download the sample
In a command window, run the following command to clone the sample app repository to your local machine. 
```
git clone https://github.com/Azure-Samples/service-fabric-java-quickstart.git
```

## Contents 
This repository contains the **Voting** folder which contain the Service Fabric application source code for this quickstart. The **AzureCluster** folder contains assets that is used as part of the more in-depth Service Fabric Java tutorial. This quickstart is a sample on how to use service remoting to communicate betweens services. The [Java Getting Started](https://github.com/Azure-Samples/service-fabric-java-getting-started) repository contains a sample on how to communicate between services using HTTP protocols. 

## Run the application locally
1. Start your local cluster by running the following command:

    ```bash
    sudo /opt/microsoft/sdk/servicefabric/common/clustersetup/devclustersetup.sh
    ```
    The startup of the local cluster takes some time. To confirm that the cluster is fully up, access the Service Fabric Explorer at **http://localhost:19080**. The five healthy nodes indicate the local cluster is up and running. 
2. Open Eclipse.
3. Click File -> Open Projects from File System... 
4. Click Directory and choose the `Voting` directory from the `service-fabric-java-quickstart` folder you cloned from Github. Click Finish.  
5. You now have the `Voting` project in the Package Explorer for Eclipse. 
6. Right click on the project and select **Publish Application...** under the **Service Fabric** dropdown. Choose **PublishProfiles/Local.json** as the Target Profile and click Publish.     
7. Open your favorite web browser and access the application by accessing **http://localhost:8080**. 
    
You can now add a set of voting options, and start taking votes. The application runs and stores all data in your Service Fabric cluster, without the need for a separate database.

## Deploy the application to Azure

### Set up your Azure Service Fabric Cluster
To deploy the application to a cluster in Azure, create your own cluster.

Party clusters are free, limited-time Service Fabric clusters hosted on Azure. They are run by the Service Fabric team where anyone can deploy applications and learn about the platform. To get access to a Party Cluster, [follow the instructions](http://aka.ms/tryservicefabric). 

In order to perform management operations on the secure party cluster, you can use Service Fabric Explorer, CLI, or Powershell. To use Service Fabric Explorer, you will need to download the PFX file from the Party Cluster website and import the certificate into your certificate store (Windows or Mac) or into the browser itself (Ubuntu). There is no password for the self-signed certificates from the party cluster. 

To perform management operations with Powershell or CLI, you will need the PFX (Powershell) or PEM (CLI). To convert the PFX to a PEM file, please run the following command:  

```bash
openssl pkcs12 -in party-cluster-1277863181-client-cert.pfx -out party-cluster-1277863181-client-cert.pem -nodes -passin pass:
```

For information about creating your own cluster, see [Create a Service Fabric cluster on Azure](service-fabric-tutorial-create-vnet-and-linux-cluster.md).

### Add certificate information to your application

Certificate thumbprint needs to be added to your application because it is using Service Fabric programming models. 

1. You will need the thumbprint of your certificate in the ```Voting/VotingApplication/ApplicationManiest.xml``` file when running on a secure cluster. Run the following command to extract the thumbprint of the certificate.

    ```bash
    openssl x509 -in [CERTIFICATE_FILE] -fingerprint -noout
    ```

2. In the ```Voting/VotingApplication/ApplicationManiest.xml```, add the following snippet under the **ApplicationManifest** tag. The **X509FindValue** should be the thumbprint from the previous step (no semicolons). 

    ```xml
    <Certificates>
        <SecretsCertificate X509FindType="FindByThumbprint" X509FindValue="0A00AA0AAAA0AAA00A000000A0AA00A0AAAA00" />
    </Certificates>   
    ```
    
### Deploy the application using Eclipse
Now that the application and your cluster are ready, you can deploy it to the cluster directly from Eclipse.

1. Import the project into eclipse by following the steps mentioned below:
    - Import the project using "Import Projects form File System or Archive" wizard. 
    - Build the project form the Service Fabric context menu. 
    - Perform "Refresh Gradle Project" from Gradle context menu. 
    - Configure your workspace/project ignore Forbidden reference errors. 

2. Open the **Cloud.json** file under the **PublishProfiles** directory and fill in the `ConnectionIPOrURL` and `ConnectionPort` fields appropriately. An example is provided: 

    ```bash
    {
         "ClusterConnectionParameters": 
         {
            "ConnectionIPOrURL": "lnxxug0tlqm5.westus.cloudapp.azure.com",
            "ConnectionPort": "19080",
            "ClientKey": "[path_to_your_pem_file_on_local_machine]",
            "ClientCert": "[path_to_your_pem_file_on_local_machine]"
         }
    }
    ```

2. Right click on the project and select **Publish Application...** under the **Service Fabric** dropdown. Choose **PublishProfiles/Cloud.json** as the Target Profile and click Publish. 


3. Open your favorite web browser and access the application by accessing **http://\<ConnectionIPOrURL>:8080**. 

    
## Scale applications and services in a cluster
Services can be scaled across a cluster to accommodate for a change in the load on the services. You scale a service by changing the number of instances running in the cluster. You have multiple ways of scaling your services, you can use scripts or commands from Service Fabric CLI (sfctl). In this example,we are using Service Fabric Explorer.

Service Fabric Explorer runs in all Service Fabric clusters and can be accessed from a browser, by browsing to the clusters HTTP management port (19080), for example, `http://lnxxug0tlqm5.westus.cloudapp.azure.com:19080`.

To scale the web front-end service, do the following steps:

1. Open Service Fabric Explorer in your cluster - for example, `https://lnxxug0tlqm5.westus.cloudapp.azure.com:19080`.
2. Click on the ellipsis (three dots) next to the **fabric:/Voting/VotingWeb** node in the treeview and choose **Scale Service**.

    You can now choose to scale the number of instances of the web front-end service.

3. Change the number to **2** and click **Scale Service**.
4. Click on the **fabric:/Voting/VotingWeb** node in the tree-view and expand the partition node (represented by a GUID).

    You can now see that the service has two instances, and in the tree view you see which nodes the instances run on.

By this simple management task, we doubled the resources available for our front-end service to process user load. It's important to understand that you do not need multiple instances of a service to have it run reliably. If a service fails, Service Fabric makes sure a new service instance runs in the cluster.

## Next steps
In this quickstart, you learned how to:

* Use Eclipse as a tool for your Service Fabric Java applications
* Deploy Java applications to your local cluster 
* Deploy Java applications to a cluster in Azure
* Scale-out the application across multiple nodes
