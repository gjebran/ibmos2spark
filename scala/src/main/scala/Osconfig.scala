package com.ibm.ibmos2spark

import scala.collection.mutable.HashMap
import org.apache.spark.SparkContext

import org.apache.hadoop.fs.FSDataOutputStream
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

import java.net.URI

/** 
* softlayer class sets up a swift connection between an IBM Spark service
* instance and Softlayer Object Storgae instance. 
*  
* Constructor arguments

*    sparkcontext: a SparkContext object.
* 
*    name: string that identifies this configuration. You can
*      use any string you like. This allows you to create
*      multiple configurations to different Object Storage accounts.
*
*    auth_url, tenant, username and password:  string credentials for your
*      Softlayer Object Store
*/

class softlayer(sc: SparkContext, val name: String, auth_url: String, 
                  tenant: String, username: String, password: String, 
                  swift2d_driver: String = "com.ibm.stocator.fs.ObjectStoreFileSystem",
                  public: Boolean=false){
    
    
    val hadoopConf = sc.hadoopConfiguration;
    val prefix = "fs.swift2d.service." + name 

    hadoopConf.set("fs.swift2d.impl",swift2d_driver)
    hadoopConf.set(prefix + ".auth.url",auth_url)
    hadoopConf.set(prefix + ".username", username)
    hadoopConf.set(prefix + ".tenant", tenant)
    hadoopConf.set(prefix + ".auth.endpoint.prefix","endpoints")
    hadoopConf.set(prefix + ".auth.method","swiftauth")
    hadoopConf.setInt(prefix + ".http.port",8080)
    hadoopConf.set(prefix + ".apikey",password)
    hadoopConf.setBoolean(prefix + ".public",public)
    hadoopConf.set(prefix + ".use.get.auth","true")
    hadoopConf.setBoolean(prefix + ".location-aware",false)
    hadoopConf.set(prefix + ".password",password)

    
     // val ptemplate:String  = s"swift2d://dummycontainer.$name/dummyobjectname";
    // lazy val fs:FileSystem = FileSystem.get(URI.create(ptemplate), hadoopConf);

    def url(container_name: String, object_name:String) : String= {
        return s"swift2d://$container_name.$name/$object_name"
    }

    var currentContainer:String = _
    var fs:FileSystem = _

    def setup(container: String, objectname: String) {
        if (container != currentContainer) {
          currentContainer = container
          fs = FileSystem.get(URI.create(url(container,objectname)), mConf)
        } 
    }

    //lazy val fs:FileSystem = FileSystem.get(URI.create(ptemplate), mConf);


    def put(container: String, objectname: String, data: Array[Byte]) = {
        setup(container,objectname)
        val out: FSDataOutputStream =  fs.create(new Path(url(container,objectname)));
        out.write(data);
        out.close();
    }

    def delete(container: String, objectname: String ) {
        setup(container,objectname)
        fs.delete(new Path(url(container,objectname)), false);
    } 

    def get(container: String, objectname: String, data: Array[Byte]) = {
        setup(container,objectname)
        val in: FSDataInputStream =  fs.open(new Path(url(container, objectname)));
        ///not sure what to do here... need a fill a byte buffer? Ugg. 
        //I suppose I could get the file size information first, then create the buffer
    }

}

/** 
* bluemix class sets up a swift connection between an IBM Spark service
* instance and an Object Storage instance provisioned through IBM Bluemix.

* Constructor arguments:

*   sparkcontext:  a SparkContext object.

*   credentials:  a dictionary with the following required keys:
*   
*     auth_url

*     project_id (or projectId)

*     user_id (or userId)

*     password

*     region
* 
*   name:  string that identifies this configuration. You can
*     use any string you like. This allows you to create
*     multiple configurations to different Object Storage accounts.
*     This is not required at the moment, since credentials['name']
*     is still supported.
* 
* When using this from a IBM Spark service instance that
* is configured to connect to particular Bluemix object store
* instances, the values for these credentials can be obtained
* by clicking on the 'insert to code' link just below a data
* source.
*/

class bluemix(sc: SparkContext, val name: String, creds: HashMap[String, String],
                swift2d_driver: String = "com.ibm.stocator.fs.ObjectStoreFileSystem", 
                public: Boolean =false){
    

    def ifexist(credsin: HashMap[String, String], var1: String, var2: String): String = {
        if (credsin.keySet.exists(_ == var1)){
            return(credsin(var1))
        }else {
           return(credsin(var2))
        }
    }

    val username = ifexist(creds, "user_id","userId")
    val tenant = ifexist(creds, "project_id","projectId")

    
    val hadoopConf = sc.hadoopConfiguration;
    val prefix = "fs.swift2d.service." + name;

    hadoopConf.set("fs.swift2d.impl",swift2d_driver)

    hadoopConf.set(prefix + ".auth.url",creds("auth_url") + "/v3/auth/tokens")
    hadoopConf.set(prefix + ".auth.endpoint.prefix","endpoints")
    hadoopConf.set(prefix + ".auth.method","keystoneV3")
    hadoopConf.set(prefix + ".tenant",tenant)
    hadoopConf.set(prefix + ".username",username)
    hadoopConf.set(prefix + ".password",creds("password"))
    hadoopConf.setBoolean(prefix + ".public",public)
    hadoopConf.set(prefix + ".region",creds("region"))
    hadoopConf.setInt(prefix + ".http.port",8080)

    // val ptemplate:String  = s"swift2d://dummycontainer.$name/dummyobjectname";
    // lazy val fs:FileSystem = FileSystem.get(URI.create(ptemplate), hadoopConf);

    def url(container_name: String, object_name:String) : String= {
        return s"swift2d://$container_name.$name/$object_name"
    }

    var currentContainer:String = _
    var fs:FileSystem = _

    def setup(container: String, objectname: String) {
        if (container != currentContainer) {
          currentContainer = container
          fs = FileSystem.get(URI.create(url(container,objectname)), mConf)
        } 
    }

    //lazy val fs:FileSystem = FileSystem.get(URI.create(ptemplate), mConf);


    def put(container: String, objectname: String, data: Array[Byte]) = {
        setup(container,objectname)
        val out: FSDataOutputStream =  fs.create(new Path(url(container,objectname)));
        out.write(data);
        out.close();
    }

    def delete(container: String, objectname: String ) {
        setup(container,objectname)
        fs.delete(new Path(url(container,objectname)), false);
    } 

    def get(container: String, objectname: String, data: Array[Byte]) = {
        setup(container,objectname)
        val in: FSDataInputStream =  fs.open(new Path(url(container, objectname)));
        ///not sure what to do here... need a fill a byte buffer? Ugg. 
        //I suppose I could get the file size information first, then create the buffer
    }

}


