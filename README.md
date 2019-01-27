# BeautyRPC 
基于Thrift实现的RPC中间件

 ## 使用 
 
 ### 1.引入maven依赖(**坐标待申请**)
 ```
 <dependency>
     <groupId>com.beautyboss.slogen</groupId>
     <artifactId>BeautyRPC</artifactId>
     <version>1.0-SNAPSHOT</version>
 </dependency>
 ```
 ### 2. 注解
 #### 2.1 Server端
 
 实现Service(实现Thrift的Iface接口)并在实现类上注解`@ThriftServer`
 
 #### 2.2 Client端
 
 直接对`.Client`成员变量上增加注解`@ThriftClient`。
 
 ### 3. 增加`Configuration`类,生成`ThriftScannerConfig`对象。
 
 #### 3.1 Server端
 
 ```
  @Bean
 public ThriftScannerConfig scannerConfig() {
     ThriftScannerConfig config = new ThriftScannerConfig();
     // 设置thrift模式为服务端
     config.setThriftModel(ThriftScannerModel.THRIFT_SERVER_MODEL);
     return config;
 }
 ```
 #### 3.2 Client端
 
 ```
 @Bean
 public ThriftScannerConfig scannerConfig() {
     ThriftScannerConfig config = new ThriftScannerConfig();
     // 设置thrift模式为客户端
     config.setThriftModel(ThriftScannerModel.THRIFT_CLIENT_MODEL);
     return config;
 }
 ```
 
 ### 4. 增加配置
 
 读取配置的时候首先会去读取`thrift-rpc.properties`文件，如果该文件存在，则会读取`application.properties`和`application-{env}.properties`文件中的配置，
 对于相同的key,`application-{env}.properties`中的配置会覆盖`application.properties`中的配置。
 
 #### 4.1 Server端
 #### 4.2 Client端

### 5. 启动
#### 5.1 Server端

在`main`函数中调用`SpringThriftApplication.run()`方法启动。

#### 5.2 Client端

Client端无变化，原来该怎么启动还是怎么启动。


### 6.Demo

#### 1 `thrift`文件中定义好接口，生成相应的`java`文件

```demo.thrift
namespace java com.beautyboss.slogen.demo

struct Request {
    1:i64 id,
    2:string word,
}

struct Response {
    1:i64 id,
    2:string data,
}

service HelloWorldService {
    Response sayHello(1:Request request);
}

```

#### 2. Server端

1. 实现`HelloWorldServiceImpl.Iface`接口

```
@ThriftServer
public class HelloWorldServiceImpl implements HelloWorldService.Iface{
    @Override
    public Response sayHello(Request request) throws TException {
        Response response = new Response();
        response.setId(request.getId());
        response.setData("Response from server : word " + request.getWord());
        return response;
    }
}
```

2. 生成`ThriftScannerConfig`

```
@Configuration
public class ThriftConfig {

    @Bean
    public ThriftScannerConfig scannerConfig() {
        ThriftScannerConfig config = new ThriftScannerConfig();
        config.setThriftModel(ThriftScannerModel.THRIFT_SERVER_MODEL);
        return config;
    }
}
```

3. `application.properties`新增配置

TODO

4. 启动

```
@ComponentScans({@ComponentScan("com.beautyboss.slogen.server")})
@SpringBootApplication(scanBasePackages = "com.beautyboss.slogen.server")
public class ServerApplication {

    public static void main(String[] args) {
        SpringThriftApplication.run(ServerApplication.class,args);
    }
}
```

#### 3. Client端

1. 生成`ThriftScannerConfig`

```
@Configuration
public class ThriftConfig {

    @Bean
    public ThriftScannerConfig scannerConfig() {
        ThriftScannerConfig config = new ThriftScannerConfig();
        config.setThriftModel(ThriftScannerModel.THRIFT_CLIENT_MODEL);
        return config;
    }
}
```

2. 添加配置

TODO

3. 直接使用`HelloWorldService.Client`对象

```
@RestController
public class ClientController {

    @ThriftClient
    private HelloWorldService.Client client;

    @GetMapping("/hello")
    public Response sathello(@RequestParam("word") String word) throws TException {

        Request request = new Request();
        request.setId(System.currentTimeMillis());
        request.setWord(word);
        return client.sayHello(request);
    }
}
```

![beuatyrpc.png](https://i.loli.net/2019/01/27/5c4db7571b8e9.png)



