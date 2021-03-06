package com.alibaba.android.arouter.core;

import com.alibaba.android.arouter.base.UniqueKeyTreeMap;
import com.alibaba.android.arouter.facade.enums.RouteType;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.alibaba.android.arouter.facade.template.IInterceptor;
import com.alibaba.android.arouter.facade.template.IProvider;
import com.alibaba.android.arouter.facade.template.IRouteGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 静态化存储数据
 * Storage of route meta and other data.
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/23 下午1:39
 */
class Warehouse {

    // LogisticsCenter.init() 中被赋值
    // Cache route and metas
    // 例：
    // routes.put("service", ARouter$$Group$$service.class);
    // routes.put("test", ARouter$$Group$$test.class);
    static Map<String, Class<? extends IRouteGroup>> groupsIndex = new HashMap<>();
    // LogisticsCenter.completion(postcard) 中被赋值
    // atlas.put("/app/ImageActivity", RouteMeta.build(RouteType.ACTIVITY, ImageActivity.class, "/app/imageactivity", "app", null, -1, -2147483648));
    // atlas.put("/app/MainActivity", RouteMeta.build(RouteType.ACTIVITY, MainActivity.class, "/app/mainactivity", "app", null, -1, -2147483648));
    // atlas.put("/app/TextActivity", RouteMeta.build(RouteType.ACTIVITY, TextActivity.class, "/app/textactivity", "app", null, -1, -2147483648));
    static Map<String, RouteMeta> routes = new HashMap<>();

    // 存储 IProvider
    // Cache provider
    // LogisticsCenter.completion(postcard) 中被赋值
    static Map<Class, IProvider> providers = new HashMap<>();
    // LogisticsCenter.init() 中被赋值
    //
    // providers.put("com.alibaba.android.arouter.facade.service.PathReplaceService", RouteMeta.build(RouteType.PROVIDER, PathReplaceServiceImpl.class, "/app/pathreplace", "app", null, -1, -2147483648));
    // providers.put("com.alibaba.android.arouter.demo.testservice.HelloService", RouteMeta.build(RouteType.PROVIDER, HelloServiceImpl.class, "/service/hello", "service", null, -1, -2147483648));
    // providers.put("com.alibaba.android.arouter.facade.service.SerializationService", RouteMeta.build(RouteType.PROVIDER, JsonServiceImpl.class, "/service/json", "service", null, -1, -2147483648));
    // providers.put("com.alibaba.android.arouter.demo.testservice.SingleService", RouteMeta.build(RouteType.PROVIDER, SingleService.class, "/service/single", "service", null, -1, -2147483648));
    static Map<String, RouteMeta> providersIndex = new HashMap<>();

    /*
    *  Cache interceptor
    *  LogisticsCenter.init() 中被赋值;
    *  此处拦截器的存储使用TreeMap，存储的时候，已经对拦截器的优先级进行排序
    * */
    // interceptors.put(7, Test1Interceptor.class);
    static Map<Integer, Class<? extends IInterceptor>> interceptorsIndex = new UniqueKeyTreeMap<>("More than one interceptors use same priority [%s]");
    // InterceptorServiceImpl.init()中被赋值
    static List<IInterceptor> interceptors = new ArrayList<>();

    static void clear() {
        routes.clear();
        groupsIndex.clear();
        providers.clear();
        providersIndex.clear();
        interceptors.clear();
        interceptorsIndex.clear();
    }
}
