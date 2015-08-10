# 跟我一起写EventBus（四）

## 系列文章

* [`跟我一起写EventBus（一）`](how-to-write-an-eventbus-part1.md)
* [`跟我一起写EventBus（二）`](how-to-write-an-eventbus-part2.md)
* [`跟我一起写EventBus（三）`](how-to-write-an-eventbus-part2.md)


## 概述

前面三部分我们已经实现了一个完整的 `EventBus` ，这一部分主要是优化和增强，包括事件类型模糊匹配和缓存优化，还有扩展功能和高级用法。

## 方法缓存

Java中的反射虽然速度已经很快，但相对于正常的方法调用来说还是慢很多，使用注解也有不小的性能成本，但是通过使用缓存，一次查找多次使用，可以最大程度较少这个成本，实际使用中几乎可以忽略不计。

对于 `xBus` 来说，主要两种东西需要缓存，一是某个对象包含的事件接收器方法列表，一个是事件类型对应的泛类型列表。前者是在注册时需要查找，后者是在发送事件时需要查找。下面分开说明。

### 方法表缓存

#### 数据结构

实现原理其实非常简单，就是用一个Map保存对象和方法列表，但不是保存原始的 `Method` 对象，而是保存经过处理的 `MethodInfo` 对象，第三部分里提到过，这个类保存了一些解析出来的额外信息：比如事件类型和分发模式等，它的结构如下：

```java
public class MethodInfo {
    public final Method method;
    public final Class<?> targetType;
    public final Class<?> eventType;
    public final String name;
    public final Bus.EventMode mode;
}
```

缓存方法的Map定义如下：

```java
final static Map<String, Set<MethodInfo>> sMethodCache =
                new ConcurrentHashMap<String, Set<MethodInfo>>();
```

使用static是为了跨多个`Bus`实例共享，鉴于大多数应用都是使用默认的实例，改成普通的非static变量也可以。

#### 缓存流程

调用 `register(target)` 时会检查方法表缓存，如果不存在，就通过 `MethodFinder` 查找对象的事件接收器方法，然后添加到缓存；如果存在缓存，直接忽略这一步。代码如下：

```java
    public <T> void register(final T target) {
    // 真正的实现代码在 addSubscribers() 里
        addSubscribers(target);
    }
    
    private void addSubscribers(final Object target) {
        // 这里找出target里包含的所有事件接收器方法
        Set<MethodInfo> methods = getMethods(targetType);
        for (MethodInfo method : methods) {
            addSubscriber(subscriber);
        }
    }
    
    // 查找方法和缓存的实现在这里
    private Set<MethodInfo> getMethods(Class<?> targetClass) {
        String cacheKey = targetClass.getName();
        Set<MethodInfo> methods;
        synchronized (Cache.sMethodCache) {
            methods = Cache.sMethodCache.get(cacheKey);
        }
        if (methods == null) {
        // 如果不存在缓存， 就查找一次
            methods = mMethodFinder.find(this, targetClass);
            synchronized (Cache.sMethodCache) {
            // 然后添加到方法表缓存
                Cache.sMethodCache.put(cacheKey, methods);
            }
        }
        return methods;
    }
    
```

简单的测试显示，包含一千个方法的类，一次查找最多可能需要差不多20毫秒，Android应用大多数Activity和Fragment类都不会有这么多方法，大部分情况消耗的时间都在5毫秒以下，详细的性能测试后续会补充。有了缓存之后，后续再进入同一个界面，不需要重复查找，消耗的时间就是零了。

### 事件类型缓存

事件类型缓存主要用于事件的泛匹配。在 `post(event)` 方法中需要用到。

```java
    public <E> void post(E event) {
        final Class<?> theEventType = event.getClass();
        final String cacheKey = theEventType.getName();
        Set<Class<?>> eventTypes;
        // 检查缓存中是否存在
        synchronized (Cache.sEventTypeCache) {
            eventTypes = Cache.sEventTypeCache.get(cacheKey);
        }
        if (eventTypes == null) {
        // 如果没有缓存， 递归查找父类和接口，然后保存到缓存中
            eventTypes = Helper.findSuperTypes(theEventType);
            synchronized (Cache.sEventTypeCache) {
                Cache.sEventTypeCache.put(cacheKey, eventTypes);
            }
        }
        for (Class<?> eventType : eventTypes) {
            postEventByType(event, eventType);
        }
    }
```



###  事件类型模糊匹配

这个过程可以简单的理解为是 `event instanceof ClassType` 的过程，如果事件能强制转换为 `ClassType` 类型，就可以收到事件。举例，假设 `post(event)` 中 `event` 的类型是 ClassA，四个接收器方法的参数类型分别是Object，IClass, BaseClass，ClassA，ClassB，ClassC，那么前面四个方法都可以接收到事件，最后两个类型不匹配，收不到事件，代码示例如下。

```java
interface IClass{}

class BaseClass implements IClass{}

class ClassA extends BaseClass{}

class ClassB extends ClassA{}

class ClassC extends ClassB{}

public class EventMatchDemo {
    
    public void postTestEvent(){
    // 假设发送的事件类型是 ClassA 
    	ClassA event=new ClassA();
        Bus.getDefault().post(event);
    }
    
    @BusReceiver
    public void onEvent0(Object event){
    	// 所有的类都是Object的子类，能收到
       // (event instanceof Object)==True  
    }
    
    @BusReceiver
    public void onEvent0(IClass event){
    	// ClassA是IClass的实现类，能收到
       // (event instanceof IClass)==True  
    }

    @BusReceiver
    public void onEvent1(BaseClass event){
    	// ClassA是BaseClass的子类，能收到
        // (event instanceof BaseClass)==True  
    }

    @BusReceiver
    public void onEventA(ClassA event){
    	// ClassA类型相同，能收到
        //  (event instanceof ClassA)==True  
    }

    @BusReceiver
    public void onEventB(ClassB event){
        // 收不到，ClassA 不能强制类型转换为ClassB
        // (event instanceof Object)==False  
    }

    @BusReceiver
    public void onEventC(ClassC event){
        // 收不到，ClassA 不能强制类型转换为ClassC
        // (event instanceof Object)==False  
    }

}
```

简单的说，发送一个事件 `post(event)` ，如果 `event` 对象能转换为 `onEvent(SomeType e)` 中参数 `SomeType` 的类型，那么这个方法就能收到事件，否则收不到，遵循Java中一般的类型转换原则。

### 扩展接口

#### MethodFinder

首先是方法查找接口，默认支持注解和方法名两种方式，实现 `MethodFinder` ，可以设置自定义的方法查找策略。

`MethodFinder` 接口非常简单，返回给定的对象中符合某些条件的方法列表。

```java
public interface MethodFinder {

    Set<MethodInfo> find(final Bus bus, final Class<?> targetClass);
}
```

#### 其它方法

```java
public Bus setDebug(final boolean debug);
public Bus setMethodFinder(final MethodFinder finder);
public Bus setStrictMode(final boolean strictMode);
```

## 总结

到这里，我们实现了一个完整的 `EventBus` ，它已经可以用在实际项目中。下一部分，也是最后一部分会介绍它的使用方法和一些自定义配置。
