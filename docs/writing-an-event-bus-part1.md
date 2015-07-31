# 从零开始写一个EventBus（一）

## 什么是EventBus

先介绍一下概念，EventBus直译过来就是`事件总线`，它使用发布订阅模式支持组件之间的通信，不需要显式地注册回调，比观察者模式更灵活，可用于替换Java中传统的事件监听模式，EventBus的作用就是解耦，它不是通用的发布订阅系统，也不能用于进程间通信。可用于Android的EventBus库主要有这几个：Google出品的`Guava`，`Guava`是一个庞大的库，`EventBus`只是它附带的一个小功能，因此实际项目中使用并不多。用的最多的是[`greenrobot/EventBus`](https://github.com/greenrobot/EventBus)，这个库的优点是接口简洁，集成方便，但是限定了方法名，不支持注解。另一个库[`square/otto`](https://github.com/square/otto)修改自`Guava`，用的人也不少。

以 `greenrobot/EventBus` 为例，我们看一下`EventBus`模式的典型用法：

```java

// 注册EventBus，接受事件
class Fragment {
    public void onCreate(){
       EventBus.getDefault().register(this);
    }
    public void onDestroy(){
       EventBus.getDefault().unregister(this);
    }
    public void onEvent(SomeEvent1 event){
        // handle event
    }
}

// 处理任务，发送事件
public class Service {
    public void doSomeThing(){
        // do your work
        // send event
        EventBus.getDefault().post(new SomeEvent1());
    }
}

```

## 接口定义

我们的目标是从零开始自己写一个`EventBus`，参考上面的示例，我们首先定义接口，假设我们的接口叫`IBus`，实现类叫`Bus`，它最少需要三个方法。为了灵活，我们采用Annotation的方式，不需要限制方法名。

接口定义如下：

```java
public interface IBus {
    
    // register event target
    boolean register(Object target);

    // unregister event target
    boolean unregister(Object target);

    // post event
    void post(Object event);
}
```

最简单的用法如下：

```java
public class BusDemo {

    static class SomeEvent {}

    public static void main(String[] args) {
        new BusDemo().show();
    }

	// 注册EventBus，发送事件
	// 这里为了演示，全部放在一个地方
    public void show() {
        Bus.getDefault().register(this);
        Bus.getDefault().post(new SomeEvent());
        Bus.getDefault().unregister(this);
    }

	// 必须使用这个Annotation
	// 必须是接受一个参数的public方法，没有其它限制
    @BusReceiver
    public void onReceive(SomeEvent event) {
        System.out.println("onReceive() event=" + event);
        // handle your event here...
    }
}

```

## 开始实现

## 测试一下

## 进一步的问题

## 完整代码

* 查看：<https://github.com/mcxiaoke/xBus/tree/v0.1.0>
* 下载：<https://github.com/mcxiaoke/xBus/archive/v0.1.0.zip>

## 参考文档
<http://javarticles.com/2015/04/guava-eventbus-examples.html>
<http://timnew.me/blog/2014/12/06/typical-eventbus-design-patterns/>
<https://code.google.com/p/simpleeventbus/>
<https://github.com/greenrobot/EventBus/blob/master/HOWTO.md>
<https://code.google.com/p/guava-libraries/wiki/EventBusExplained>
<http://www.cnblogs.com/peida/p/eventbus.html>
<http://doc.akka.io/docs/akka/snapshot/java/event-bus.html>
<http://blog.cainwong.com/using-an-eventbus-in-android-pt-1-why-an-eventbus/>
<http://blog.cainwong.com/using-an-eventbus-in-android-pt-2-sticking-your-config/>
<http://blog.cainwong.com/using-an-eventbus-in-android-pt-3-threading/>
