package com.undersky.im.core

/** 聊天列表分页：每页条数（与 UI、HISTORY 请求一致） */
const val CHAT_PAGE_SIZE: Int = 20

/** 进会话时增量同步「比本地更新」的消息条数上限（服务端 cap 200） */
const val CHAT_SYNC_NEWER_LIMIT: Int = 100
