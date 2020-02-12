package com.example.studypermission.aspect;

// TODO 专门处理权限的 Aspect

import android.content.Context;

import androidx.fragment.app.Fragment;

import com.example.studypermission.MyPermissionActivity;
import com.example.studypermission.annotation.Permission;
import com.example.studypermission.annotation.PermissionCancel;
import com.example.studypermission.annotation.PermissionDenied;
import com.example.studypermission.core.IPermission;
import com.example.studypermission.util.PermissionUtils;


import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class PermissionAspect {

    @Pointcut
    ("execution(@com.example.studypermission.annotation.Permission * *(..)) && @annotation(permission)")
    public void pointActionMethod(Permission permission) {/* 法内部不做任何事情，只为了@Pointcut服务*/}

    // 对方法环绕监听
    @Around("pointActionMethod(permission)")
    public void aProceedingJoinPoint(final ProceedingJoinPoint point, Permission permission) throws Throwable {
        Context context = null;
        final Object thisObject = point.getThis();
        if (thisObject instanceof Context) {
            context = (Context) thisObject;
        } else if (thisObject instanceof Fragment) {
            context = ((Fragment) thisObject).getActivity();
        }
        if (null == context || permission == null) {
            throw new IllegalAccessException("null == context || permission == null is null");
        }
        final Context finalContext = context;
        MyPermissionActivity.requestPermissionAction
                (context, permission.value(), permission.requestCode(), new IPermission() {
                    @Override
                    public void ganted() { // 申请成功 授权成功
                        // 让被 @Permission 的方法 正常的执行下去
                        try {
                            point.proceed();
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }

                    @Override
                    public void cancel() { // 被拒绝
                        // 调用到 被 @PermissionCancel 的方法
                        PermissionUtils.invokeAnnotation(thisObject, PermissionCancel.class);
                    }

                    @Override
                    public void denied() { // 严重拒绝 勾选了 不再提醒
                        // 调用到 被 @PermissionDenied 的方法
                        PermissionUtils.invokeAnnotation(thisObject, PermissionDenied.class);

                        // 不仅仅要提醒用户，还需要 自动跳转到 手机设置界面
                        PermissionUtils.startAndroidSettings(finalContext);
                    }
                });
    }

}
