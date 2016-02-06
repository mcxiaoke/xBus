# xBus - 简洁的EventBus实现

基于发布订阅(Pub/Sub)模式的一个事件消息库，使用通用的 `register(target)`, `unregister(target)`, `post(event)` 消息通信接口，能有效的减少甚至消除Android应用中异步任务逻辑和界面更新之间的耦合，实现模块化，提高开发效率。

[![Maven Central](http://img.shields.io/badge/2015.08.18-com.mcxiaoke.next:1.0.2-brightgreen.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.mcxiaoke.xbus%22)

* 2015.09.15 **1.0.2** - 修复多线程发送事件的同步问题
* 2015.08.18 **1.0.1** - 修复 `unresiger()` 的空指针问题
* 2015.08.08 **1.0.0** - 基本功能全部完成，发布1.0.0正式版

## 使用指南

### Gradle集成

```groovy
	compile 'com.mcxiaoke.xbus:bus:1.0.+'
```

### 接收事件

```java
public class SimpleActivity extends Activity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注册
        Bus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消注册
        Bus.getDefault().unregister(this);
    }

    @BusReceiver
    public void onStringEvent(String event) {
        // handle your event
        // 这里处理事件
    }

    @BusReceiver
    public void onSomeEvent(SomeEventClass event) {
    	// SomeEventClass表示任意的自定义类
        // handle your event
        // 这里处理事件
    }

    @BusReceiver
    public void onObjectEvent(Object event) {
    	// 不建议使用Object，会收到所有类型的事件
        // handle your event
        // 这里处理事件
    }
}
```

### 发送事件

然后在需要的地方调用 `post(event)` 发送事件通知，如 `Service` 或某个线程里，可以在任何地方发送事件：

```java
// 比如在IntentService里
public class SimpleService extends IntentService {

    public SimpleService() {
        super("SimpleService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
       // 这里是举例，可以在任何地方发送事件
        Bus.getDefault().post("String Event");
        Bus.getDefault().post(new SomeEventClass());
        Bus.getDefault().post(new Object());
    }
}
```

## 高级用法

### 任何地方注册

你还可以选择在 `onStart()` 里注册，在 `onStop()` 里取消注册。你完全可以在任何地方注册和取消注册，没有任何限制。但是建议你在生命周期事件方法里注册和取消注册，如 `Activity/Fragment/Service` 的 `onCreate/onDestroy` 方法里， `register()` 和 `unregister()` 建议配对使用，避免内存泄露。

```java
    @Override
    protected void onStart() {
        super.onStart();
        // you can also register here
        Bus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // you can also unregister here
        Bus.getDefault().unregister(this);
    }
```

### 自定义 `Bus`

你也可以不使用默认的 `Bus.getDefault()`，改用自己创建的 `Bus` 对象：

```java
public class MainApp extends Application {

    private Bus mBus = new Bus();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public Bus getBus() {
        return mBus;
    }
}
```


### Debug

默认不输出任何LOG信息，可以这样启用调试模式：

```java
public Bus setDebug(final boolean debug)
```

### MethodFinder

默认使用注解(`@BusReceiver`)识别事件接收器方法，可以这样修改 ：

```java
public Bus setMethodFinder(final MethodFinder finder)
```

默认使用的是 AnnotationMethodFinder，只有使用了 @BusReceiver 的方法才可以接受事件。

可选使用 `NamedMethodFinder` ，`NamedMethodFinder` 使用方法名识别，默认方法名是 `onEvent` ，你可以指定其它的方法名。

使用 `NamedMethodFinder` 会比使用 `AnnotationMethodFinder` 效率高一点，因为它忽略注解，直接使用方法名字符串匹配。一般使用，两者差别不大。

你还可以实现 `MethodFinder` 接口，自定义其它的事件接收器方法匹配模式：

```
interface MethodFinder {

    Set<MethodInfo> find(final Bus bus, final Class<?> targetClass);
}
```

### StrictMode

#### 宽泛匹配模式

默认情况下， `Bus` 使用宽泛的事件类型匹配模式，事件参数会匹配它的父类和接口，如果你调用 `post(String)`，那么这几个方法都会收到举例：

```java
// 如果你调用这个方法，发送一个StringBuilder类型的事件
Bus.getDefault().post(new StringBuilder("Event"));

// 这几个方法会收到事件
public void onEvent1(StringBuilder event) // 匹配，类型相符
public void onEvent2(Object event) // 匹配，StringBuilder是Object的子类
public void onEvent3(CharSequence event) // 匹配，StringBuilder是CharSequence的实现类
public void onEvent4(Serializable event) // 匹配，StringBuilder实现了Serializable接口

// 这几个方法不会收到事件
public void onEvent5(Exception event) 不匹配，Exception与String完全无关
public void onEvent6(String event) // 不匹配，StringBuilder不能转换成String类型

```

对于 `post(event)` 和 `onEvent(EventType)` ，匹配规则是：如果  `event.getClass()`  可以强制转换成 `EventType`，那么匹配成功，能收到事件。

#### 严格匹配模式

可以使用下面的方法更改默认行为，使用严格的事件类型匹配模式：

```java
public Bus setStrictMode(final boolean strictMode)
```

启用严格匹配模式后，发送和接受方法的参数类型必须严格匹配才能收到事件，举例：

```java
// setStrictMode(true) 启用严格模式后：
Bus.getDefault().post(new StringBuilder("Event"));

// 只有 onEvent1 能收到事件
public void onEvent1(StringBuilder event)
public void onEvent2(Object event)public void onEvent3(CharSequence event) 
public void onEvent4(Serializable event)
public void onEvent5(Exception event)
public void onEvent6(String event) 
```

对于 `post(event)` 和 `onEvent(EventType)` ，严格模式的匹配规则是当且仅当 `event.getClass().equals(EventType)` 时才能收到事件。

说明：启用严格模式效率会稍微高一点，因为不会递归查找 `event` 的父类和实现的接口，但是由于 `Bus` 内部使用了缓存，对于同一个事件类型，并不会重复查找，所以实际使用几乎没有差别。

### StickyEvent

可以使用下面的方法发送 `Sticky` 事件，这种事件会保留在内存中，当下一个注册者注册时，会立即收到上一次发送的该类型事件，每种类型的事件只会保留一个， `Sticky` 事件使用严格匹配模式。

```java
public <E> void postSticky(E event) 
```

一般不需要使用 `Sticky` 事件，但在某些场景下可以用到，比如一个网络状态监听服务，会不断的发送网络状态信息，接受者一旦注册就可以立即收到一个事件，可以知道当前的网络状态。

### @BusEvent

还有一个注解 `@BusEvent` 可用于标注某个类是事件类，这个像 `@Override` 注解一样，纯标注用，没有其它用途，没有运行时消耗。

## 实现教程

* [`跟我一起写EventBus（一）`](docs/how-to-write-an-eventbus-part1.md)
* [`跟我一起写EventBus（二）`](docs/how-to-write-an-eventbus-part2.md)
* [`跟我一起写EventBus（三）`](docs/how-to-write-an-eventbus-part3.md)
* [`跟我一起写EventBus（四）`](docs/how-to-write-an-eventbus-part4.md)
* [`xBus使用教程`](docs/xbus-user-guide.md)

## 项目状态

* 2015.08.03 **0.1.0** 草稿版，实现了一个最简单的可用的原型
* 2015.08.03 **0.2.0** 支持在基类中注册和添加事件接收器方法
* 2015.08.04 **0.6.0** 半成品，支持注册对象和事件类型的继承
* 2015.08.05 **0.7.0** 改进架构，增加扩展支持和错误处理
* 2015.08.06 **0.8.0** 完善了异常处理和简单的测试示例

------

## 关于作者

#### 联系方式
* Blog: <http://blog.mcxiaoke.com>
* Github: <https://github.com/mcxiaoke>
* Email: [mail@mcxiaoke.com](mailto:mail@mcxiaoke.com)

#### 开源项目

* Awesome-Kotlin: <https://github.com/mcxiaoke/awesome-kotlin>
* Kotlin-Koi: <https://github.com/mcxiaoke/kotlin-koi>
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

    Copyright 2015 Xiaoke Zhang

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

