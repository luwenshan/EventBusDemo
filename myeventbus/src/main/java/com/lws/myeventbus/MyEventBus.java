package com.lws.myeventbus;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MyEventBus {

    // 事件对象的订阅者列表集合，key -> 事件.class
    private Map<Class<?>, List<SubscriberMethod>> subscriberMethodMap;
    // 订阅者的事件列表集合
    private Map<Object, List<Class<?>>> subscriberEventMap;
    // 粘性事件集合，key -> 事件.class
    private Map<Class<?>, Object> stickyEventMap;
    private MainHandler mainHandler;

    static volatile MyEventBus instance;

    private MyEventBus() {
        subscriberMethodMap = new HashMap<>();
        subscriberEventMap = new HashMap<>();
        stickyEventMap = new HashMap<>();
        mainHandler = new MainHandler();
    }

    public static MyEventBus getDefault() {
        if (instance == null) {
            synchronized (MyEventBus.class) {
                if (instance == null) {
                    instance = new MyEventBus();
                }
            }
        }
        return instance;
    }

    public void register(Object subscriber) {
        List<SubscriberMethod> subscriberMethodList = getSubscriberMethod(subscriber);
        if (subscriberMethodList == null || subscriberMethodList.size() == 0) {
            return;
        }
        for (SubscriberMethod subscriberMethod : subscriberMethodList) {
            subscribe(subscriber, subscriberMethod);
        }
    }

    public void unregister(Object subscriber) {
        List<Class<?>> eventList = subscriberEventMap.get(subscriber);
        if (eventList == null || eventList.size() == 0) {
            return;
        }
        for (Class<?> event : eventList) {
            List<SubscriberMethod> methodList = subscriberMethodMap.get(event);
            Iterator<SubscriberMethod> iterator = methodList.iterator();
            while (iterator.hasNext()) {
                SubscriberMethod subscriberMethod = iterator.next();
                if (subscriberMethod.subscriber == subscriber) {
                    iterator.remove();
                }
            }
        }
        subscriberEventMap.remove(subscriber);
    }

    public void post(Object event) {
        List<SubscriberMethod> subscriberMethodList = subscriberMethodMap.get(event.getClass());
        if (subscriberMethodList == null || subscriberMethodList.size() == 0) {
            return;
        }
        for (SubscriberMethod subscriberMethod : subscriberMethodList) {
            postEvent(subscriberMethod, event);
        }
    }

    public void postSticky(Object event) {
        stickyEventMap.put(event.getClass(), event);
        post(event);
    }

    public boolean isRegistered(Object subscriber) {
        return false;
    }

    private void subscribe(Object subscriber, SubscriberMethod subscriberMethod) {
        Class<?> eventType = subscriberMethod.eventType;
        List<SubscriberMethod> subscriberMethods = subscriberMethodMap.get(eventType);
        if (subscriberMethods == null) {
            subscriberMethods = new ArrayList<>();
            subscriberMethodMap.put(eventType, subscriberMethods);
        }
        subscriberMethods.add(subscriberMethod);
        List<Class<?>> subscriberEvents = subscriberEventMap.get(subscriber);
        if (subscriberEvents == null) {
            subscriberEvents = new ArrayList<>();
            subscriberEventMap.put(subscriber, subscriberEvents);
        }
        subscriberEvents.add(eventType);
        if (subscriberMethod.sticky) {
            Object eventSticky = stickyEventMap.get(eventType);
            if (eventSticky != null) {
                postEvent(subscriberMethod, eventSticky);
            }
        }
    }

    private void postEvent(SubscriberMethod subscriberMethod, Object event) {
        Object subscriber = subscriberMethod.subscriber;
        ThreadMode threadMode = subscriberMethod.threadMode;
        Method method = subscriberMethod.method;
        switch (threadMode) {
            case POSTING:
                invokeMethod(subscriber, method, event);
                break;
            case MAIN:
                Object[] objects = new Object[]{subscriber, method, event};
                mainHandler.sendMessage(mainHandler.obtainMessage(1, objects));
                break;
        }
    }

    private void invokeMethod(Object subscriber, Method method, Object event) {
        try {
            method.invoke(subscriber, event);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private List<SubscriberMethod> getSubscriberMethod(Object subscriber) {
        List<SubscriberMethod> list = new ArrayList<>();
        Class<?> subscriberClass = subscriber.getClass();
        Method[] methods = subscriberClass.getDeclaredMethods();
        for (Method method : methods) {
            Subscribe subscribe = method.getAnnotation(Subscribe.class);
            if (subscribe == null) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                throw new RuntimeException("MyEventBus 订阅方法只接受一个参数");
            }
            SubscriberMethod subscriberMethod = new SubscriberMethod(subscriber, method, parameterTypes[0], subscribe.threadMode(), subscribe.sticky());
            list.add(subscriberMethod);
        }
        return list;
    }

    class MainHandler extends Handler {
        public MainHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Object[] obj = (Object[]) msg.obj;
            Object subsriber = obj[0];
            Method method = (Method) obj[1];
            Object event = obj[2];
            invokeMethod(subsriber, method, event);
        }
    }

    class SubscriberMethod {
        Object subscriber;
        Method method;
        ThreadMode threadMode;
        Class<?> eventType;
        boolean sticky;

        public SubscriberMethod(Object subscriber, Method method, Class<?> eventType, ThreadMode threadMode, boolean sticky) {
            this.subscriber = subscriber;
            this.method = method;
            this.threadMode = threadMode;
            this.eventType = eventType;
            this.sticky = sticky;
        }
    }
}
