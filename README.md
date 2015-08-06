## xBus：一个新的EventBus


### 项目状态

* 20150731 **0.1.0** 草稿版，实现了一个最简单的可用的原型
* 20150803 **0.1.1** 教程版，改进了一下代码，添加了第一部分的文档
* 20150803 **0.2.0** 支持在基类中注册和添加事件接收器方法
* 20150804 **0.2.1** 教程版，完善功能，改进代码，添加了第二部分的文档
* 20150804 **0.6.0** 半成品，支持注册对象和事件类型的继承
* 20150805 **0.7.0** 基本功能完成，改进架构，增加扩展支持和错误处理

### 实现教程

* [跟我一起写EventBus（一）](docs/how-to-write-an-eventbus-part1.md)
* [跟我一起写EventBus（二）](docs/how-to-write-an-eventbus-part2.md)

### 初步计划
 
* 调用register(target)时，查找target和它的基类里满足条件的方法：

    * 使用@BusReceiver注解的public方法(单个参数)
    * 使用onBusEvent作为方法名的public方法(单个参数)
    * 使用IBusEvent作为参数的public方法(单个参数)
    * 使用BusOptions配置的自定义方法名

* 调用post(event)方法时，发送事件通知，需要支持Scheduler：

    * 在调用者线程调用Receiver的方法
    * 在主线程调用Receiver的方法
    * 在其它的线程调用Receiver的方法

* 需要支持一些自定义配置，比如：

    * 执行Receiver方法的默认线程
    * 特殊的Receiver方法名识别

* 一些优化：

    * 方法和注解查找速度优化
    * 使用弱引用避免内存泄露

------

## 关于作者

#### 联系方式
* Blog: <http://blog.mcxiaoke.com>
* Github: <https://github.com/mcxiaoke>
* Email: [mail@mcxiaoke.com](mailto:mail@mcxiaoke.com)

#### 开源项目

* Next公共组件库: <https://github.com/mcxiaoke/Android-Next>
* Gradle渠道打包: <https://github.com/mcxiaoke/gradle-packer-plugin>
* EventBus实现xBus: <https://github.com/mcxiaoke/xBus>
* Rx文档中文翻译: <https://github.com/mcxiaoke/RxDocs>
* MQTT协议中文版: <https://github.com/mcxiaoke/mqtt>
* 蘑菇饭App: <https://github.com/mcxiaoke/minicat>
* 饭否客户端: <https://github.com/mcxiaoke/fanfouapp-opensource>
* Volley镜像: <https://github.com/mcxiaoke/android-volley>

------

## License

    Copyright 2013 - 2015 Xiaoke Zhang

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

