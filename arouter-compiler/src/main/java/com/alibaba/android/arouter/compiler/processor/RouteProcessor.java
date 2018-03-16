package com.alibaba.android.arouter.compiler.processor;

import com.alibaba.android.arouter.compiler.utils.Consts;
import com.alibaba.android.arouter.compiler.utils.Logger;
import com.alibaba.android.arouter.compiler.utils.TypeUtils;
import com.alibaba.android.arouter.facade.annotation.Autowired;
import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.enums.RouteType;
import com.alibaba.android.arouter.facade.model.RouteMeta;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import static com.alibaba.android.arouter.compiler.utils.Consts.ACTIVITY;
import static com.alibaba.android.arouter.compiler.utils.Consts.ANNOTATION_TYPE_AUTOWIRED;
import static com.alibaba.android.arouter.compiler.utils.Consts.ANNOTATION_TYPE_ROUTE;
import static com.alibaba.android.arouter.compiler.utils.Consts.FRAGMENT;
import static com.alibaba.android.arouter.compiler.utils.Consts.IPROVIDER_GROUP;
import static com.alibaba.android.arouter.compiler.utils.Consts.IROUTE_GROUP;
import static com.alibaba.android.arouter.compiler.utils.Consts.ITROUTE_ROOT;
import static com.alibaba.android.arouter.compiler.utils.Consts.KEY_MODULE_NAME;
import static com.alibaba.android.arouter.compiler.utils.Consts.METHOD_LOAD_INTO;
import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_GROUP;
import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_PROVIDER;
import static com.alibaba.android.arouter.compiler.utils.Consts.NAME_OF_ROOT;
import static com.alibaba.android.arouter.compiler.utils.Consts.PACKAGE_OF_GENERATE_FILE;
import static com.alibaba.android.arouter.compiler.utils.Consts.SEPARATOR;
import static com.alibaba.android.arouter.compiler.utils.Consts.SERVICE;
import static com.alibaba.android.arouter.compiler.utils.Consts.WARNING_TIPS;
import static javax.lang.model.element.Modifier.PUBLIC;

/**
 * A processor used for find route.
 *
 * @author Alex <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 16/8/15 下午10:08
 */
// 主要的作用是注解 processor 类，并对其生成 META-INF 的配置信息。
@AutoService(Processor.class)
// ??????????????
// 这个注解用来注册可能通过命令行传递给处理器的操作选项
@SupportedOptions(KEY_MODULE_NAME)
// 标识该处理器支持的源码版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)

// 这个 Processor 要处理的 Annotation 名字
// com.alibaba.android.arouter.facade.annotation.Route
// com.alibaba.android.arouter.facade.annotation.Autowired
@SupportedAnnotationTypes({ANNOTATION_TYPE_ROUTE, ANNOTATION_TYPE_AUTOWIRED})
public class RouteProcessor extends AbstractProcessor {

    // Filter用来创建新的源文件，class文件以及辅助文件
    private Filer mFiler;       // File util, write class file into disk.

    /**
     * 参考：
     * http://blog.csdn.net/dd864140130/article/details/53875814
     * <p>
     * Elements 中包含用于操作Element的工具方法
     * element表示一个静态的，语言级别的构件。
     * 而任何一个结构化文档都可以看作是由不同的element组成的结构体，比如XML，JSON等。
     * 对于java源文件来说，他同样是一种结构化文档：
     * package com.closedevice;             //PackageElement
     * public class Main{                   //TypeElement
     * private int x;                       //VariableElement
     * private Main(){                      //ExecuteableElement
     * }
     * private void print(String msg){      //其中的参数部分String msg为TypeElement
     * }
     * }
     */
    private Elements elements;
    // Types中包含用于操作TypeMirror的工具方法
    private Types types;
    // TypeMirror代表java语言中的类型.
    // Types包括基本类型，声明类型（类类型和接口类型），数组，类型变量和空类型。也代表通配类型参数，可执行文件的签名和返回类型等。
    // TypeMirror类中最重要的是getKind()方法，该方法返回TypeKind类型，为了方便大家理解
    // Element代表源代码，TypeElement代表的是源码中的类型元素，比如类。
    // 虽然我们可以从TypeElement中获取类名，TypeElement中不包含类本身的信息，
    // 比如它的父类，要想获取这信息需要借助TypeMirror，可以通过Element中的asType()获取元素对应的TypeMirror
    private TypeMirror iProvider = null;

    // 判断Element类型
    private TypeUtils typeUtils;


    //
    // ModuleName and routeMeta.
    // 例：
    // group='test'
    // {type=FRAGMENT,
    // rawType=com.alibaba.android.arouter.demo.BlankFragment,
    // destination=null,
    // path='/test/fragment',
    // group='test',
    // priority=-1,
    // extra=-2147483648}
    private Map<String, Set<RouteMeta>> groupMap = new HashMap<>();
    // 例：
    // group: test
    // group: ARouter$$Group$$test
    private Map<String, String> rootMap = new TreeMap<>();  // Map of root metas, used for generate class file in order.

    private String moduleName = null;   // Module name, maybe its 'app' or others


    // 日志封装
    // 通过Messager来报告错误，警告和其他提示信息
    private Logger logger;

    /**
     * Initializes the processor with the processing environment by
     * setting the {@code processingEnv} field to the value of the
     * {@code processingEnv} argument.  An {@code
     * IllegalStateException} will be thrown if this method is called
     * more than once on the same object.
     *
     * @param processingEnv environment to access facilities the tool framework
     *                      provides to the processor
     * @throws IllegalStateException if this method is called more than once.
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        // Generate class.
        // Filter用来创建新的源文件，class文件以及辅助文件
        mFiler = processingEnv.getFiler();
        // Get type utils.
        types = processingEnv.getTypeUtils();

        // Get class meta.
        // Elements 中包含用于操作Element的工具方法
        elements = processingEnv.getElementUtils();

        typeUtils = new TypeUtils(types, elements);
        // Messager用来报告错误，警告和其他提示信息
        logger = new Logger(processingEnv.getMessager());   // Package the log utils.

        // Attempt to get user configuration [moduleName]
        Map<String, String> options = processingEnv.getOptions();
        if (MapUtils.isNotEmpty(options)) {
            moduleName = options.get(KEY_MODULE_NAME);
        }

        if (StringUtils.isNotEmpty(moduleName)) {
            moduleName = moduleName.replaceAll("[^0-9a-zA-Z_]+", "");

            logger.info("The user has configuration the module name, it was [" + moduleName + "]");
        } else {
            logger.error("These no module name, at 'build.gradle', like :\n" +
                    "apt {\n" +
                    "    arguments {\n" +
                    "        moduleName project.getName();\n" +
                    "    }\n" +
                    "}\n");
            throw new RuntimeException("ARouter::Compiler >>> No module name, for more information, look at gradle log.");
        }

        iProvider = elements.getTypeElement(Consts.IPROVIDER).asType();

        logger.info(">>> RouteProcessor init. <<<");
    }

    /**
     * {@inheritDoc}
     *
     * @param annotations
     * @param roundEnv
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (CollectionUtils.isNotEmpty(annotations)) {
            Set<? extends Element> routeElements = roundEnv.getElementsAnnotatedWith(Route.class);
            try {
                logger.info(">>> Found routes, start... <<<");
                this.parseRoutes(routeElements);

            } catch (Exception e) {
                logger.error(e);
            }
            return true;
        }

        return false;
    }

    private void parseRoutes(Set<? extends Element> routeElements) throws IOException {
        if (CollectionUtils.isNotEmpty(routeElements)) {
            // Perpare the type an so on.

            logger.info(">>> Found routes, size is " + routeElements.size() + " <<<");

            rootMap.clear();

            TypeMirror type_Activity = elements.getTypeElement(ACTIVITY).asType();
            TypeMirror type_Service = elements.getTypeElement(SERVICE).asType();
            TypeMirror fragmentTm = elements.getTypeElement(FRAGMENT).asType();
            TypeMirror fragmentTmV4 = elements.getTypeElement(Consts.FRAGMENT_V4).asType();

            // Interface of ARouter
            TypeElement type_IRouteGroup = elements.getTypeElement(IROUTE_GROUP);
            TypeElement type_IProviderGroup = elements.getTypeElement(IPROVIDER_GROUP);
            ClassName routeMetaCn = ClassName.get(RouteMeta.class);
            ClassName routeTypeCn = ClassName.get(RouteType.class);

            /*
               Build input type, format as :

               ```Map<String, Class<? extends IRouteGroup>>```
             */
            ParameterizedTypeName inputMapTypeOfRoot = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ParameterizedTypeName.get(
                            ClassName.get(Class.class),
                            WildcardTypeName.subtypeOf(ClassName.get(type_IRouteGroup))
                    )
            );

            /*

              ```Map<String, RouteMeta>```
             */
            ParameterizedTypeName inputMapTypeOfGroup = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ClassName.get(RouteMeta.class)
            );

            /*
              Build input param name.
             */
            ParameterSpec rootParamSpec = ParameterSpec.builder(inputMapTypeOfRoot, "routes").build();
            ParameterSpec groupParamSpec = ParameterSpec.builder(inputMapTypeOfGroup, "atlas").build();
            ParameterSpec providerParamSpec = ParameterSpec.builder(inputMapTypeOfGroup, "providers").build();  // Ps. its param type same as groupParamSpec!

            /*
              Build method : 'loadInto'
             */
            MethodSpec.Builder loadIntoMethodOfRootBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(rootParamSpec);

            //  Follow a sequence, find out metas of group first, generate java file, then statistics them as root.
            for (Element element : routeElements) {
                TypeMirror tm = element.asType();
                Route route = element.getAnnotation(Route.class);
                RouteMeta routeMete = null;

                if (types.isSubtype(tm, type_Activity)) {                 // Activity
                    logger.info(">>> Found activity route: " + tm.toString() + " <<<");

                    // Get all fields annotation by @Autowired
                    Map<String, Integer> paramsType = new HashMap<>();
                    for (Element field : element.getEnclosedElements()) {
                        if (field.getKind().isField() && field.getAnnotation(Autowired.class) != null && !types.isSubtype(field.asType(), iProvider)) {
                            // It must be field, then it has annotation, but it not be provider.
                            Autowired paramConfig = field.getAnnotation(Autowired.class);
                            paramsType.put(StringUtils.isEmpty(paramConfig.name()) ? field.getSimpleName().toString() : paramConfig.name(), typeUtils.typeExchange(field));
                        }
                    }
                    routeMete = new RouteMeta(route, element, RouteType.ACTIVITY, paramsType);
                } else if (types.isSubtype(tm, iProvider)) {         // IProvider
                    logger.info(">>> Found provider route: " + tm.toString() + " <<<");
                    routeMete = new RouteMeta(route, element, RouteType.PROVIDER, null);
                } else if (types.isSubtype(tm, type_Service)) {           // Service
                    logger.info(">>> Found service route: " + tm.toString() + " <<<");
                    routeMete = new RouteMeta(route, element, RouteType.parse(SERVICE), null);
                } else if (types.isSubtype(tm, fragmentTm) || types.isSubtype(tm, fragmentTmV4)) {
                    logger.info(">>> Found fragment route: " + tm.toString() + " <<<");
                    routeMete = new RouteMeta(route, element, RouteType.parse(FRAGMENT), null);
                } else {
                    throw new RuntimeException("ARouter::Compiler >>> Found unsupported class type, type = [" + types.toString() + "].");
                }

                categories(routeMete);
                // if (StringUtils.isEmpty(moduleName)) {   // Hasn't generate the module name.
                //     moduleName = ModuleUtils.generateModuleName(element, logger);
                // }
            }

            MethodSpec.Builder loadIntoMethodOfProviderBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                    .addAnnotation(Override.class)
                    .addModifiers(PUBLIC)
                    .addParameter(providerParamSpec);

            // Start generate java source, structure is divided into upper and lower levels, used for demand initialization.
            for (Map.Entry<String, Set<RouteMeta>> entry : groupMap.entrySet()) {
                String groupName = entry.getKey();

                MethodSpec.Builder loadIntoMethodOfGroupBuilder = MethodSpec.methodBuilder(METHOD_LOAD_INTO)
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(groupParamSpec);

                // Build group method body
                Set<RouteMeta> groupData = entry.getValue();
                for (RouteMeta routeMeta : groupData) {
                    switch (routeMeta.getType()) {
                        case PROVIDER:  // Need cache provider's super class
                            List<? extends TypeMirror> interfaces = ((TypeElement) routeMeta.getRawType()).getInterfaces();
                            for (TypeMirror tm : interfaces) {
                                if (types.isSameType(tm, iProvider)) {   // Its implements iProvider interface himself.
                                    // This interface extend the IProvider, so it can be used for mark provider
                                    loadIntoMethodOfProviderBuilder.addStatement(
                                            "providers.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, null, " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + "))",
                                            (routeMeta.getRawType()).toString(),
                                            routeMetaCn,
                                            routeTypeCn,
                                            ClassName.get((TypeElement) routeMeta.getRawType()),
                                            routeMeta.getPath(),
                                            routeMeta.getGroup());
                                } else if (types.isSubtype(tm, iProvider)) {
                                    // This interface extend the IProvider, so it can be used for mark provider
                                    loadIntoMethodOfProviderBuilder.addStatement(
                                            "providers.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, null, " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + "))",
                                            tm.toString(),    // So stupid, will duplicate only save class name.
                                            routeMetaCn,
                                            routeTypeCn,
                                            ClassName.get((TypeElement) routeMeta.getRawType()),
                                            routeMeta.getPath(),
                                            routeMeta.getGroup());
                                }
                            }
                            break;
                        default:
                            break;
                    }

                    // Make map body for paramsType
                    StringBuilder mapBodyBuilder = new StringBuilder();
                    Map<String, Integer> paramsType = routeMeta.getParamsType();
                    if (MapUtils.isNotEmpty(paramsType)) {
                        for (Map.Entry<String, Integer> types : paramsType.entrySet()) {
                            mapBodyBuilder.append("put(\"").append(types.getKey()).append("\", ").append(types.getValue()).append("); ");
                        }
                    }
                    String mapBody = mapBodyBuilder.toString();

                    loadIntoMethodOfGroupBuilder.addStatement(
                            "atlas.put($S, $T.build($T." + routeMeta.getType() + ", $T.class, $S, $S, " + (StringUtils.isEmpty(mapBody) ? null : ("new java.util.HashMap<String, Integer>(){{" + mapBodyBuilder.toString() + "}}")) + ", " + routeMeta.getPriority() + ", " + routeMeta.getExtra() + "))",
                            routeMeta.getPath(),
                            routeMetaCn,
                            routeTypeCn,
                            ClassName.get((TypeElement) routeMeta.getRawType()),
                            routeMeta.getPath().toLowerCase(),
                            routeMeta.getGroup().toLowerCase());
                }

                // Generate groups
                String groupFileName = NAME_OF_GROUP + groupName;
                JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                        TypeSpec.classBuilder(groupFileName)
                                .addJavadoc(WARNING_TIPS)
                                .addSuperinterface(ClassName.get(type_IRouteGroup))
                                .addModifiers(PUBLIC)
                                .addMethod(loadIntoMethodOfGroupBuilder.build())
                                .build()
                ).build().writeTo(mFiler);

                logger.info("---rootMap---");
                logger.info(">>> Generated group: " + groupName + "<<<");
                logger.info(">>> Generated group: " + groupFileName + "<<<");
                rootMap.put(groupName, groupFileName);
            }

            if (MapUtils.isNotEmpty(rootMap)) {
                // Generate root meta by group name, it must be generated before root, then I can find out the class of group.
                for (Map.Entry<String, String> entry : rootMap.entrySet()) {
                    loadIntoMethodOfRootBuilder.addStatement("routes.put($S, $T.class)", entry.getKey(), ClassName.get(PACKAGE_OF_GENERATE_FILE, entry.getValue()));
                }
            }

            // Wirte provider into disk
            String providerMapFileName = NAME_OF_PROVIDER + SEPARATOR + moduleName;
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(providerMapFileName)
                            .addJavadoc(WARNING_TIPS)
                            .addSuperinterface(ClassName.get(type_IProviderGroup))
                            .addModifiers(PUBLIC)
                            .addMethod(loadIntoMethodOfProviderBuilder.build())
                            .build()
            ).build().writeTo(mFiler);

            logger.info(">>> Generated provider map, name is " + providerMapFileName + " <<<");

            // Write root meta into disk.
            String rootFileName = NAME_OF_ROOT + SEPARATOR + moduleName;
            JavaFile.builder(PACKAGE_OF_GENERATE_FILE,
                    TypeSpec.classBuilder(rootFileName)
                            .addJavadoc(WARNING_TIPS)
                            .addSuperinterface(ClassName.get(elements.getTypeElement(ITROUTE_ROOT)))
                            .addModifiers(PUBLIC)
                            .addMethod(loadIntoMethodOfRootBuilder.build())
                            .build()
            ).build().writeTo(mFiler);

            logger.info(">>> Generated root, name is " + rootFileName + " <<<");
        }
    }

    /**
     * Sort metas in group.
     *
     * @param routeMete metas.
     */
    private void categories(RouteMeta routeMete) {
        logger.info("---categories---");
        if (routeVerify(routeMete)) {
            logger.info(">>> Start categories, group = " + routeMete.getGroup() + ", path = " + routeMete.getPath() + " <<<");
            Set<RouteMeta> routeMetas = groupMap.get(routeMete.getGroup());
            if (CollectionUtils.isEmpty(routeMetas)) {
                Set<RouteMeta> routeMetaSet = new TreeSet<>(new Comparator<RouteMeta>() {
                    @Override
                    public int compare(RouteMeta r1, RouteMeta r2) {
                        try {
                            return r1.getPath().compareTo(r2.getPath());
                        } catch (NullPointerException npe) {
                            logger.error(npe.getMessage());
                            return 0;
                        }
                    }
                });
                routeMetaSet.add(routeMete);

                logger.info("routeMete: " + routeMete);
                logger.info("routeMete.getGroup(): " + routeMete.getGroup());

                groupMap.put(routeMete.getGroup(), routeMetaSet);
            } else {
                routeMetas.add(routeMete);
            }
        } else {
            logger.warning(">>> Route meta verify error, group is " + routeMete.getGroup() + " <<<");
        }
    }

    /**
     * Verify the route meta
     *
     * @param meta raw meta
     */
    private boolean routeVerify(RouteMeta meta) {
        String path = meta.getPath();

        if (StringUtils.isEmpty(path) || !path.startsWith("/")) {   // The path must be start with '/' and not empty!
            return false;
        }

        if (StringUtils.isEmpty(meta.getGroup())) { // Use default group(the first word in path)
            try {
                String defaultGroup = path.substring(1, path.indexOf("/", 1));
                if (StringUtils.isEmpty(defaultGroup)) {
                    return false;
                }

                meta.setGroup(defaultGroup);
                return true;
            } catch (Exception e) {
                logger.error("Failed to extract default group! " + e.getMessage());
                return false;
            }
        }

        return true;
    }
}
