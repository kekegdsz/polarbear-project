package com.undersky.androidim.feature.home

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.undersky.androidim.bootstrap.BootstrapApplication
import com.undersky.androidim.bootstrap.ImHostActivity
import com.undersky.business.user.AuthTokenHolder
import com.undersky.business.user.DirectoryUserDto
import com.undersky.business.user.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 从缓存拉目录；若为空则请求接口，供消息页发起群聊使用。 */
suspend fun loadDirectoryUsersForPicker(app: BootstrapApplication, session: UserSession): List<DirectoryUserDto> =
    withContext(Dispatchers.IO) {
        AuthTokenHolder.set(session.token)
        val s = app.services
        var raw = s.userDirectoryCacheStore.read(session.userId)
        if (raw.isEmpty()) {
            val result = s.userDirectoryRepository.listAll()
            result.onSuccess { list -> s.userDirectoryCacheStore.save(session.userId, list) }
            raw = result.getOrElse { emptyList() }
        }
        sortedDirectoryUsersForGroup(raw, session.userId)
    }

/**
 * 底部弹层勾选联系人（微信式），完成后发起建群。
 */
fun Fragment.showCreateGroupMemberPicker(session: UserSession, users: List<DirectoryUserDto>) {
    if (users.isEmpty()) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage("没有可邀请的用户，请先到通讯录加载列表或稍后重试")
            .setPositiveButton("确定", null)
            .show()
        return
    }
    parentFragmentManager.setFragmentResultListener(
        CreateGroupPickBottomSheet.REQUEST_KEY,
        viewLifecycleOwner
    ) { _, bundle ->
        val ids = bundle.getLongArray(CreateGroupPickBottomSheet.RESULT_MEMBER_IDS)?.toList().orEmpty()
        if (ids.isEmpty()) return@setFragmentResultListener
        launchCreateGroupWithMemberIds(session, ids)
    }
    CreateGroupPickBottomSheet.show(parentFragmentManager, users)
}

private fun Fragment.launchCreateGroupWithMemberIds(session: UserSession, memberIds: List<Long>) {
    val app = requireActivity().application as BootstrapApplication
    lifecycleScope.launch {
        try {
            val ev = app.services.imClient.createGroupAndAwait(name = null, memberUserIds = memberIds)
            app.services.imClient.requestConversations()
            (requireActivity() as ImHostActivity).navigateMainToChat(-1L, ev.groupId, ev.name)
        } catch (_: TimeoutCancellationException) {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage("建群超时，请检查网络后重试")
                .setPositiveButton("确定", null)
                .show()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(e.message ?: "建群失败，请检查网络后重试")
                .setPositiveButton("确定", null)
                .show()
        }
    }
}

private fun DirectoryUserDto.sortKeyForGroup(): String =
    nickname?.takeIf { it.isNotBlank() } ?: username?.takeIf { it.isNotBlank() } ?: ""

private fun sortedDirectoryUsersForGroup(raw: List<DirectoryUserDto>, selfId: Long): List<DirectoryUserDto> =
    raw
        .filter { it.id != selfId }
        .sortedWith(
            compareBy<DirectoryUserDto> { it.sortKeyForGroup().isBlank() }
                .thenBy { it.sortKeyForGroup().lowercase() }
                .thenBy { it.id }
        )
