# 跟我一起写EventBus（三）

在 [`跟我一起写EventBus（一）`](how-to-write-an-eventbus-part1.md) 里实现了一个非常粗糙的`EventBus`，在 [`跟我一起写EventBus（二）`](how-to-write-an-eventbus-part2.md) ，又增加了基类中注册和事件类型宽泛匹配的功能，这一节需要加上在不同线程分发事件的功能，下面会详细解释事件的分发流程。

在不同的线程分发事件（即在指定的线程调用使用了 `@BusReceiver` 注解的事件接收器的方法），主要支持三种线程：

1. 事件发送者（调用 `post(event)`方法）所在的线程，这是上一版的处理方式，对于大部分场景来说，这都不太合适；
2. 主线程（UI线程），对Android来说，异步任务完成后一般是更新界面，而更新UI必须在主线程中操作，所以这是一个主要用例；
3. 独立线程，另外维护一个线程池，在单独的线程中处理事件，这个适用于需要在事件接收器方法中进行耗时操作的情况，可以避免堵塞发送者线程或主线程。


## Scheduler接口

在不同的线程分发事件，需要一个调度器，这里定义一个简单的调度器 `Scheduler` 接口：

```java
interface Scheduler {
    void post(Runnable runnable);
}

// 这和java.util.concurrent.Executor的接口几乎是一样的，其实可以直接使用这个接口，后面接口的实现需要扩充一些功能
```

调度器的作用很简单，就是执行一个 `Runnable` 定义的任务，可以是同步的、异步的，具体看实现类的定义。

### 发送者线程分发

发送者线程分发是最简单的，直接调用事件接收器的方法即可，使用 `Scheduler` 接口的实现就是：

```java
class SenderScheduler implements Scheduler {
    private Bus mBus;

    public SenderScheduler(final Bus bus) {
        mBus = bus;
    }

    @Override
    public void post(final Runnable runnable) {
    // 直接调用方法，就是在调用者线程
        runnable.run();
    }
}
```

### 主线程分发

针对Android系统的特点，要在主线程分发事件，需要用到 `Handler` ，再定义一个使用Handler的调度器：

```java
class HandlerScheduler implements Scheduler {
    private Bus mBus;
    private Handler mHandler;

    public HandlerScheduler(final Bus bus, final Handler handler) {
        mBus = bus;
        mHandler = handler;
    }

    @Override
    public void post(final Runnable runnable) {
        mHandler.post(runnable);
    }
}
```

有了 `HandlerScheduler` 这个类，要实现在主线程分发事件就简单了：

```java
   static Scheduler getMainThreadScheduler(final Bus bus) {
        return new HandlerScheduler(bus, new Handler(Looper.getMainLooper()));
    }
```

### 独立线程分发

要在独立线程分发事件，可以使用并发框架里的 `Executor` ，不用额外的维护线程的创建和终止：

```java
class ExecutorScheduler implements Scheduler {
    private Bus mBus;
    private Executor mExecutor;

    public ExecutorScheduler(final Bus bus) {
        mBus = bus;
        mExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void post(final Runnable runnable) {
    // 在线程池中执行任务
        mExecutor.execute(runnable);
    }
}
```

## Scheduler使用

### 定义

最后创建三种调度器的工厂方法，调度器这边就准备好了：

```java
public final class Schedulers {

    public static Scheduler sender(final Bus bus) {
        return new SenderScheduler(bus);
    }

    public static Scheduler getMainThreadScheduler(final Bus bus) {
        return new HandlerScheduler(bus, new Handler(Looper.getMainLooper()));
    }

    public static Scheduler thread(final Bus bus) {
        return new ExecutorScheduler(bus);
    }
}
```

### 事件模式

在`Bus`类中使用Enum定义三种事件分发模式，在Android应用中，绝大部分的事件处理都是更新界面，因为这里把主线程分发定为默认模式：

```java
    /**
     * 事件发送模式：
     *
     * Sender - 在发送者的线程调用@BusReceiver/onEvent方法
     * Main - 在主线程调用@BusReceiver/onEvent方法（默认为此模式）
     * Thread - 在一个单独的线程调用@BusReceiver/onEvent方法
     */
    public enum EventMode {

        Sender, Main, Thread
    }
```

### 使用方法

在事件接收器方法中使用mode指定分发模式，默认是主线程分发，示例如下

```java
    // default is main thread
    public void onEvent0(String event){
        // handle event 
    }
    
    @BusReceiver(mode= EventMode.Main)
    public void onEvent1(String event){
       // handle event 
    }

    @BusReceiver(mode= EventMode.Sender)
    public void onEvent2(String event){
        // handle event 
    }

    @BusReceiver(mode= EventMode.Thread)
    public void onEvent3(String event){
        // handle event 
    }
```


## 事件分发流程

目标调用 `Bus.getDefault().register(target)` 的时候，查找目标对象中包含的事件接收器方法，然后构造 `MethodInfo` 对象和 `Subscriber` 对象，保存在Map中，同时保存事件类型和 `Subscriber` 直接的对应关系，发送者调用 `post(event)` 时会从Map中查找事件对应的订阅者，然后根据事件分发模式使用 `Scheduler` 分发事件，大概流程就是这样的，下面是几个重要的数据结构。

### EventEmitter

`Scheduler` 接受的是 `Runnable` 参数，因此我们实现一个事件发送器类 `EventEmitter` ，它实现了 `Runnable` 接口。

```java
public class EventEmitter implements Runnable {
    private static final String TAG = Bus.TAG;

    public final Bus bus; // Bus对象
    public final Object event; // 事件对象
    public final Subscriber subscriber; // 订阅关系
    public final Bus.EventMode mode; // 分发模式

    public EventEmitter(final Bus bus, final Object event,
                        final Subscriber subscriber) {
        this.bus = bus;
        this.event = event;
        this.subscriber = subscriber;
        this.mode = subscriber.mode;
    }

    @Override
    public void run() {
        try {
            subscriber.invoke(event);
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
```

### Subscriber

`Subscriber` 类保存事件类型，事件接收器方法和事件目标之间的关系，定义如下：

```java
class Subscriber {
    public final MethodInfo method;// 保存事件方法的信息，下面会解释
    public final Object target; // 事件目标对象
    public final Class<?> targetType; // 目标类型
    public final Class<?> eventType; // 事件类型
    public final Bus.EventMode mode; // 分发模式
    public final String name; // 名字

    public Subscriber(final MethodInfo method, final Object target) {
        this.method = method;
        this.target = target;
        this.eventType = method.eventType;
        this.targetType = method.targetType;
        this.mode = method.mode;
        this.name = method.name;
    }

    public Object invoke(Object event)
            throws InvocationTargetException, IllegalAccessException {
        return this.method.method.invoke(this.target, event);
    }

}
```

### MethodInfo

`MethodInfo` 保存通过注解或方法名查找到的事件接收器方法的信息，包含 `Method` 对象和参数类型（也就是接受的事件类型），还有注解指定的分发模式，定义如下：

```java
public class MethodInfo {
    public final Method method;
    public final Class<?> targetType;
    public final Class<?> eventType;
    public final String name;
    public final Bus.EventMode mode;

    public MethodInfo(final Method method, final Class<?> targetClass, final Bus.EventMode mode) {
        this.method = method;
        this.targetType = targetClass;
        this.eventType = method.getParameterTypes()[0];
        this.mode = mode;
        this.name = targetType.getName() + "." + method.getName()
                + "(" + eventType.getName() + ")";
    }
}
```

### 分发事件

发送者调用 `post(event)` 方法时， `Bus` 会找到所有可能的接受者，然后构造  `EventEmitter` 对象，调用 `sendEvent(emitter)` 方法根据接受者指定的分发模式分发每一个事件，如下：

```java
    public void sendEvent(EventEmitter emitter) {
        if (EventMode.Sender.equals(emitter.mode)) {
            mSenderScheduler.post(emitter);
        } else if (EventMode.Main.equals(emitter.mode)) {
            if (Helper.isMainThread()) {
                mSenderScheduler.post(emitter);
            } else {
                mMainScheduler.post(emitter);
            }
        } else if (EventMode.Thread.equals(emitter.mode)) {
            mThreadScheduler.post(emitter);
        }
    }
```


## 总结

以上就是在发送者线程、主线程、独立线程分发事件的全过程，下一节会介绍事件类型的模糊匹配和缓存优化，还有扩展功能和高级用法。

## 系列文章

* [`跟我一起写EventBus（一）`](how-to-write-an-eventbus-part1.md)
* [`跟我一起写EventBus（二）`](how-to-write-an-eventbus-part2.md) 
