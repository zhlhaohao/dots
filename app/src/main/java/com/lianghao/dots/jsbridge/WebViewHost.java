package com.lianghao.myapp.jsbridge;

/**
 * 宿主接口：插件通过它访问承载 WebView 的 Activity 能力。
 * 自 ../news 的 BaseWebViewActivity 依赖抽象而来（原框架直接耦合 Activity 类）。
 */
public interface WebViewHost {

    /** 宿主 Activity，仅保证非空时可用；插件不应假设其具体类型 */
    android.app.Activity getHostActivity();

    /** 注册 Activity 结果回调（扫码/拍照等拉起其他页面的插件用） */
    void registerResultCallback(int requestCode, BaseJSPlugin jsPlugin);

    /** 注册权限回调（定位/相机等运行时权限插件用） */
    void registerPermissionCallback(int requestCode, BaseJSPlugin jsPlugin);

    /**
     * 拉起外部 Activity 并把结果转发给已 registerResultCallback 的插件。
     * 宿主经 ActivityResult API 实现（news 侧为 activity.startActivityForResult）。
     *
     * @param requestCode 插件占用的结果码（转发时不再区分，单挂起请求场景）
     * @param intent      目标 Intent（如 ACTION_IMAGE_CAPTURE）
     */
    void startPluginActivityForResult(android.content.Intent intent, int requestCode);

    /**
     * 申请运行时权限并把结果转发给已 registerPermissionCallback 的插件。
     * 宿主经 ActivityResult API 实现（news 侧为 EasyPermissions.requestPermissions）。
     *
     * @param requestCode 插件占用的权限码
     * @param rationale   权限用途说明文案（宿主可忽略，直接弹系统权限框）
     * @param permissions 权限列表（如 CAMERA）
     */
    void requestRuntimePermissions(int requestCode, String rationale, String... permissions);
}
