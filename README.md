## xBus：一个新的EventBus


### 项目状态

* 20150731 **0.1.0** 草稿版，实现了一个最简单的可用的原型
* 20150803 **0.1.1** 教程版，改进了一下代码，添加了第一部分的文档
* 20150803 **0.2.0** 支持在基类中注册和添加事件接收器方法
* 20150804 **0.2.1** 教程版，完善功能，改进代码，添加了第二部分的文档

### 实现教程

* [跟我一起写EventBus（一）](docs/how-to-write-an-eventbus-part1.md)
* [跟我一起写EventBus（二）](docs/how-to-write-an-eventbus-part2.md)

### 项目TODO

- [x] `findAnnotatedMethods`方法没有考虑效率问题，如果某个`target`中有成千上万个方法，这个方法可能比较慢，是需要考虑缓存或其它的优化方法。
- [x] `post(event)`方法需要遍历保存所有目标对象的所有方法，这个在方法数量很大时效率同样存在问题，可以改进一下遍历过程，或者可以加缓存，不用每次都遍历。
- [ ] `Bus`类直接保存了目标对象`target`的强引用，如果使用者忘记调用`unregister(target)`方法取消注册，可能造成内存泄露，任何改进。
- [x] `Bus`的实现没有考虑在多个线程中使用的问题，没有添加任何同步代码，可能会造成内部数据的不同步，或者发生错误。
- [x] `Bus`的实现不支持外部配置，限定了事件接收器方法只能使用`@BusReceiver`，且只能是`public`的，只能带一个参数，能不能支持使用者自定义这些行为。
- [x] `Bus`目前的版本不支持继承，既不支持在基类中注册，也不支持事件接收器方法中参数类型的继承，如何支持。
- [ ] 当前的事件接收器不支持泛型参数，不支持集合类型，如果支持，还可以考虑，如何支持多个事件参数和可变参数类型的事件。
- [x] 当前的`post(event)`方法是在调用者线程执行，在很多情况下，使用者可能需要任务执行的线程和事件接收器的线程是分开的，比如在某个后台线程中执行异步任务，在主线程中接收事件更新界面，这个需要支持用户自定义。
- [ ] 目前的`Bus`实现没有任何异常处理的代码，一个健壮的程序不能缺少完善的异常处理。

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

