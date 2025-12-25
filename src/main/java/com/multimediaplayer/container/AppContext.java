package com.multimediaplayer.container;

import org.slf4j.Logger;

/**
 * 全局应用上下文接口：统一提供模块获取、全局资源访问能力
 */
public interface AppContext {
    // 根据接口类型获取其他模块实例
    <T> T getModule(Class<T> moduleInterface);
    // 获取全局日志对象
    Logger getGlobalLogger();
}
