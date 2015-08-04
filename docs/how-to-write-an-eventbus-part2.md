# 跟我一起写EventBus（二）

在 [跟我一起写EventBus（一）](how-to-write-an-eventbus-part1.md) 里我们实现了一个非常粗糙的`EventBus`，在这一节里面我们要给这个`EventBus`添加以下两个功能：

* 支持在基类中调用 `register(target)` 注册，调用 `unregister(target)` 取消注册
* 发送事件时， `post(event)` 支持匹配基类的事件接收器

## 基类注册

支持 `register` 和 `unregister` 的继承，用法如下：

```java
// 基类
public class BaseClass {
    void onCreate() {
        Bus.getDefault().register(this);
    }
    void onDestroy() {
        Bus.getDefault().unregister(this);
    }
}
// 子类
public class SubClass extends BaseClass {
    @Override
    void onCreate() {
        super.onCreate();
    }
    @Override
    void onDestroy() {
        super.onDestroy();
    }
}
```

## 事件类型

`post(event)` 中的 `event` 可以是事件接收器的参数 `eventType` 的实现类或子类，用法如下：

```java
/**
 * 运行这个Demo，控制台的输出是：
 * onCharSequenceEvent() event=A StringBuilder
 * onObjectEvent() event=A StringBuilder
 */
public class EventDemo {
    public static void main(String[] args) {
        new EventDemo().run();
    }
    public void run() {
        Bus.getDefault().register(this);
        Bus.getDefault().post(new StringBuilder("A StringBuilder"));
        Bus.getDefault().unregister(this);
    }
    @BusReceiver
    public void onStringEvent(String event) {
        // 不会执行，因为event是StringBuilder，event instanceof String == false
        System.out.println("onStringEvent() event=" + event);
    }
    @BusReceiver
    public void onExceptionEvent(Exception event) {
        // 不会执行，因为event是StringBuilder，event instanceof Exception == false
        System.out.println("onExceptionEvent() event=" + event);
    }
    @BusReceiver
    public void onCharSequenceEvent(CharSequence event) {
        // 会执行，因为event是StringBuilder，event instanceof CharSequence == true
        System.out.println("onCharSequenceEvent() event=" + event);
    }
    @BusReceiver
    public void onObjectEvent(Object event) {
        // 会执行，因为event是StringBuilder，event instanceof Object == true
        System.out.println("onObjectEvent() event=" + event);
    }
}
```

## 功能实现

下面我们来看看如何实现这两个功能

### 支持在基类中注册

首先，注册和取消注册这两个方法默认支持在基类中使用，因为实际运行的程序中并不存在基类的实例，即使你在基类中调用 `register(target)` ，这个 `target` 也是实际运行的子类对象，所以基类中注册指示集中了代码，并没有改变运行时的行为。

但是，之前版本的 `EventBus` 在查找事件接收器方法时只查找了当前类中的方法，忽略了父类和继承链上的方法，因此需要修改先前版本的 `findAnnotatedMethods` 方法，最简单的思路就是一个 `while` 循环，递归查找当前类和父类的方法，然后过滤出符合条件的事件接收器方法，我们把检查方法是否符合条件的代码先提到一个单独的方法，叫 `isAnnotatedMethod` ，然后 `findAnnotatedMethods` 方法实现如下：

```java
    public static Set<Method> findAnnotatedMethods(final Class<?> type,
                                                   final Class<? extends Annotation> annotation) {
        Class<?> clazz = type;
        final Set<Method> methods = new HashSet<Method>();
        // 逐级查找父类的方法，遇到Object类时停止
        while (clazz!=Object.class) {
            final Method[] allMethods = clazz.getDeclaredMethods();
            for (final Method method : allMethods) {
                if (isAnnotatedMethod(method, annotation)) {
                    methods.add(method);
                }
            }
            // search more methods in super class
            clazz = clazz.getSuperclass();
        }
        return methods;
    }
```

可以看到，和之前版本的差别不大，逻辑几乎一样，就是增加了递归父类查找。这里可以做一点优化，实际查找父类时不需要查找JDK自带的类，针对Android系统，也不需要查找系统框架中的类，因此增加了一个优化的判断方法：

```java
    private static boolean shouldSkipClass(final Class<?> clazz) {
        final String clsName = clazz.getName();
        return Object.class.equals(clazz)
                || clsName.startsWith("java.")
                || clsName.startsWith("javax.")
                || clsName.startsWith("android.")
                || clsName.startsWith("com.android.");
    }
```

上面的 `while (clazz!=Object.class)` 改成 `!shouldSkipClass(clazz)` ，其它部分不变，这样修改之后，可以大幅提高父类查找的效率，减少很多不必要的遍历。

下面是提取出来的 `isAnnotatedMethod` 方法，用于判断某个方法是否满足事件接收器的要求：

```java
    private static boolean isAnnotatedMethod(final Method method,
                                             final Class<? extends Annotation> annotation) {
        // 是否使用了@BusReceiver的注解，这个放在最上面，一点效率优化
        if (!method.isAnnotationPresent(annotation)) {
            return false;
        }
        // 过滤掉非public的方法
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        // 过滤掉`static`的方法
        if (Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        // 过滤掉volatile的方法，这里是修复Java编译器自动添加bridge方法造成的方法重复的问题，具体细节比较复杂，这里不提了
        // 这里实际过滤掉的是BRIDGE修饰符修饰的方法，观察Modifier类中的常量定义可以发现，BRIDGE和VOLATILE的值都是0x00000040，但是不存在一个叫Modifier.isBridge的方法，所以用Modifier.isVolatile替代，效果相同
        // Java代码中不允许使用volatile修饰方法，但是编译器不受此限制。还需要提一下的是，Method类中的equals方法没有使用修饰符比较。
        // fix getDeclaredMethods bug, if method in base class,
        // it returns duplicate method,
        // one is normal, the other is the same but with volatile modifier
        if (Modifier.isVolatile(method.getModifiers())) {
            return false;
        }
        // must has only one parameter
        if (method.getParameterTypes().length != 1) {
            return false;
        }

        return true;
    }
```

### 事件类型的匹配

在第一节中 `EventBus` 的实现只支持事件类型对象的精确匹配，那个版本的 `post(event)` 方法中对事件类型的判断是：

```java
if (eventClass.equals(method.getParameterTypes()[0]))
```

我们修改为：

```java
Class<?> parameterClass = method.getParameterTypes()[0];
if (parameterClass.isAssignableFrom(eventClass)) {
// post event to receiver
}
```

这里解释一下，对于 `objectA.isAssignableFrom(classB)` ，当 `objectA` 是 `classB` 的实现类（如果 `classB` 是一个接口）或其子类时返回 `true` ，这里我们判断如果事件类是事件接收器的参数类的实现类或子类就可以向它发送事件。

## 一点优化

第一版 `EventBus` 我们使用 `List<Method>` 保存某个对象的方法列表，实际上我们要求这个列表不能有重复的方法存在，所以可以改用`Set`实现，几处修改如下：

```java
// findAnnotatedMethods 方法返回一个 Set<Method>
final Set<Method> methods = new HashSet<Method>();
        while (!shouldSkipClass(clazz)) {...}
        
// Bus类中的mMethodMap类型修改为 Map<Object, Set<Method>>
private Map<Object, Set<Method>> mMethodMap = new WeakHashMap<Object, Set<Method>>();
```

## 完整代码

* 完整的代码见 [tutorial-part2-code](https://github.com/mcxiaoke/xBus/tree/tutorial-part2/src/main/com/mcxiaoke/bus)
* 代码打包下载 [tutorial-part2-zip](https://github.com/mcxiaoke/xBus/archive/tutorial-part2.zip)

## 进一步的问题

这个版本增加了对继承的支持，放宽了事件接收器参数匹配的规则，还完善了一些细节，做了一些简单的优化，离实用更进一步了，后面还有不少问题需要解决，如下：

1. `post(event)` 方法需要遍历保存所有目标对象的所有方法，这个在方法数量很大时效率同样存在问题，可以改进一下遍历过程，或者可以加缓存，不用每次都遍历。
2. `Bus` 类直接保存了目标对象 `target` 的强引用，如果使用者忘记调用 `unregister(target)` 方法取消注册，可能造成内存泄露，任何改进。
3. `Bus` 的实现没有考虑在多个线程中使用的问题，没有添加任何同步代码，可能会造成内部数据的不同步，或者发生错误。
4. `Bus` 的实现不支持外部配置，限定了事件接收器方法只能使用 `@BusReceiver` ，且只能是`public`的，只能带一个参数，能不能支持使用者自定义这些行为。
5. 当前的事件接收器不支持泛型参数，不支持集合类型，如果支持，还可以考虑，如何支持多个事件参数和可变参数类型的事件。
6. 当前的 `post(event)` 方法是在调用者线程执行，在很多情况下，使用者可能需要任务执行的线程和事件接收器的线程是分开的，比如在某个后台线程中执行异步任务，在主线程中接收事件更新界面，这个需要支持用户自定义。
7. 目前的`Bus`实现没有任何异常处理的代码，一个健壮的程序不能缺少完善的异常处理。
