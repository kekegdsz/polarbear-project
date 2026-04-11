/**
 * App 分发配置 - 按需修改下载链接与联系方式
 */
export const appConfig = {
  name: '苍穹之下',
  shortName: '苍穹之下',
  description: '官方 App 下载中心，支持 iOS 与 Android 双平台。',
  version: '1.0.0',
  build: '100',

  download: {
    ios: {
      label: 'iOS 下载',
      url: 'itms-services://?action=download-manifest&url=https://example.com/your.plist',
      direct: 'https://example.com/your.ipa',
      minVersion: 'iOS 12.0 及以上'
    },
    android: {
      label: 'Android 下载',
      url: 'https://example.com/your.apk',
      direct: 'https://example.com/your.apk',
      minVersion: 'Android 6.0 及以上'
    }
  },

  store: {
    ios: null,
    android: null
  },

  contact: {
    email: 'support@example.com',
    website: 'https://example.com'
  }
}
