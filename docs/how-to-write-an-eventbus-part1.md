# 跟我一起写EventBus（一）

<!-- TOC -->

- [什么是`EventBus`](#什么是eventbus)
- [接口定义](#接口定义)
- [接口分析](#接口分析)
- [开始实现](#开始实现)
    - [查找使用 `@BusReceiver` 的方法](#查找使用-busreceiver-的方法)
    - [`register(target)` 的实现](#registertarget-的实现)
    - [`unregister(target)` 的实现](#unregistertarget-的实现)
    - [`post(event)` 的实现](#postevent-的实现)
- [测试一下](#测试一下)
- [完整代码](#完整代码)

<!-- /TOC -->

## 什么是`EventBus`

先介绍一下概念， `EventBus` 直译过来就是`事件总线`，它使用发布订阅模式支持组件之间的通信，不需要显式地注册回调，比观察者模式更灵活，可用于替换Java中传统的事件监听模式，`EventBus`的作用就是解耦，它不是通用的发布订阅系统，也不能用于进程间通信。可用于Android的`EventBus`库主要有这几个：Google出品的`Guava`，`Guava`是一个庞大的库，`EventBus` 只是它附带的一个小功能，因此实际项目中使用并不多。用的最多的是[`greenrobot/EventBus`](https://github.com/greenrobot/EventBus)，这个库的优点是接口简洁，集成方便，但是限定了方法名，不支持注解。另一个库[`square/otto`](https://github.com/square/otto)修改自 `Guava` ，用的人也不少。

以 `greenrobot/EventBus` 为例，我们看一下 `EventBus` 模式的典型用法：

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

我们的目标是从零开始自己写一个 `EventBus` ，参考上面的示例，首先定义接口，假设接口叫 `IBus` ，实现类叫 `Bus` ，它最少需要三个方法。为了灵活和简洁，暂时不考虑性能，我们采用 `Annotation` 的方式指定事件接收者。

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

## 接口分析

上面定义的 `IBus` 接口有三个方法 `register(target)` ， `unregister(target)` ， `post(event)` ，事件接收器使用`Annotation`的方式指定，下面逐一考察这些问题：

1. 按照接口定义， `register(target)` 方法用于注册事件接收器的目标对象，我们需要保存这个对象，同时还需要查找这个对象中存在的事件接收器方法，上面说了，这些事件接受器是使用了 `@BusReceiver` 注解的 `public` 方法，查找出这些方法之后需要保存这些方法，后续 `post(event)` 发送事件的时候需要用到。
2. `unregister(target)` 方法用于取消注册目标对象，调用这个方法之后，不能再给这个对象发送任何事件，也就是要从保存的事件目标对象集合里移除这个对象和对应的事件接收器方法集合，这要求我们能找到这个对象以及这个对象中的事件接受器对象集合。
3.  `post(event)` 方法用于发送事件给目标对象，也就是调用注册了这个事件接收器的方法，这要求我们可以通过事件对象或时间对象类型找到对应的目标对象( `target` )和事件接收器( '@BusReceiver' )方法。
4. 事件接收器通过 `@BusReceiver` 这个注解指定，因此我们需要在 `register(target)` 的时候目标对象中所有使用了这个注解的方法，并排除非 `public` 的方法， `static` 方法也需要排除，没有参数或参数超过一个的也要排除。

## 开始实现

### 查找使用 `@BusReceiver` 的方法

首先我们要定义 `BusReceiver` 注解，它的定义很简单，如下：

```java
@Target(ElementType.METHOD) // 表示这个注解适用于方法
@Retention(RetentionPolicy.RUNTIME) //表示这个注解需要保留到运行时
public @interface BusReceiver {
}
```

我们定义一个通用一点的方法叫 `findAnnotatedMethods(class,annotation)` ，用于查找给定的`class`里面使用了 `Annotation` 的方法，为了简化问题，我们先忽略从父类继承的方法，只查找指定类的方法，这个方法是个通用的工具方法，可以放在一个单独的类中，定义为静态方法，假设这个类为`Helper`，如下：

```java

    public static List<Method> findAnnotatedMethods(final Class<?> type,
                                                    final Class<? extends Annotation> annotation) {
        final List<Method> methods = new ArrayList<Method>();
//        Class<?> clazz = type;
        // for now ignore super class, handle current class only
        Method[] ms = type.getDeclaredMethods();
        for (Method method : ms) {
            methods.add(method);
        }
        return methods;
    }
```

这是最简单的查找使用了某个特定注解的方法，按照我们接口的定义，事件接收器方法需要满足这几个条件：

* 使用了 `@BusReceiver` 注解
* 是 `public` 方法，并且不是 `static` 方法
* 参数只能是一个，不能没有也不能是多个

按照这个条件加上过滤， `findAnnotatedMethods` 方法最终是这样的：

```java
    public static List<Method> findAnnotatedMethods(final Class<?> type,
                                                    final Class<? extends Annotation> annotation) {
        final List<Method> methods = new ArrayList<Method>();
//        Class<?> clazz = type;
        // for now ignore super class, handle current class only
        Method[] ms = type.getDeclaredMethods();
        for (Method method : ms) {
            // must not static
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            // must be public
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            // must has only one parameter
            if (method.getParameterTypes().length != 1) {
                continue;
            }
            // must has annotation
            if (!method.isAnnotationPresent(annotation)) {
                continue;
            }
            methods.add(method);
        }
        return methods;
    }
```

因为判断 `Method` 的修饰符和参数列表长度比较快， 我们放在前面， `method.isAnnotationPresent` 这个方法比较慢，放在最后。

### `register(target)` 的实现

有了 `findAnnotatedMethods` 方法， `register(target)` 的实现就简单多了，这个方法需要找出所有符合条件的事件接收器方法，然后保存起来，由于后面我们要根据对象查找方法，我们将 `target:method` 关系保存到一个`Map`中，假设我们的`IBus`接口的实现类叫`Bus`，在`Bus`类中增加一个成员变量保存它们：

```
	private Map<Object, List<Method>> mMethodMap = new HashMap<Object, List<Method>>();

    public void register(final Object target) {
        List<Method> methods = Helper.findAnnotatedMethods(target.getClass(), BusReceiver.class);
        if (methods == null || methods.isEmpty()) {
            return;
        }
        mMethodMap.put(target, methods);
    }
```

### `unregister(target)` 的实现

有了上面保存的 `mMethodMap` 数据，取消注册目标就是移除目标对象注册过的所有事件接收器方法，可以这样写：

```java
    public void unregister(final Object target) {
        mMethodMap.remove(target);
    }
```

### `post(event)` 的实现

`xBus`的使用者在完成某项任务之后调用 `post(event)` 方法发送事件，这个方法会遍历所有注册过的`target`里包含的，接受这个事件的事件接收器方法，因此需要知道怎么从事件对象找到事件接收器，由于可能有多个`target`都注册了这个事件的接收器，因此我们需要遍历所有的目标对象，找到符合条件的方法，然后调用这些方法。怎么样才算符合条件呢，首先要符合 `findAnnotatedMethods` 那里提到的条件：使用 `@BusReceiver` 注解，是 `publici` 且非 `static` 方法，有且只有一个参数，针对某个事件 `event` ，还需要参数类型是一致的，举例说明：

```java
	// 对类型为`SomeEvent`的事件`event`，只能发送给如下形式的事件接收器：
	// 这里方法名字没有要求，但是注解和方法参数签名不能变
	@BusReceier
	public void anyMethodName(SomeEvent event){
	// handle event
	}
```

能想到的最直接的查找思路就是遍历 `mMethods` 列表，逐个对比，找出符合要求的方法，然后调用对应的方法：

```java
    public void post(Object event) {
        final Class<?> eventClass = event.getClass();
        for (Map.Entry<Object, List<Method>> entry : mMethodMap.entrySet()) {
            final Object target = entry.getKey();
            final List<Method> methods = entry.getValue();
            if (methods == null || methods.isEmpty()) {
                continue;
            }
            for (Method method : methods) {
            // 如果事件类型相符，就调用对应的方法发送事件
            // 这里的类型是要求精确匹配的，没有考虑继承
                if (eventClass.equals(method.getParameterTypes()[0])) {
                    try {
                        method.invoke(target, event);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    }
```

## 测试一下

```java
public class BusDemo {

    public static void main(String[] args) {
        final BusDemo demo = new BusDemo();
        Bus bus = Bus.getDefault();
        bus.register(demo);
        bus.post(new Object());
        bus.post("SomeEvent");
        bus.post(12345);
        bus.post(new RuntimeException("Error"));
    }

	// 没有发送这个事件
    @BusReceiver
    public void onReceiveRunnableNotPost(Runnable event) {
        System.out.println("onReceiveRunnableNotPost() event=" + event);
    }

    @BusReceiver
    public void onObjectEvent(Object event) {
        System.out.println("onObjectReceive() event=" + event);
    }

	// 发送的事件是`RuntimeException`，不是精确匹配`Exception`
    @BusReceiver
    public void onExceptionEvent(Exception event) {
        System.out.println("onExceptionEvent() event=" + event);
    }

    @BusReceiver
    public void onStringReceive(String event) {
        System.out.println("onStringReceive() event=" + event);
    }

    @BusReceiver
    public void onInteger(Integer event) {
        System.out.println("onInteger() event=" + event);
    }
}
```


## 完整代码

* 完整的代码见 [tutorial-part1/src/main](https://github.com/mcxiaoke/xBus/tree/tutorial-part1/src/main/com/mcxiaoke/bus)
* 详细的示例见 [tutorial-part1/src/demo](https://github.com/mcxiaoke/xBus/tree/tutorial-part1/src/demo/com/mcxiaoke/bus/demo)
* 代码打包下载 [archive/tutorial-part1.zip](https://github.com/mcxiaoke/xBus/archive/tutorial-part1.zip)

## 进一步的问题

这个粗糙的版本只是实现了一个最基本的`EventBus`的功能，如果想把它用在实际的项目中，还需要考虑很多问题，比如：

1.  `findAnnotatedMethods` 方法没有考虑效率问题，如果某个`target`中有成千上万个方法，这个方法可能比较慢，是需要考虑缓存或其它的优化方法。
2. `post(event)`方法需要遍历保存所有目标对象的所有方法，这个在方法数量很大时效率同样存在问题，可以改进一下遍历过程，或者可以加缓存，不用每次都遍历。
3. `Bus`类直接保存了目标对象`target`的强引用，如果使用者忘记调用 `unregister(target)` 方法取消注册，可能造成内存泄露，任何改进。
4. `Bus`的实现没有考虑在多个线程中使用的问题，没有添加任何同步代码，可能会造成内部数据的不同步，或者发生错误。
5. `Bus`的实现不支持外部配置，限定了事件接收器方法只能使用 `@BusReceiver` ，且只能是`public`的，只能带一个参数，能不能支持使用者自定义这些行为。
6. `Bus`目前的版本不支持继承，既不支持在基类中注册，也不支持事件接收器方法中参数类型的继承，如何支持。
7. 当前的事件接收器不支持泛型参数，不支持集合类型，如果支持，还可以考虑，如何支持多个事件参数和可变参数类型的事件。
8. 当前的`post(event)`方法是在调用者线程执行，在很多情况下，使用者可能需要任务执行的线程和事件接收器的线程是分开的，比如在某个后台线程中执行异步任务，在主线程中接收事件更新界面，这个需要支持用户自定义。
9. 目前的`Bus`实现没有任何异常处理的代码，一个健壮的程序不能缺少完善的异常处理。


这些问题都是一个完整的 `EventBus` 实现需要考虑的问题，教程的后续部分将逐步实现这些功能，解决存在的问题。

## 相关阅读

#### 什么是EventBus
<https://code.google.com/p/guava-libraries/wiki/EventBusExplained>
<http://doc.akka.io/docs/akka/snapshot/java/event-bus.html>

#### EventBus的实现
<http://javarticles.com/2015/04/guava-eventbus-examples.html>
<http://timnew.me/blog/2014/12/06/typical-eventbus-design-patterns/>
<https://code.google.com/p/simpleeventbus/>
<https://github.com/greenrobot/EventBus/blob/master/HOWTO.md>

#### EventBus的使用
<http://www.cnblogs.com/peida/p/eventbus.html>
<http://blog.cainwong.com/using-an-eventbus-in-android-pt-1-why-an-eventbus/>
<http://blog.cainwong.com/using-an-eventbus-in-android-pt-2-sticking-your-config/>
<http://blog.cainwong.com/using-an-eventbus-in-android-pt-3-threading/>
