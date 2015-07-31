## 一个简单的EventBus实现 


### 项目状态

* 20150731 **0.1.0** 草稿版，实现了一个最简单的可用的原型

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
