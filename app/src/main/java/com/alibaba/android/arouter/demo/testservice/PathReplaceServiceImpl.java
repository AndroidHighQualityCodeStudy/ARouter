package com.alibaba.android.arouter.demo.testservice;

import android.content.Context;
import android.net.Uri;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.facade.service.PathReplaceService;

/**
 * Created by xiaxveliang on 2018/3/20.
 */
// 实现PathReplaceService接口，并加上一个Path内容任意的注解即可
@Route(path = "/app/pathreplace") // 必须标明注解
public class PathReplaceServiceImpl implements PathReplaceService {
    /**
     * For normal path.
     *
     * @param path raw path
     */
    @Override
    public String forString(String path) {
        return path;    // 按照一定的规则处理之后返回处理后的结果
    }

    /**
     * For uri type.
     *
     * @param uri raw uri
     */
    @Override
    public Uri forUri(Uri uri) {
        return uri;    // 按照一定的规则处理之后返回处理后的结果
    }

    @Override
    public void init(Context context) {

    }
}
