/**
 * App 分发配置 - 按需修改
 */
export const appConfig = {
  name: '我的应用',
  shortName: 'MyApp',
  description: '一款出色的移动应用，支持 iOS 与 Android 双平台。',
  version: '1.0.0',
  build: '100',

  // 下载链接 - 替换为你的实际链接
  download: {
    ios: {
      label: 'iOS 下载',
      url: 'itms-services://?action=download-manifest&url=https://example.com/your.plist',
      direct: 'https://example.com/your.ipa', // 可选：直链
      minVersion: '12.0'
    },
    android: {
      label: 'Android 下载',
      url: 'https://example.com/your.apk',
      direct: 'https://example.com/your.apk',
      minVersion: 'Android 6.0'
    }
  },

  // 应用商店链接（可选）
  store: {
    ios: null, // 'https://apps.apple.com/app/xxx'
    android: null // 'https://play.google.com/store/apps/details?id=xxx'
  },

  // 联系方式
  contact: {
    email: 'support@example.com',
    website: 'https://example.com'
  }
}
