package com.alibaba.android.arouter.core;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.alibaba.android.arouter.exception.HandlerException;
import com.alibaba.android.arouter.exception.NoRouteFoundException;
import com.alibaba.android.arouter.facade.Postcard;
import com.alibaba.android.arouter.facade.enums.RouteType;
import com.alibaba.android.arouter.facade.enums.TypeKind;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.alibaba.android.arouter.facade.template.IInterceptorGroup;
import com.alibaba.android.arouter.facade.template.IProvider;
import com.alibaba.android.arouter.facade.template.IProviderGroup;
import com.alibaba.android.arouter.facade.template.IRouteGroup;
import com.alibaba.android.arouter.facade.template.IRouteRoot;
import com.alibaba.android.arouter.launcher.ARouter;
import com.alibaba.android.arouter.utils.ClassUtils;
import com.alibaba.android.arouter.utils.Consts;
import com.alibaba.android.arouter.utils.LogUtils;
import com.alibaba.android.arouter.utils.MapUtils;
import com.alibaba.android.arouter.utils.PackageUtils;
import com.alibaba.android.arouter.utils.TextUtils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import static com.alibaba.android.arouter.launcher.ARouter.logger;
import static com.alibaba.android.arouter.utils.Consts.AROUTER_SP_CACHE_KEY;
import static com.alibaba.android.arouter.utils.Consts.AROUTER_SP_KEY_MAP;
import static com.alibaba.android.arouter.utils.Consts.DOT;
import static com.alibaba.android.arouter.utils.Consts.ROUTE_ROOT_PAKCAGE;
import static com.alibaba.android.arouter.utils.Consts.SDK_NAME;
import static com.alibaba.android.arouter.utils.Consts.SEPARATOR;
import static com.alibaba.android.arouter.utils.Consts.SUFFIX_INTERCEPTORS;
import static com.alibaba.android.arouter.utils.Consts.SUFFIX_PROVIDERS;
import static com.alibaba.android.arouter.utils.Consts.SUFFIX_ROOT;
import static com.alibaba.android.arouter.utils.Consts.TAG;

/**
 * 基础物流类
 * <p>
 * LogisticsCenter contain all of the map.
 * <p>
 * 1. Create instance when it first used.
 * 2. Handler Multi-Module relationship map(*)
 * 3. Complex logic to solve duplicate group definition
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/23 15:02
 */
public class LogisticsCenter {


    //
    private static Context mContext;
    // 线程池
    static ThreadPoolExecutor executor;

    /**
     * 基础物流类初始化，加载生成的类
     * LogisticsCenter init, load all metas in memory. Demand initialization
     * <p>
     * <p>
     */
    public synchronized static void init(Context context, ThreadPoolExecutor tpe) throws HandlerException {
        LogUtils.e("LogisticsCenter", "init");
        //
        mContext = context;
        // 线程池
        executor = tpe;

        try {
            long startInit = System.currentTimeMillis();
            Set<String> routerMap;
            // 新版本 或者  debug包
            // It will rebuild router map every times when debuggable.
            if (ARouter.debuggable() || PackageUtils.isNewVersion(context)) {
                logger.info(TAG, "Run with debug mode or new install, rebuild router map.");
                // These class was generate by arouter-compiler.
                // 获取com.alibaba.android.arouter.routes 路径下的生成文件
                routerMap = ClassUtils.getFileNameByPackageName(mContext, ROUTE_ROOT_PAKCAGE);

                LogUtils.e(TAG, "routerMap: " + routerMap);

                // 数据存储
                if (!routerMap.isEmpty()) {
                    context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE).edit().putStringSet(AROUTER_SP_KEY_MAP, routerMap).apply();
                }
                // 存储新的版本号
                PackageUtils.updateVersion(context);    // Save new version name when router map update finish.
            } else {
                // 从sp中获取数据
                logger.info(TAG, "Load router map from cache.");
                routerMap = new HashSet<>(context.getSharedPreferences(AROUTER_SP_CACHE_KEY, Context.MODE_PRIVATE).getStringSet(AROUTER_SP_KEY_MAP, new HashSet<String>()));
            }

            logger.info(TAG, "Find router map finished, map size = " + routerMap.size() + ", cost " + (System.currentTimeMillis() - startInit) + " ms.");
            startInit = System.currentTimeMillis();
            // 循环com.alibaba.android.arouter.routes 路径下的所有文件
            for (String className : routerMap) {
                // com.alibaba.android.arouter.routes.ARouter$$Root
                if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_ROOT)) {
                    // 反射
                    // 创建 new ARouter$$Root$$app().loadInto();
                    //
                    // 将数据加载到Warehouse.groupsIndex中
                    // routes.put("service", ARouter$$Group$$service.class);
                    // routes.put("test", ARouter$$Group$$test.class);
                    //
                    // This one of root elements, load root.
                    ((IRouteRoot) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.groupsIndex);
                }
                // com.alibaba.android.arouter.routes.ARouter$$Interceptors
                else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_INTERCEPTORS)) {
                    // 反射
                    // 创建 new ARouter$$Interceptors$$app().loadInto(Warehouse.interceptorsIndex)
                    // interceptors.put(7, Test1Interceptor.class);
                    ((IInterceptorGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.interceptorsIndex);
                }
                // com.alibaba.android.arouter.routes.ARouter$$Providers
                else if (className.startsWith(ROUTE_ROOT_PAKCAGE + DOT + SDK_NAME + SEPARATOR + SUFFIX_PROVIDERS)) {
                    // new ARouter$$Providers$$app().loadInto();
                    //
                    // providers.put("com.alibaba.android.arouter.demo.testservice.HelloService", RouteMeta.build(RouteType.PROVIDER, HelloServiceImpl.class, "/service/hello", "service", null, -1, -2147483648));
                    // providers.put("com.alibaba.android.arouter.facade.service.SerializationService", RouteMeta.build(RouteType.PROVIDER, JsonServiceImpl.class, "/service/json", "service", null, -1, -2147483648));
                    // providers.put("com.alibaba.android.arouter.demo.testservice.SingleService", RouteMeta.build(RouteType.PROVIDER, SingleService.class, "/service/single", "service", null, -1, -2147483648));
                    ((IProviderGroup) (Class.forName(className).getConstructor().newInstance())).loadInto(Warehouse.providersIndex);
                }
            }

            logger.info(TAG, "Load root element finished, cost " + (System.currentTimeMillis() - startInit) + " ms.");

            if (Warehouse.groupsIndex.size() == 0) {
                logger.error(TAG, "No mapping files were found, check your configuration please!");
            }

            if (ARouter.debuggable()) {
                logger.debug(TAG, String.format(Locale.getDefault(), "LogisticsCenter has already been loaded, GroupIndex[%d], InterceptorIndex[%d], ProviderIndex[%d]", Warehouse.groupsIndex.size(), Warehouse.interceptorsIndex.size(), Warehouse.providersIndex.size()));
            }
        } catch (Exception e) {
            throw new HandlerException(TAG + "ARouter init logistics center exception! [" + e.getMessage() + "]");
        }
    }

    /**
     * Build postcard by serviceName
     * <p>
     * 生成一个Postcard对象
     *
     * @param serviceName serviceName 为 PathReplaceService 时，通过 Warehouse.providersIndex 找到 PathReplaceServiceImpl
     * @return postcard
     */
    public static Postcard buildProvider(String serviceName) {
        LogUtils.e("LogisticsCenter: ", "buildProvider");
        LogUtils.e("LogisticsCenter: ", "serviceName: " + serviceName);
        RouteMeta meta = Warehouse.providersIndex.get(serviceName);
        LogUtils.e("LogisticsCenter: ", "meta: " + meta);
        if (null == meta) {
            return null;
        } else {
            Postcard postcard = new Postcard(meta.getPath(), meta.getGroup());
            LogUtils.e("LogisticsCenter: ", "postcard: " + postcard);
            return postcard;
        }
    }

    /**
     * Completion the postcard by route metas
     * <p>
     * 加载用到的组内数据
     * <p>
     * 根据 /arouter/service/interceptor 可找到 com.alibaba.android.arouter.core.InterceptorServiceImpl
     *
     * @param postcard Incomplete postcard, should completion by this method.
     */
    public synchronized static void completion(Postcard postcard) {
        LogUtils.e("_ARouter", "completion");
        LogUtils.e("_ARouter", "completion postcard: " + postcard);
        if (null == postcard) {
            LogUtils.e("_ARouter", "NoRouteFoundException");
            throw new NoRouteFoundException(TAG + "No postcard!");
        }
        // 查找对应的打开文件
        RouteMeta routeMeta = Warehouse.routes.get(postcard.getPath());

        LogUtils.e("_ARouter", "completion routeMeta: " + routeMeta);

        if (null == routeMeta) {
            LogUtils.e("_ARouter", "null == routeMeta");
            /**
             * 加载对应的组内数据
             */
            Class<? extends IRouteGroup> groupMeta = Warehouse.groupsIndex.get(postcard.getGroup());  // Load route meta.
            if (null == groupMeta) {
                throw new NoRouteFoundException(TAG + "There is no route match the path [" + postcard.getPath() + "], in group [" + postcard.getGroup() + "]");
            } else {
                // Load route and cache it into memory, then delete from metas.
                try {
                    if (ARouter.debuggable()) {
                        logger.debug(TAG, String.format(Locale.getDefault(), "The group [%s] starts loading, trigger by [%s]", postcard.getGroup(), postcard.getPath()));
                    }
                    /**
                     * 加载组内的Activity
                     *
                     * atlas.put("/app/ImageActivity", RouteMeta.build(RouteType.ACTIVITY, ImageActivity.class, "/app/imageactivity", "app", null, -1, -2147483648));
                     * atlas.put("/app/MainActivity", RouteMeta.build(RouteType.ACTIVITY, MainActivity.class, "/app/mainactivity", "app", null, -1, -2147483648));
                     * atlas.put("/app/TextActivity", RouteMeta.build(RouteType.ACTIVITY, TextActivity.class, "/app/textactivity", "app", null, -1, -2147483648));
                     */
                    IRouteGroup iGroupInstance = groupMeta.getConstructor().newInstance();
                    // 将我们添加@Route注解的类映射到Warehouse.routes中；
                    iGroupInstance.loadInto(Warehouse.routes);

                    // 将已经加载过的组从Warehouse.groupsIndex中移除，避免重复添加进Warehouse.routes
                    Warehouse.groupsIndex.remove(postcard.getGroup());

                    if (ARouter.debuggable()) {
                        logger.debug(TAG, String.format(Locale.getDefault(), "The group [%s] has already been loaded, trigger by [%s]", postcard.getGroup(), postcard.getPath()));
                    }
                } catch (Exception e) {
                    throw new HandlerException(TAG + "Fatal exception when loading group meta. [" + e.getMessage() + "]");
                }
                // 这个时候Warehouse.routes已经有值了，所以重新调用本方法执行else代码块
                completion(postcard);   // Reload
            }
        } else {
            // 给postcard赋值
            postcard.setDestination(routeMeta.getDestination());
            postcard.setType(routeMeta.getType());
            postcard.setPriority(routeMeta.getPriority());
            postcard.setExtra(routeMeta.getExtra());

            LogUtils.e("_ARouter", "completion postcard: " + postcard);

            Uri rawUri = postcard.getUri();
            if (null != rawUri) {   // Try to set params into bundle.
                Map<String, String> resultMap = TextUtils.splitQueryParameters(rawUri);
                Map<String, Integer> paramsType = routeMeta.getParamsType();

                if (MapUtils.isNotEmpty(paramsType)) {
                    // Set value by its type, just for params which annotation by @Param
                    for (Map.Entry<String, Integer> params : paramsType.entrySet()) {
                        setValue(postcard,
                                params.getValue(),
                                params.getKey(),
                                resultMap.get(params.getKey()));
                    }

                    // Save params name which need auto inject.
                    postcard.getExtras().putStringArray(ARouter.AUTO_INJECT, paramsType.keySet().toArray(new String[]{}));
                }

                // Save raw uri
                postcard.withString(ARouter.RAW_URI, rawUri.toString());
            }

            switch (routeMeta.getType()) {
                case PROVIDER:  // if the route is provider, should find its instance
                    // Its provider, so it must be implememt IProvider
                    Class<? extends IProvider> providerMeta = (Class<? extends IProvider>) routeMeta.getDestination();
                    IProvider instance = Warehouse.providers.get(providerMeta);
                    if (null == instance) { // There's no instance of this provider
                        IProvider provider;
                        try {
                            provider = providerMeta.getConstructor().newInstance();
                            provider.init(mContext);
                            Warehouse.providers.put(providerMeta, provider);
                            instance = provider;
                        } catch (Exception e) {
                            throw new HandlerException("Init provider failed! " + e.getMessage());
                        }
                    }
                    // 将IProvider的实现类保存在postcard中，因此可以从postcard获取IProvider的实例对象；
                    postcard.setProvider(instance);
                    // greenChannel()会忽略拦截器
                    postcard.greenChannel();    // Provider should skip all of interceptors
                    break;
                case FRAGMENT:
                    // greenChannel()会忽略拦截器
                    postcard.greenChannel();    // Fragment needn't interceptors
                default:
                    break;
            }
        }
    }

    /**
     * Set value by known type
     *
     * @param postcard postcard
     * @param typeDef  type
     * @param key      key
     * @param value    value
     */
    private static void setValue(Postcard postcard, Integer typeDef, String key, String value) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
            return;
        }

        try {
            if (null != typeDef) {
                if (typeDef == TypeKind.BOOLEAN.ordinal()) {
                    postcard.withBoolean(key, Boolean.parseBoolean(value));
                } else if (typeDef == TypeKind.BYTE.ordinal()) {
                    postcard.withByte(key, Byte.valueOf(value));
                } else if (typeDef == TypeKind.SHORT.ordinal()) {
                    postcard.withShort(key, Short.valueOf(value));
                } else if (typeDef == TypeKind.INT.ordinal()) {
                    postcard.withInt(key, Integer.valueOf(value));
                } else if (typeDef == TypeKind.LONG.ordinal()) {
                    postcard.withLong(key, Long.valueOf(value));
                } else if (typeDef == TypeKind.FLOAT.ordinal()) {
                    postcard.withFloat(key, Float.valueOf(value));
                } else if (typeDef == TypeKind.DOUBLE.ordinal()) {
                    postcard.withDouble(key, Double.valueOf(value));
                } else if (typeDef == TypeKind.STRING.ordinal()) {
                    postcard.withString(key, value);
                } else if (typeDef == TypeKind.PARCELABLE.ordinal()) {
                    // TODO : How to description parcelable value with string?
                } else if (typeDef == TypeKind.OBJECT.ordinal()) {
                    postcard.withString(key, value);
                } else {    // Compatible compiler sdk 1.0.3, in that version, the string type = 18
                    postcard.withString(key, value);
                }
            } else {
                postcard.withString(key, value);
            }
        } catch (Throwable ex) {
            logger.warning(Consts.TAG, "LogisticsCenter setValue failed! " + ex.getMessage());
        }
    }

    /**
     * Suspend bussiness, clear cache.
     */
    public static void suspend() {
        Warehouse.clear();
    }
}