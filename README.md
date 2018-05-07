# jython-compile-maven-plugin

修复国内安装官方源访问不对问题(速度, https各种问题),使用镜像源改为阿里云`http://mirrors.aliyun.com/pypi/simple`

```
<dependency>
  <groupId>com.github.yishenggudou</groupId>
  <artifactId>jython-compile-maven-plugin</artifactId>
  <version>0.0.6</version>
</dependency>
```

## 用法

其他参考官方文档,唯一的变化

在configuration里面加了一个 `mirror` 属性

```
<plugin>
                <groupId>com.github.yishenggudou</groupId>
                <artifactId>jython-compile-maven-plugin</artifactId>
                <version>0.0.6</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>jython</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <mirror>http://mirrors.aliyun.com/pypi/simple</mirror>
                    <libraries>
                        <param>sqlalchemy</param>
                    </libraries>
                </configuration>
            </plugin>
```

## note

因为要发仓库,我改了groupId
