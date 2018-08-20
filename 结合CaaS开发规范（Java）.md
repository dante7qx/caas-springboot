## 结合CaaS开发规范

### 一. 配置文件的使用

推荐使用 yml 格式的配置文件。

#### 1. 使用 @Value 注解读取配置文件

```java
....
import org.springframework.beans.factory.annotation.Value;

@Value("${db.name}")
private String dbName;
```

#### 2. 将配置转化成 Bean（推荐）

- 配置文件 application-db.yml

```yaml
db:
  name: mysql
  user: root
```

- 创建类 DBProperties.java

```java
@Component
@ConfigurationProperties(prefix = "db")
public class DBProperties {

	private String name;
	private String user;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}
}
```

- 使用

```java
@RestController
public class HelloController {
    @Autowired
	private DBProperties dbProp;
    
    @GetMapping("/db")
	public String db() {
		return "db.name -> " + dbProp.getName() + ", db.user" + dbProp.getUser();
	}
}
```

#### 3. 通过PropertiesUtils获取（极不推荐）

​	系统部署时，配置文件中的配置可以在CaaS平台中通过环境变量进行设置，但CaaS中环境变量的Key只能使用下横线"_"进行分隔，例如：

```properties
配置文件中： db.name=mysql
CaaS中：	  db_name=mysql
```

​	因此，通过JDK中Properties 获取配置文件中的配置时，需要先从环境变量中进行获取，并且将分隔符中的 点“.”、 中横线"-" 用下横线替换，如果环境变量中获取不到指定key的值，再从配置文件中获取。

```java
public class ConfigReader {
	private static final Logger LOG = LoggerFactory.getLogger(ConfigReader.class);
	protected Map<String, String> constantsMap;

	private ConfigReader() {
		constantsMap = Maps.newHashMap();
	}

	public static ConfigReader getInstance(String[] filePaths) {
		ConfigReader configReader = new ConfigReader();

		for (String filePath : filePaths) {
			Properties prop = new Properties();
			if (StringUtils.isEmpty(filePath) || !filePath.endsWith(".properties")) {
				throw new IllegalArgumentException("only properties to read");
			}
			InputStream in = ConfigReader.class.getClassLoader().getResourceAsStream(filePath);

			try {
				if (in == null) {
					in = new FileInputStream(new File(filePath));
				}
				prop.load(in);
				prop.forEach((k, v) -> {
					configReader.constantsMap.put((String) k, (String) v);
				});
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException(e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						LOG.warn("fail to close inputstream of " + filePath);
					}
				}
			}
		}

		return configReader;
	}

	/**
	 * 先从环境变量读取，将key中的"." 和 "-" 用 "_" 替换，若环境变量中没有值，再从配置文件获取
	 * 
	 * @param key
	 * @return
	 */
	public String get(String key) {
		String val = System.getenv().get(key.replaceAll("\\.", "_").replaceAll("-", "_"));
		if (StringUtils.isEmpty(val)) {
			if (constantsMap.containsKey(key)) {
				return trimValue(key);
			} else {
				return "";
			}
		} else {
			return val.trim();
		}
	}

	public boolean getBoolean(String key) {
		return Boolean.valueOf(get(key));
	}

	private String trimValue(String key) {
		return constantsMap.get(key).trim();
	}

	public String getString(String key, String defaultValue) {
		return StringUtils.isEmpty(get(key)) ? defaultValue : get(key);
	}

	public int getInt(String key, int defaultValue) {
		return StringUtils.isEmpty(get(key)) ? defaultValue : Integer.parseInt(get(key));
	}

	public float getFloat(String key, float defaultValue) {
		return StringUtils.isEmpty(get(key)) ? defaultValue : Float.valueOf(get(key));
	}

	public int getInt(String key) {
		return StringUtils.isEmpty(get(key)) ? -1 : Integer.parseInt(get(key));
	}

}
```

### 二. 多配置切换

对于Java系统，部署时需要切换配置文件，例如有三个配置文件

- 核心配置文件：application.properties （或 application.yml）

  公共的配置，与环境无关，项目启动时都会加载。

- 开发环境用的配置文件：application-dev.properties （或 application-dev.yml）

- 生产环境用的配置文件：application-pro.properties（或 application-pro.yml）

通过在**application.properties（或 application.yml）**中的 **spring.profiles.active** 可以进行动态的切换配置文件。

部署时，在 CaaS 中配置环境变量

```properties
# 使用配置文件 application-dev.properties （或 application-dev.yml 
spring_profiles_active = dev

# 使用配置文件 application-pro.properties （或 application-pro.yml）
spring_profiles_active = pro

# 若项目中还有其他配置文件，要在项目启动时一同加载，例如 
# application-db.properties（application-db.yml)
# application-zk.properties（application-zk.yml）
# 同时使用 application-pro、application-db、application-zk 三个配置文件
spring_profiles_active = pro,db,zk
```

### 三. 统一打包规范

​	对于基于 springboot 开发的项目，可打成一个可用**Java**运行的 jar 包或 war 包，不需要在部署时额外的安装Tomcat，直接通过 `java -jar xx.jar` 或 `java -jar xx.war` 即可。

​	这样，用于生成镜像的 Dockerfile 就可以统一（仅替换 jar/ war 包名字即可）。例如

```dockerfile
FROM java:8
LABEL Author=Dante

ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENV APPS_HOME=/AppServer
RUN mkdir -p $APPS_HOME/
WORKDIR $APPS_HOME/

## 每个项目按实际情况修改 xxx.jar 的名字和运行时的内存配置
ADD xxx.jar $APPS_HOME/app.jar
ENV JAVA_OPTS="-Xms4096m -Xmx4096m"

RUN sh -c 'touch $APPS_HOME/app.jar'
RUN chmod 644 $APPS_HOME/app.jar

EXPOSE 8080
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar $APPS_HOME/app.jar" ]
```

- 打包配置

  在实际运行项目的pom.xml进行配置

  ```xml
  ...
  
  <build>
      <plugins>
          <plugin>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-maven-plugin</artifactId>
              <configuration>
                  <!-- 项目的启动类 -->
                  <mainClass>org.dante.springboot.S2IDemoApplication</mainClass>
              </configuration>
              <executions>
                  <execution>
                      <goals>
                          <goal>repackage</goal>
                      </goals>
                  </execution>
              </executions>
          </plugin>
      </plugins>
  </build>
  
  ...
  ```

  



